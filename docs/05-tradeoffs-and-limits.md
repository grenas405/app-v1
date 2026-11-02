# 05 — Trade-offs: Where "Pure & Composable" Meets Android's Limits

Android is a lifecycle-driven, callback-heavy, object-oriented platform. A functional core works well inside it, but in some places the platform pushes back. Each entry covers:

- **Symptom:** what you'll run into
- **Why:** the underlying cause
- **Mitigation:** what to do
- **When to bend the rule:** the point where purity costs more than it's worth

---

## 1. Process death and configuration changes

**Symptom.** The user types a draft, switches apps, and Android kills the process to reclaim memory. On return, the draft is gone. A `ViewModel` survives rotation but **not** process death.

**Why.** Your `TasksState` lives in memory. Android restores only what you saved into a `Bundle`, which is small, must be serializable, and has size limits around a few hundred KB.

**Mitigation.**
- Persist durable data (the task list) via effects. That part already works.
- Save small, transient UI state (the `draft`) with `SavedStateHandle` in the ViewModel, or `rememberSaveable` in the composable.
- Keep state types serializable-friendly: data classes of primitives, lists, and value classes.

**When to bend.** Pure `reduce` doesn't know about the Bundle. The shell has to decide which fields to save and restore. That's acceptable: it's an explicit translation step at the edge.

## 2. APIs that need an Activity or Context

**Symptom.** Requesting a permission, opening the camera, picking a file, sharing, or launching another app all need an `Activity`, `ActivityResultLauncher`, or `Context`. None of these can be called from `reduce` or even easily from a ViewModel.

**Why.** These APIs are tied to the UI lifecycle, and their results arrive through callbacks registered on the Activity.

**Mitigation.**
- Still model the *intent* as an `Effect` (`Effect.RequestCameraPermission`), but **run it in the UI layer** with `rememberLauncherForActivityResult`, then feed the result back as an action (`PermissionResult(granted)`).
- Represent one-shot UI effects (snackbars, navigation, launching intents) as a `Channel`/`SharedFlow` of events, or as state the UI consumes and acknowledges. Pick one style and stay consistent.

**When to bend.** For trivial cases, such as "open this URL", calling `LocalUriHandler.current.openUri(url)` directly from a click lambda is fine. Routing it through reduce → effect → UI adds ceremony without making anything easier to test.

## 3. Performance: immutable data copies

**Symptom.** Lag when lists grow into the thousands, or high GC churn during fast input such as drags or every keystroke.

**Why.** `tasks.map { … }` allocates a new list on every change, which is O(n). Usually that's negligible, but it adds up in hot paths.

**Mitigation.**
- Measure first with the Profiler, Macrobenchmark, or `androidx.tracing`. At "to-do app" sizes this almost never matters.
- Use `kotlinx.collections.immutable` (`PersistentList`) for cheaper structural sharing.
- Move heavy *pure* computations off the main thread: `withContext(Dispatchers.Default) { expensivePureFn(x) }`. Pure doesn't mean cheap.
- Keep high-frequency state, such as drag offsets and animation values, local to the composable instead of in the global state.

**When to bend.** Performance-critical inner loops (custom drawing, audio, games) can use local mutable arrays **inside** a function, as long as nothing mutable escapes. The function stays pure from the outside.

## 4. Concurrency and ordering

**Symptom.** Two saves run concurrently and the older one finishes last, overwriting newer data. Or a `Loaded` result arrives after the user has already added tasks, and wipes them.

**Why.** `reduce` is pure and sequential, but the effects it returns run asynchronously in any order.

**Mitigation.**
- Serialize writes. Use a single `Mutex`, a `Channel` consumer, or `collectLatest`/`conflate` on a "save requested" flow so only the latest state gets written.
- Make results idempotent or versioned. For example, ignore `Loaded` once `loading == false`, and attach a request id to async results.
- If you dispatch from background threads, switch to `_state.update { }`. That makes the read-modify-write atomic, but `update` may retry, so **don't run effects inside it**. Collect them and run them afterward.
- Never swallow `CancellationException`. `runCatching { suspendFn() }` catches it and breaks structured concurrency, so use it only around non-suspending code (like `decodeTasks`) or rethrow cancellation.

**When to bend.** Consider an existing reactive source such as Room's `Flow<List<Task>>`. Making the database the source of truth, and deriving UI state with `combine`/`map`, often beats a hand-rolled reducer for data-heavy screens.

## 5. Compose has its own local state and effects

**Symptom.** You try to put scroll position, focus, text selection, and animation state into `TasksState`, and the code becomes awkward and slow.

**Why.** Compose is a declarative UI with an internal memory (`remember`) and a lifecycle-aware effect system (`LaunchedEffect`). Some state is inherently *view* state.

**Mitigation.** Split state deliberately:

| Lives in core state (`TasksState`) | Lives in the composable (`remember…`) |
|-----------------------------------|---------------------------------------|
| Domain data, validation errors, loading flags | Scroll/lazy-list state, focus, animation, "menu expanded" |
| Anything that affects business decisions | Anything only the rendering cares about |

**When to bend.** Use `LaunchedEffect` for UI-only side effects, such as scrolling to the newly added item.

## 6. Framework classes require inheritance and callbacks

**Symptom.** `Activity`, `Service`, `BroadcastReceiver`, WorkManager `CoroutineWorker`, `FirebaseMessagingService`, widgets, and tiles all must be *subclasses* with *override* callbacks.

**Why.** Android's component model is object-oriented, and the OS instantiates these classes itself.

**Mitigation.** Keep each subclass a **thin adapter**: unpack the inputs, call a pure function, and perform the resulting effect.

```kotlin
class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val tasks = fileTaskStore(File(applicationContext.filesDir, "tasks.json")).load()
        val message = reminderMessage(tasks)            // pure, in :core
        if (message != null) showNotification(applicationContext, message)
        return Result.success()
    }
}
```

## 7. Dependency wiring at scale

**Symptom.** Passing `TaskStore` and similar dependencies by hand through `MainActivity` gets tedious once there are 10+ dependencies and several screens.

**Why.** Manual composition (functions as dependencies) has no automatic graph.

**Mitigation.**
- **Small app (this project):** a manual `AppContainer` built in `Application.onCreate` is fine and very readable.
- **Growing app:** adopt a DI framework. Hilt is Google's default (Android-only, annotation-heavy). Koin is runtime and KMP-friendly. Metro is compile-time and KMP-friendly.

**Trade-off.** DI frameworks add annotations and some build-time cost, but remove boilerplate. Records-of-functions also don't play as nicely with Hilt as interfaces do. If you adopt Hilt, switch `TaskStore` to an `interface`.

## 8. Error handling: Result types vs exceptions

**Symptom.** The pure core uses `Validation` / `Result`, but Android and Java APIs throw `IOException`, `SecurityException`, and others.

**Mitigation.**
- Convert exceptions to values **at the adapter boundary** (`fileTaskStore`), then pass values inward.
- Keep the error types meaningful, e.g. `sealed interface LoadError { NotFound; Corrupt; … }`, rather than `Throwable`.
- Don't wrap everything. Programming bugs (null where impossible, index out of range) should crash loudly in debug.

## 9. Library interop

**Symptom.** Room entities, Retrofit/Ktor responses, and some SDKs expect mutable classes, annotations, or reflection.

**Mitigation.** Keep a **DTO/entity layer at the edge** with pure `toDomain`/`toDto` mappers (see `TaskJson.kt`). The core never sees library types.

**Cost.** Extra mapping code. It's worth it once you have two or more storage or network formats, and optional before that.

## 10. Code shrinking (R8) and reflection

**Symptom.** A release build crashes with missing classes, but debug works.

**Why.** R8 strips and renames code that's reached only through reflection.

**Mitigation.** Prefer compile-time tools: the kotlinx.serialization plugin ships its own rules, and KSP generates code instead of using reflection. Test a `release` build early. AGP 9.3's `analyzeReleaseR8Config` task can audit your keep rules.

## 11. Boilerplate of the reducer pattern

**Symptom.** A simple screen ends up with State + Action + Effect + reduce + tests for three buttons.

**Mitigation, a sliding scale:**
1. **Tiny screen:** ViewModel methods that each call a pure function, `fun toggle(id) { _state.update { it.copy(tasks = toggleTask(it.tasks, id)) } }`. The logic is still pure, with no Action type.
2. **Medium screen:** full `reduce` + `Effect` (this plan's default).
3. **Shared app-wide state:** consider Jetpack **AppState** (alpha, Oct 2026), which brings Compose's state model to repositories and ViewModels. Evaluate it once it's beta.

Don't force every screen into the heaviest pattern.

## 12. Not everything is unit-testable

Compose layout, gestures, accessibility, real devices, and OEM quirks still need instrumented tests or manual testing on the Windows host. The pure core covers the logic but tells you nothing about pixels.

---

## Summary table

| Limitation | Severity for this project | Default response |
|------------|---------------------------|------------------|
| Process death | Medium | `SavedStateHandle` for draft; persist data via effects |
| Activity-bound APIs | Medium (when you add permissions) | Effect → run in UI → result action |
| Immutable copy cost | Low | Measure; `PersistentList` if needed |
| Async ordering | Medium | Serialize writes; version results |
| Compose local state | Low | Split view-state vs domain state |
| Framework inheritance | Low | Thin adapter subclasses |
| DI at scale | Low now | Manual container → Koin/Metro/Hilt later |
| Exceptions vs values | Low | Convert at adapters |
| Library interop | Low | DTO mappers |
| R8/reflection | Low | Codegen over reflection; test release builds |
| Reducer boilerplate | Medium | Use the sliding scale |
| UI testing | Medium | Instrumented tests on the host |
