# Short Press Input Replay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让单目标和批量短按功能通过目标隔离的原生键鼠输入回放触发，同时保留不支持类型的旧执行路径。

**Architecture:** `ActionExecutor` 增加统一的一次性短按执行入口，逐目标捕获物理键并同步发送 PRESS，随后复用现有一次性释放表在下一 Tick 发送 RELEASE。`SyntheticInputReplayer` 将未绑定键排除在支持范围外，所有回放继续使用现有 `SyntheticInputContext` 做目标隔离。

**Tech Stack:** Java 17、Minecraft 1.20.1、Forge 47.4.18、Mixin 0.8.5、现有 `keyWheelRegressionTest`。

## Global Constraints

- 短按时机、长按阈值、轮盘操作、交换模式、锁定和保持长按行为不变。
- 单目标短按只触发指定 `KeyMapping`；批量短按保持现有目标顺序。
- `KEYSYM` 和 `MOUSE` 使用原生回放；`SCANCODE`、未绑定键或客户端不可用时使用旧路径。
- 不加入第三方模组 ID、按键 ID、专用适配器或网络协议。
- JAR 只输出到 `C:\Users\Lenovo\Desktop\Ai_Run\output`，不部署到 `mods`，不上传 GitHub。

---

### Task 1: 将短按执行接入一次性原生回放

**Files:**
- Modify: `src/test/java/com/example/keywheel/input/KeyWheelRegressionTest.java`
- Modify: `src/main/java/com/example/keywheel/input/SyntheticInputReplayer.java`
- Modify: `src/main/java/com/example/keywheel/input/ActionExecutor.java`

**Interfaces:**
- Consumes: `SyntheticInputReplayer.supports(InputConstants.Key)`、`replay(KeyMapping, InputConstants.Key, int)`、现有 `pendingReplayReleases`。
- Produces: `ActionExecutor.executeOneShot(List<KeyMapping>)`、`shouldReplayShortPress(boolean, boolean)`。

- [ ] **Step 1: Write the failing regression test**

在回归入口调用 `requireShortPressReplayLifecycle()`，并添加：

```java
private static void requireShortPressReplayLifecycle() throws Exception {
    require(!SyntheticInputReplayer.supports(InputConstants.UNKNOWN),
            "unbound mappings must use the legacy short-press path");
    Class<?> executor = Class.forName("com.example.keywheel.input.ActionExecutor");
    var policy = executor.getDeclaredMethod("shouldReplayShortPress", boolean.class, boolean.class);
    policy.setAccessible(true);
    require((boolean) policy.invoke(null, true, true),
            "supported short presses must use native input replay");
    require(!(boolean) policy.invoke(null, false, true),
            "unsupported short presses must use the legacy path");
    require(!(boolean) policy.invoke(null, true, false),
            "short-press replay requires a ready client");
    requireDeclaredMethod("com.example.keywheel.input.ActionExecutor", "executeOneShot");
    byte[] executorBytes = resourceBytes("com/example/keywheel/input/ActionExecutor.class");
    require(containsBytes(executorBytes, "pendingReplayReleases".getBytes(StandardCharsets.UTF_8)),
            "short presses must share the one-shot release lifecycle");
}
```

- [ ] **Step 2: Run the test and verify RED**

Run: `.\gradlew.bat keyWheelRegressionTest --no-daemon --offline --console=plain`

Expected: FAIL because `InputConstants.UNKNOWN` is currently considered supported or `shouldReplayShortPress` / `executeOneShot` does not exist.

- [ ] **Step 3: Exclude unbound keys from replay support**

Change `SyntheticInputReplayer.supports` to:

```java
public static boolean supports(InputConstants.Key key) {
    return key != null
            && !InputConstants.UNKNOWN.equals(key)
            && (key.getType() == InputConstants.Type.KEYSYM
            || key.getType() == InputConstants.Type.MOUSE);
}
```

- [ ] **Step 4: Route `run` and `runBatch` through one-shot replay**

Change the public methods to call a new helper:

```java
public static void run(KeyMapping target) {
    if (target == null) return;
    executeOneShot(List.of(target));
}

public static void runBatch(List<KeyMapping> targets) {
    if (targets == null || targets.isEmpty()) return;
    executeOneShot(targets);
}
```

Add the policy and helper:

```java
static boolean shouldReplayShortPress(boolean supported, boolean clientReady) {
    return supported && clientReady;
}

private static void executeOneShot(List<KeyMapping> targets) {
    Minecraft mc = Minecraft.getInstance();
    for (KeyMapping target : targets) {
        if (target == null) continue;
        InputConstants.Key key = target.getKey();
        if (!shouldReplayShortPress(SyntheticInputReplayer.supports(key), mc != null)) {
            execute(List.of(target), false);
            continue;
        }
        if (!isCurrentMapping(mc, target, key)) continue;
        if (!SyntheticInputReplayer.replay(target, key, GLFW.GLFW_PRESS)) {
            execute(List.of(target), false);
            continue;
        }
        synchronized (pendingSetDownFalse) {
            heldMappings.remove(target);
            heldReplayKeys.remove(target);
            pendingReplayReleases.put(target, key);
        }
    }
}
```

- [ ] **Step 5: Run the regression test and verify GREEN**

Run: `.\gradlew.bat keyWheelRegressionTest --no-daemon --offline --console=plain`

Expected: PASS; existing short-press, wheel replay, lock and hold assertions remain green.

- [ ] **Step 6: Commit locally**

```powershell
git add src/main/java/com/example/keywheel/input/ActionExecutor.java src/main/java/com/example/keywheel/input/SyntheticInputReplayer.java src/test/java/com/example/keywheel/input/KeyWheelRegressionTest.java
git commit -m "Replay short press actions through native input"
```

### Task 2: 全量验证与本地 JAR

**Files:**
- Modify only if verification exposes a defect.

**Interfaces:**
- Consumes: Task 1 的短按回放与现有轮盘回放。
- Produces: 可实机测试的本地 `keywheel-1.0.0.jar`。

- [ ] **Step 1: Scan production sources for dedicated compatibility**

Run:

```powershell
git grep -n -E 'PassiveSkillTree|skilltree|key\.display_skill_tree' -- src/main
```

Expected: no output.

- [ ] **Step 2: Run full clean verification**

Run: `.\gradlew.bat clean check build --no-daemon --offline --console=plain`

Expected: `BUILD SUCCESSFUL`; `keyWheelRegressionTest` passes and Mixin annotation processing reports no errors.

- [ ] **Step 3: Validate the output JAR**

Check `C:\Users\Lenovo\Desktop\Ai_Run\output\keywheel-1.0.0.jar` contains `SyntheticInputContext.class`、`SyntheticInputReplayer.class`、`MouseHandlerInvoker.class`、`keywheel.mixins.json`、`keywheel.refmap.json` and `META-INF/mods.toml`; confirm `clientSideOnly=true`, record size and SHA-256.

- [ ] **Step 4: Verify repository and remote state**

Run:

```powershell
git diff --check
git status --short
git rev-parse origin/master
```

Expected: clean feature worktree after local commit; `origin/master` remains `608765ba83491ba423bfad8a9c6d9e46aa775460` and no push occurs.

- [ ] **Step 5: Hand off manual verification steps**

Verify in game: configure one raw-input `KeyMapping` as the swap-mode short-press target; tap its physical key and confirm only that target triggers. Then hold the key to open the wheel, choose a different target, and confirm short press, wheel selection, locking and hold behavior remain independent.
