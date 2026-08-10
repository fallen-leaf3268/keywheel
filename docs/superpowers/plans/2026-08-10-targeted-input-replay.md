# Targeted Input Replay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让轮盘选中的正常 `KeyMapping` 通过原版键盘或鼠标处理器接收目标隔离的合成输入，从而兼容 Forge 原始输入事件监听器且不误触发同键功能。

**Architecture:** 使用客户端线程内的短生命周期 `SyntheticInputContext` 标记唯一目标；轮盘动作通过 Minecraft 任务队列延迟到界面关闭后，再由 `SyntheticInputReplayer` 调用原版输入入口。现有映射 Mixin 在事务期间屏蔽非目标，同步事件结束后使用 `finally` 恢复状态。

**Tech Stack:** Java 17、Minecraft 1.20.1、Forge 47.4.18、Mixin 0.8.5、现有 `keyWheelRegressionTest`。

## Global Constraints

- 玩家侧短按、长按轮盘、交换模式、功能锁定和保持长按操作方式严格不变。
- 一次轮盘选择只能触发一个目标 `KeyMapping`。
- 同时支持 `InputConstants.Type.KEYSYM` 与 `InputConstants.Type.MOUSE`。
- 不加入任何第三方模组 ID、按键 ID 或专用兼容类。
- 不新增网络协议，继续保持 `clientSideOnly=true`。
- 不支持硬编码键码、绕过 `KeyMapping` 或直接轮询 GLFW 的第三方实现。
- JAR 仅输出到 `C:\Users\Lenovo\Desktop\Ai_Run\output`，不部署到 `mods`。

---

### Task 1: 合成输入上下文与隔离策略

**Files:**
- Create: `src/main/java/com/example/keywheel/input/SyntheticInputContext.java`
- Modify: `src/main/java/com/example/keywheel/mixin/KeyMappingClickMixin.java`
- Modify: `src/main/java/com/example/keywheel/mixin/KeyMappingLookupMixin.java`
- Modify: `src/main/java/com/example/keywheel/mixin/ForgeKeyMappingMixin.java`
- Test: `src/test/java/com/example/keywheel/input/KeyWheelRegressionTest.java`

**Interfaces:**
- Produces: `SyntheticInputContext.begin(KeyMapping, InputConstants.Key)`, `isActive()`, `allows(KeyMapping)`, `shouldMask(KeyMapping, InputConstants.Key)`。
- Consumes: 现有 `WheelActionBridge.isForceAllowed(KeyMapping)` 与锁定配置。

- [ ] **Step 1: Write the failing context-isolation test**

在回归测试入口调用 `requireSyntheticInputIsolation()`，创建两个同键映射并验证：事务外全部允许，事务内只允许目标、只屏蔽同键非目标，关闭作用域后状态清空。测试还要反射确认 `KeyMappingClickMixin` 存在 `keywheel$maskSyntheticKey` 注入。

```java
private static void requireSyntheticInputIsolation() throws Exception {
    KeyMapping target = new KeyMapping("key.keywheel.replay.target", 71, "key.categories.misc");
    KeyMapping sameKey = new KeyMapping("key.keywheel.replay.other", 71, "key.categories.misc");
    KeyMapping otherKey = new KeyMapping("key.keywheel.replay.unrelated", 72, "key.categories.misc");
    Class<?> context = Class.forName("com.example.keywheel.input.SyntheticInputContext");
    var begin = context.getDeclaredMethod("begin", KeyMapping.class, InputConstants.Key.class);
    var active = context.getDeclaredMethod("isActive");
    var allows = context.getDeclaredMethod("allows", KeyMapping.class);
    var shouldMask = context.getDeclaredMethod("shouldMask", KeyMapping.class, InputConstants.Key.class);
    AutoCloseable scope = (AutoCloseable) begin.invoke(null, target, target.getKey());
    try {
        require((boolean) active.invoke(null), "synthetic replay context must be active inside its scope");
        require((boolean) allows.invoke(null, target), "synthetic replay must allow its target");
        require(!(boolean) allows.invoke(null, sameKey), "synthetic replay must isolate a same-key non-target");
        require((boolean) shouldMask.invoke(null, sameKey, sameKey.getKey()), "same-key non-target key must be masked");
        require(!(boolean) shouldMask.invoke(null, otherKey, otherKey.getKey()), "unrelated keys must not be masked");
    } finally {
        scope.close();
    }
    require(!(boolean) active.invoke(null), "synthetic replay context must always clear");
    requireDeclaredMethod("com.example.keywheel.mixin.KeyMappingClickMixin", "keywheel$maskSyntheticKey");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat keyWheelRegressionTest --no-daemon --offline`

