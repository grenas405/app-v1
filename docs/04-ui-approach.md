# 04 — UI Approach

## Options compared

| Approach | Status (Nov 2026) | Fits the pure-function style? | Verdict |
|----------|-------------------|-------------------------------|---------|
| **Jetpack Compose + Material 3** | Google's default. Android UI is officially "Compose-first" (May 2026). Stable 1.12 (Aug 2026) | **Yes.** A composable is a function from state to UI | ✅ **Use this** |
| XML Views + ViewBinding | Maintenance mode. Layout Editor and Navigation Editor get no new features | No. Mutable widget trees, imperative updates | ❌ Learn only to read legacy code |
| Compose Multiplatform (JetBrains) | Stable on Android and iOS. Shares Compose UI code with iOS | Yes, same model as Compose | ⏭ Phase 2 option, if you want shared UI |
| KMP shared logic + native SwiftUI on iOS | Mature | Yes. SwiftUI is also declarative | ⏭ Phase 2 option, if you want native iOS feel |
| Flutter / React Native | Mature, but not Kotlin | — | Out of scope for this project |

**Recommended stack:** single Activity · Jetpack Compose · Material 3 · Navigation 3 (once there's more than one screen) · `StateFlow` + `collectAsStateWithLifecycle` · unidirectional data flow.

## Why Compose suits this architecture

A composable is (roughly) a function `(state) -> UI`. When state changes, Compose re-calls the function with the new state, a step called *recomposition*. This mirrors the core:

```
reduce : (State, Action) -> State        // core
Screen : (State, (Action) -> Unit) -> UI // UI
```

## Pattern: stateful route, stateless screen

Only the outermost composable for a screen (the **route**) knows about the ViewModel. Everything below it takes **plain values plus lambdas**, which makes it previewable, testable, and reusable.

```kotlin
// ui/tasks/TasksRoute.kt — stateful edge (shell)
@Composable
fun TasksRoute(
    store: TaskStore,
    vm: TasksViewModel = viewModel { TasksViewModel(store) },
) {
    val state by vm.state.collectAsStateWithLifecycle()
    TasksScreen(state = state, onAction = vm::dispatch)
}
```

```kotlin
// ui/tasks/TasksScreen.kt — stateless: same input → same UI
@Composable
fun TasksScreen(
    state: TasksState,
    onAction: (TasksAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Tasks (${pendingCount(state.tasks)} left)") }) },
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            DraftField(
                text = state.draft,
                error = state.error,
                onTextChange = { onAction(TasksAction.DraftChanged(it)) },
                onSubmit = { onAction(TasksAction.Submit) },
            )
            Spacer(Modifier.height(12.dp))
            when {
                state.loading -> CircularProgressIndicator()
                state.tasks.isEmpty() -> Text("Nothing to do 🎉")
                else -> TaskList(
                    tasks = state.tasks,
                    onToggle = { onAction(TasksAction.Toggle(it)) },
                    onRemove = { onAction(TasksAction.Remove(it)) },
                )
            }
        }
    }
}
```

```kotlin
// ui/tasks/DraftField.kt
@Composable
fun DraftField(
    text: String,
    error: String?,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        label = { Text("New task") },
        isError = error != null,
        supportingText = { error?.let { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        modifier = modifier.fillMaxWidth(),
    )
}
```

```kotlin
// ui/tasks/TaskRow.kt
@Composable
fun TaskList(
    tasks: List<Task>,
    onToggle: (TaskId) -> Unit,
    onRemove: (TaskId) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier) {
        items(tasks, key = { it.id.value }) { task ->
            TaskRow(task = task, onToggle = onToggle, onRemove = onRemove)
        }
    }
}

@Composable
fun TaskRow(
    task: Task,
    onToggle: (TaskId) -> Unit,
    onRemove: (TaskId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = task.done, onCheckedChange = { onToggle(task.id) })
        Text(
            text = task.title,
            style = if (task.done) LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough)
                    else LocalTextStyle.current,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onRemove(task.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete ${task.title}")
        }
    }
}
```

### Previews: free because the screen is stateless

```kotlin
private val sample = TasksState(
    tasks = listOf(Task(TaskId(1), "Buy milk", false), Task(TaskId(2), "Walk dog", true)),
    loading = false,
)

@Preview(showBackground = true)
@Composable
private fun TasksScreenPreview() {
    TasksTheme { TasksScreen(state = sample, onAction = {}) }
}

@Preview(showBackground = true, widthDp = 840)   // tablet / unfolded width
@Composable
private fun TasksScreenWidePreview() {
    TasksTheme { TasksScreen(state = sample, onAction = {}) }
}
```

## Composable rules of thumb

1. **Parameters in, lambdas out.** Use `state` down and `onX` callbacks up (*state hoisting*).
2. Always accept `modifier: Modifier = Modifier` and apply it to the root element.
3. **No business logic in composables.** Call core functions such as `pendingCount(state.tasks)`. Don't recompute rules inline.
4. **Ephemeral UI state may stay local** with `remember` / `rememberSaveable`. Examples: a dropdown's expanded flag, scroll position, animation progress. It doesn't belong in `TasksState`. See [05 — trade-offs](05-tradeoffs-and-limits.md#5-compose-has-its-own-local-state-and-effects).
5. Side effects inside composables only through effect APIs (`LaunchedEffect`, `DisposableEffect`), never directly in the function body.
6. Give `LazyColumn` items stable `key`s.

## Navigation (when screen #2 arrives)

Use **Navigation 3** (stable, 1.2 as of Oct 2026). Its back stack is **a plain list you own**, which matches the "state is data" approach:

```kotlin
sealed interface Screen {
    data object TaskList : Screen
    data class TaskDetail(val id: Long) : Screen
}

// pure navigation functions
fun push(stack: List<Screen>, screen: Screen): List<Screen> = stack + screen
fun pop(stack: List<Screen>): List<Screen> = if (stack.size > 1) stack.dropLast(1) else stack
```

In practice Nav3 gives you a `NavBackStack` (a snapshot-state list) that you mutate in the shell. Keep the *decisions* (where to go) in pure functions and the *mutation* at the route level. Nav3 also works in Compose Multiplatform, so the same model carries over to iOS.

## Theming

- Material 3 `MaterialTheme` with a `TasksTheme` wrapper in `ui/theme/`.
- On Android 12+, support **dynamic color** (`dynamicLightColorScheme(context)`), and fall back to your own scheme on older versions.
- Light and dark from `isSystemInDarkTheme()`.

## Edge-to-edge and adaptive layouts

- `enableEdgeToEdge()` in `MainActivity`. Edge-to-edge is enforced when targeting API 35+, and `Scaffold` padding handles insets.
- **Android 17 / target 37:** apps can no longer opt out of orientation and resizability on large screens (smallest width ≥ 600dp). Design for wide windows from the start:
  - Use `currentWindowAdaptiveInfo()` and window size classes to switch between one-pane and list-detail layouts.
  - Compose 1.11+ adds `Grid`, and `FlexBox` is approaching stable. Both help with wide layouts.
  - Google Play has said it will scrutinize foldable and large-screen quality more closely in 2027.

## UI testing

- **Most behavior is tested in `reduce`** (JVM, fast).
- Compose UI tests (`createComposeRule`) in `androidTest/` verify wiring, for example "tapping the checkbox calls `onToggle` with the right id". These need an emulator or device, so run them on the Windows host.
- Screenshot tests (Compose Preview Screenshot Testing) are optional later.