Expected: FAIL，原因是 `SyntheticInputContext` 或 `keywheel$maskSyntheticKey` 尚不存在。

- [ ] **Step 3: Implement the scoped context**

实现一个只保存当前目标和原始键的 `ThreadLocal<State>`。`begin` 拒绝嵌套事务，返回幂等 `AutoCloseable`；`allows` 在无事务时返回 `true`，有事务时只允许目标身份；`shouldMask` 只屏蔽绑定到事务物理键的非目标。

```java
public final class SyntheticInputContext {
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    public static AutoCloseable begin(KeyMapping target, InputConstants.Key key) {
        if (target == null || key == null || CURRENT.get() != null) throw new IllegalStateException();
        State state = new State(target, key);
        CURRENT.set(state);
        return new Scope(state);
    }

    public static boolean isActive() {
        return CURRENT.get() != null;
    }

    public static boolean allows(KeyMapping mapping) {
        State state = CURRENT.get();
        return state == null || state.target() == mapping;
    }

    public static boolean shouldMask(KeyMapping mapping, InputConstants.Key actualKey) {
        State state = CURRENT.get();
        return state != null && state.target() != mapping && state.key().equals(actualKey);
    }
}
```

- [ ] **Step 4: Extend mapping gates**

在 `KeyMappingClickMixin.getKey` 的 RETURN 注入中，将 `shouldMask` 命中的返回值替换为 `InputConstants.UNKNOWN`。`setDown(true)`、`matches` 和 `matchesMouse` 在事务内对非目标返回或取消。`KeyMappingLookupMixin` 在调用原匹配前检查 `SyntheticInputContext.allows(mapping)`；`ForgeKeyMappingMixin.isActiveAndMatches` 在事务内直接返回“目标身份且事件键等于事务键”。所有 Mixin helper 保持 `private static @Unique`。

- [ ] **Step 5: Run regression test**

Run: `.\gradlew.bat keyWheelRegressionTest --no-daemon --offline`

Expected: PASS，且已有锁定隔离测试继续通过。

- [ ] **Step 6: Commit**

```powershell
git add src/main/java/com/example/keywheel/input/SyntheticInputContext.java src/main/java/com/example/keywheel/mixin/KeyMappingClickMixin.java src/main/java/com/example/keywheel/mixin/KeyMappingLookupMixin.java src/main/java/com/example/keywheel/mixin/ForgeKeyMappingMixin.java src/test/java/com/example/keywheel/input/KeyWheelRegressionTest.java
git commit -m "Add scoped synthetic input isolation"
```

### Task 2: 原版键盘和鼠标输入重放

**Files:**
- Create: `src/main/java/com/example/keywheel/input/SyntheticInputReplayer.java`
- Create: `src/main/java/com/example/keywheel/mixin/MouseHandlerInvoker.java`
- Modify: `src/main/java/com/example/keywheel/mixin/KeyboardHandlerMixin.java`
- Modify: `src/main/java/com/example/keywheel/mixin/MouseHandlerMixin.java`
- Modify: `src/main/resources/keywheel.mixins.json`
- Test: `src/test/java/com/example/keywheel/input/KeyWheelRegressionTest.java`

**Interfaces:**
- Consumes: `SyntheticInputContext.begin(...)` 和 `WheelActionBridge`。
- Produces: `SyntheticInputReplayer.supports(InputConstants.Key)` 与 `replay(KeyMapping, InputConstants.Key, int)`。

- [ ] **Step 1: Write the failing replayer test**

增加 `requireSyntheticInputReplayer()`：反射验证 `KEYSYM`、`MOUSE` 支持且 `SCANCODE` 不支持；确认 `MouseHandlerInvoker.keywheel$invokeOnPress` 存在、Mixin JSON 已注册；确认键鼠 Handler Mixin 引用了 `SyntheticInputContext`，以便合成事务绕过物理轮盘拦截和保持释放入口。

```java
private static void requireSyntheticInputReplayer() throws Exception {
    Class<?> replayer = Class.forName("com.example.keywheel.input.SyntheticInputReplayer");
    var supports = replayer.getDeclaredMethod("supports", InputConstants.Key.class);
    require((boolean) supports.invoke(null, InputConstants.Type.KEYSYM.getOrCreate(71)), "keysym replay must be supported");
    require((boolean) supports.invoke(null, InputConstants.Type.MOUSE.getOrCreate(0)), "mouse replay must be supported");
    require(!(boolean) supports.invoke(null, InputConstants.Type.SCANCODE.getOrCreate(1)), "scancode replay must fall back");
    requireDeclaredMethod("com.example.keywheel.mixin.MouseHandlerInvoker", "keywheel$invokeOnPress");
    require(resource("keywheel.mixins.json").contains("\"MouseHandlerInvoker\""), "mouse invoker must be registered");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat keyWheelRegressionTest --no-daemon --offline`

Expected: FAIL，原因是 replayer 与 mouse invoker 尚不存在。

- [ ] **Step 3: Implement the original-handler replayer**

`SyntheticInputReplayer.replay` 对键盘调用公开的 `Minecraft.keyboardHandler.keyPress(window, keyValue, 0, action, modifiers)`；对鼠标通过 `@Invoker("onPress")` 调用 `MouseHandler.onPress`。调用前检查类型与 Minecraft 实例，在事务和目标级 force-allow 中执行，并在 `finally` 清理两者。

```java
public static boolean replay(KeyMapping target, InputConstants.Key key, int action) {
    if (target == null || !supports(key)) return false;
    Minecraft mc = Minecraft.getInstance();
    if (mc == null || mc.getWindow() == null) return false;
    long window = mc.getWindow().getWindow();
    try (AutoCloseable ignored = SyntheticInputContext.begin(target, key)) {
        WheelActionBridge.addForceAllow(target);
        if (key.getType() == InputConstants.Type.KEYSYM) {
            mc.keyboardHandler.keyPress(window, key.getValue(), 0, action, modifierMask(target));
        } else {
            ((MouseHandlerInvoker) mc.mouseHandler)
                    .keywheel$invokeOnPress(window, key.getValue(), action, modifierMask(target));
        }
        return true;
    } catch (Exception exception) {
        KeyWheel.LOG.error("Failed to replay selected wheel input {}", target.getName(), exception);
        return true;
    } finally {
        WheelActionBridge.clearForceAllow();
    }
}
```

`modifierMask` 将 Forge `KeyModifier.CONTROL/SHIFT/ALT/NONE` 映射到 GLFW modifier bit。异常发生在输入分发开始后时返回已处理，避免回退造成双触发；只有不支持或无法取得客户端入口时返回 `false`。

- [ ] **Step 4: Bypass wheel interception during replay**

在 `KeyboardHandlerMixin` 和 `MouseHandlerMixin` HEAD 首行检查 `SyntheticInputContext.isActive()`；为真时直接退出注入回调，不取消底层方法、不调用 `releaseHeldOnInput`。真实玩家输入保持现有顺序。

- [ ] **Step 5: Run regression test**

Run: `.\gradlew.bat keyWheelRegressionTest --no-daemon --offline`

Expected: PASS，Mixin 注解处理无错误。

- [ ] **Step 6: Commit**

```powershell
git add src/main/java/com/example/keywheel/input/SyntheticInputReplayer.java src/main/java/com/example/keywheel/mixin/MouseHandlerInvoker.java src/main/java/com/example/keywheel/mixin/KeyboardHandlerMixin.java src/main/java/com/example/keywheel/mixin/MouseHandlerMixin.java src/main/resources/keywheel.mixins.json src/test/java/com/example/keywheel/input/KeyWheelRegressionTest.java
git commit -m "Replay selected wheel inputs through vanilla handlers"
```

### Task 3: 延迟轮盘动作与释放生命周期

**Files:**
- Modify: `src/main/java/com/example/keywheel/input/ActionExecutor.java`
- Modify: `src/main/java/com/example/keywheel/input/LongPressWatcher.java`
- Test: `src/test/java/com/example/keywheel/input/KeyWheelRegressionTest.java`

**Interfaces:**
- Consumes: `SyntheticInputReplayer.supports(...)` 与 `replay(...)`。
- Produces: 延迟的 `runWheelAction`、一 Tick 后的合成释放、保持目标的合成释放和现有 fallback。

- [ ] **Step 1: Write the failing lifecycle test**

增加 `requireDeferredReplayLifecycle()`，检查 `ActionExecutor.runWheelAction` 字节码包含 `Minecraft.tell` 与 `SyntheticInputReplayer`，并反射验证纯策略 `shouldQueueReplay(boolean supported, boolean clientReady)`。继续断言 `LongPressWatcher` 的短按路径不引用 `runWheelAction`。

```java
private static void requireDeferredReplayLifecycle() throws Exception {
    Class<?> executor = Class.forName("com.example.keywheel.input.ActionExecutor");
    var policy = executor.getDeclaredMethod("shouldQueueReplay", boolean.class, boolean.class);
    policy.setAccessible(true);
    require((boolean) policy.invoke(null, true, true), "supported wheel actions must be deferred for replay");
    require(!(boolean) policy.invoke(null, false, true), "unsupported keys must use the current fallback");
    byte[] bytes = resourceBytes("com/example/keywheel/input/ActionExecutor.class");
    require(containsBytes(bytes, "SyntheticInputReplayer".getBytes(StandardCharsets.UTF_8)), "wheel actions must use the replayer");
    require(containsBytes(bytes, "tell".getBytes(StandardCharsets.UTF_8)), "wheel replay must run after the wheel closes");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat keyWheelRegressionTest --no-daemon --offline`

Expected: FAIL，原因是 `shouldQueueReplay` 或延迟重放引用不存在。

- [ ] **Step 3: Queue wheel presses after screen close**

`runWheelAction` 先捕获目标当前键和保持配置。支持重放且客户端可用时调用 `Minecraft.tell`；任务执行时再次验证目标仍在 `mc.options.keyMappings` 且仍绑定捕获键，然后调用 PRESS。验证失败时取消，类型不支持时使用现有 `execute` fallback。

```java
public static void runWheelAction(KeyMapping target) {
    if (target == null) return;
    InputConstants.Key key = target.getKey();
    boolean hold = KeyWheelConfig.isHoldEnabled(target.getName());
    Minecraft mc = Minecraft.getInstance();
    if (!shouldQueueReplay(SyntheticInputReplayer.supports(key), mc != null)) {
        execute(List.of(target), hold);
        return;
    }
    mc.tell(() -> replayWheelAction(mc, target, key, hold));
}
```

- [ ] **Step 4: Track replay releases without changing short press**

为合成一键动作保存 `(mapping, capturedKey)` 待释放项；`flushSetDown` 在下一 Tick 通过 replayer 发送 RELEASE，失败时直接 `setDown(false)`。保持集合改为身份映射并保存捕获键；下一真实 PRESS、配置关闭、成员移除或世界不可用时使用捕获键重放 RELEASE。`run` 与 `runBatch` 继续走当前直接 `clickCount/setDown` 路径。

- [ ] **Step 5: Run regression test**

Run: `.\gradlew.bat keyWheelRegressionTest --no-daemon --offline`

Expected: PASS，现有短按、锁定、保持和缓存测试全部通过。

- [ ] **Step 6: Commit**

```powershell
git add src/main/java/com/example/keywheel/input/ActionExecutor.java src/main/java/com/example/keywheel/input/LongPressWatcher.java src/test/java/com/example/keywheel/input/KeyWheelRegressionTest.java
git commit -m "Defer and release replayed wheel actions"
```

### Task 4: 完整验证与产物检查

**Files:**
- Modify only if a preceding verification exposes a defect.
- Test: `src/test/java/com/example/keywheel/input/KeyWheelRegressionTest.java`

**Interfaces:**
- Consumes: Tasks 1-3 的全部行为。
- Produces: 可供实机测试的最终 JAR。

- [ ] **Step 1: Scan for forbidden dedicated compatibility**

Run:

```powershell
Get-ChildItem src -Recurse -File | Select-String -Pattern 'PassiveSkillTree|skilltree|key\.display_skill_tree'
```

Expected: 仅设计或计划文档可以出现；`src/main` 与测试产物中不得出现专用标识。

- [ ] **Step 2: Run the full clean verification**

Run: `.\gradlew.bat clean check build --no-daemon --offline`

Expected: `BUILD SUCCESSFUL`，`keyWheelRegressionTest` 通过，Mixin 注解处理无错误。

- [ ] **Step 3: Verify final JAR structure and behavior markers**

核对 `C:\Users\Lenovo\Desktop\Ai_Run\output\keywheel-1.0.0.jar`：包含 `SyntheticInputContext`、`SyntheticInputReplayer`、`MouseHandlerInvoker`、Mixin JSON 和 refmap；不包含专用模组 ID；记录 SHA-256。

- [ ] **Step 4: Verify repository cleanliness and diff quality**

Run:

```powershell
git diff --check
git status --short
```

Expected: 无空白错误；仅计划内修改存在，或在提交后工作区干净。

- [ ] **Step 5: Produce the test JAR handoff**

向用户提供输出 JAR、SHA-256 和实机步骤：在 Passive Skill Tree 功能与另一个功能共用物理键时，从轮盘选择技能树，确认技能树打开且另一个功能未触发；再验证短按、锁定和保持长按。
