# 02 — Architecture: Functional Core, Imperative Shell

## The rule

```
┌──────────────────────────── Imperative shell (thin) ────────────────────────────┐
│  Activity · ViewModel · file/network adapters · Compose runtime · OS callbacks  │
│                                                                                 │
│        ┌──────────────────── Functional core (most code) ─────────────────┐     │
│        │  data classes · validation · reduce(state, action) · formatting  │     │
│        │  encode/decode · selectors       — NO I/O, NO android.* imports  │     │
│        └──────────────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────────────────┘
```

**The core**
- Every function returns its result. It never mutates its inputs and never reads clocks, files, the network, or random numbers.
- Anything that varies, such as the current time, a new ID, or the locale, comes in as a **parameter**.
- Side effects are **described as data** (`Effect`) and returned to the shell. The core never performs them.

**The shell**
- Turns events into actions, calls `reduce`, stores the new state, and runs the returned effects.
- Owns everything tied to a lifecycle or a thread.
- Is small enough that a quick read is enough to trust it. Most of the tests target the core.

## Data flow (unidirectional)

```
   user taps ─▶ Composable ─onAction(action)─▶ ViewModel.dispatch
                    ▲                                 │
                    │ state                           ▼
               StateFlow ◀── new state ── reduce(state, action) ──▶ effects ─▶ shell runs them
                                                                       │
                                     result (e.g. Loaded) ◀────────────┘  (dispatched back as an action)
```

---

## 1. Domain types: plain immutable data

```kotlin
// core/model/Task.kt
package io.github.grenas405.tasks.core.model

@JvmInline
value class TaskId(val value: Long)

data class Task(
    val id: TaskId,
    val title: String,
    val done: Boolean,
)
```

## 2. Small pure functions over that data

Each function does one thing, takes everything it needs as arguments, and returns a new value.

```kotlin
// core/model/TaskList.kt
package io.github.grenas405.tasks.core.model

fun newTask(id: TaskId, title: String): Task =
    Task(id = id, title = title, done = false)

fun addTask(tasks: List<Task>, task: Task): List<Task> =
    tasks + task

fun toggleTask(tasks: List<Task>, id: TaskId): List<Task> =
    tasks.map { if (it.id == id) it.copy(done = !it.done) else it }

fun removeTask(tasks: List<Task>, id: TaskId): List<Task> =
    tasks.filterNot { it.id == id }

fun pendingCount(tasks: List<Task>): Int =
    tasks.count { !it.done }

fun nextIdAfter(tasks: List<Task>): Long =
    (tasks.maxOfOrNull { it.id.value } ?: 0L) + 1
```

## 3. Composition helpers

Keep these to a handful of explicit helpers. You don't need a functional-programming library.

```kotlin
// core/fp/Compose.kt
package io.github.grenas405.tasks.core.fp

/** f andThen g  ==  { x -> g(f(x)) } */
infix fun <A, B, C> ((A) -> B).andThen(next: (B) -> C): (A) -> C =
    { a -> next(this(a)) }
```

```kotlin
// core/fp/Validation.kt
package io.github.grenas405.tasks.core.fp

sealed interface Validation<out T> {
    data class Valid<T>(val value: T) : Validation<T>
    data class Invalid(val errors: List<String>) : Validation<Nothing>
}

fun <A, B> Validation<A>.flatMap(next: (A) -> Validation<B>): Validation<B> =
    when (this) {
        is Validation.Valid -> next(value)
        is Validation.Invalid -> this
    }
```

## 4. Validation built by composing small checks

```kotlin
// core/model/TitleRules.kt
package io.github.grenas405.tasks.core.model

import io.github.grenas405.tasks.core.fp.Validation
import io.github.grenas405.tasks.core.fp.andThen
import io.github.grenas405.tasks.core.fp.flatMap

const val MAX_TITLE_LENGTH = 80

fun trimTitle(raw: String): String = raw.trim()

fun collapseWhitespace(text: String): String = text.replace(Regex("\\s+"), " ")

val normalizeTitle: (String) -> String = ::trimTitle andThen ::collapseWhitespace

fun checkNotBlank(text: String): Validation<String> =
    if (text.isBlank()) Validation.Invalid(listOf("Title can't be empty"))
    else Validation.Valid(text)

fun checkMaxLength(max: Int): (String) -> Validation<String> = { text ->
    if (text.length > max) Validation.Invalid(listOf("Title must be $max characters or fewer"))
    else Validation.Valid(text)
}

fun validateTitle(raw: String): Validation<String> =
    Validation.Valid(normalizeTitle(raw))
        .flatMap(::checkNotBlank)
        .flatMap(checkMaxLength(MAX_TITLE_LENGTH))
```

`checkMaxLength(80)` *returns* a function. This is how to configure behavior without classes or global settings.

## 5. Screen state, actions, effects, and the reducer

```kotlin
// core/tasks/TasksState.kt
package io.github.grenas405.tasks.core.tasks

import io.github.grenas405.tasks.core.model.Task
import io.github.grenas405.tasks.core.model.TaskId

data class TasksState(
    val tasks: List<Task> = emptyList(),
    val draft: String = "",
    val error: String? = null,
    val nextId: Long = 1,
    val loading: Boolean = true,
)

sealed interface TasksAction {
    data class Loaded(val tasks: List<Task>) : TasksAction
    data class DraftChanged(val text: String) : TasksAction
    data object Submit : TasksAction
    data class Toggle(val id: TaskId) : TasksAction
    data class Remove(val id: TaskId) : TasksAction
}

/** Side effects described as data. The shell decides how to run them. */
sealed interface Effect {
    data class SaveTasks(val tasks: List<Task>) : Effect
}

data class Next(
    val state: TasksState,
    val effects: List<Effect> = emptyList(),
)
```

```kotlin
// core/tasks/Reduce.kt
package io.github.grenas405.tasks.core.tasks

import io.github.grenas405.tasks.core.fp.Validation
import io.github.grenas405.tasks.core.model.*

fun reduce(state: TasksState, action: TasksAction): Next =
    when (action) {
        is TasksAction.Loaded -> Next(
            state.copy(tasks = action.tasks, nextId = nextIdAfter(action.tasks), loading = false)
        )
        is TasksAction.DraftChanged -> Next(state.copy(draft = action.text, error = null))
        TasksAction.Submit -> submitDraft(state)
        is TasksAction.Toggle -> withSave(state.copy(tasks = toggleTask(state.tasks, action.id)))
        is TasksAction.Remove -> withSave(state.copy(tasks = removeTask(state.tasks, action.id)))
    }

private fun submitDraft(state: TasksState): Next =
    when (val result = validateTitle(state.draft)) {
        is Validation.Invalid -> Next(state.copy(error = result.errors.first()))
        is Validation.Valid -> withSave(
            state.copy(
                tasks = addTask(state.tasks, newTask(TaskId(state.nextId), result.value)),
                draft = "",
                error = null,
                nextId = state.nextId + 1,
            )
        )
    }

private fun withSave(state: TasksState): Next =
    Next(state, listOf(Effect.SaveTasks(state.tasks)))
```

The whole screen's behavior lives in one function, and that function can be tested without any setup.

## 6. Tests (plain JUnit, run in the headless VM)

```kotlin
// core/src/test/kotlin/.../ReduceTest.kt
class ReduceTest {

    @Test
    fun `valid draft is normalized, added, and saved`() {
        val next = reduce(TasksState(draft = "  Buy   milk "), TasksAction.Submit)

        assertEquals(listOf(Task(TaskId(1), "Buy milk", done = false)), next.state.tasks)
        assertEquals("", next.state.draft)
        assertEquals(listOf(Effect.SaveTasks(next.state.tasks)), next.effects)
    }

    @Test
    fun `blank draft sets an error and saves nothing`() {
        val next = reduce(TasksState(draft = "   "), TasksAction.Submit)

        assertEquals("Title can't be empty", next.state.error)
        assertTrue(next.effects.isEmpty())
    }
}
```

## 7. Encoding and decoding: pure functions at the edge of the core

```kotlin
// data/TaskJson.kt
package io.github.grenas405.tasks.data

import io.github.grenas405.tasks.core.model.Task
import io.github.grenas405.tasks.core.model.TaskId
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TaskDto(val id: Long, val title: String, val done: Boolean)

fun toDto(task: Task): TaskDto = TaskDto(task.id.value, task.title, task.done)
fun toDomain(dto: TaskDto): Task = Task(TaskId(dto.id), dto.title, dto.done)

private val json = Json { ignoreUnknownKeys = true }

fun encodeTasks(tasks: List<Task>): String =
    json.encodeToString(tasks.map(::toDto))

fun decodeTasks(text: String): Result<List<Task>> =
    runCatching { json.decodeFromString<List<TaskDto>>(text).map(::toDomain) }
```

The DTO keeps serialization annotations out of the domain model. Changing the storage format then can't break the core.

## 8. The shell: dependencies as plain functions

Instead of interface + class + DI framework, a dependency is a **record of functions**:

```kotlin
// data/TaskStore.kt
package io.github.grenas405.tasks.data

data class TaskStore(
    val load: suspend () -> List<Task>,
    val save: suspend (List<Task>) -> Unit,
)

fun fileTaskStore(file: File, io: CoroutineDispatcher = Dispatchers.IO): TaskStore =
    TaskStore(
        load = {
            withContext(io) {
                if (file.exists()) decodeTasks(file.readText()).getOrDefault(emptyList())
                else emptyList()
            }
        },
        save = { tasks -> withContext(io) { file.writeText(encodeTasks(tasks)) } },
    )

/** For tests and Compose Previews. */
fun inMemoryTaskStore(initial: List<Task> = emptyList()): TaskStore {
    var saved = initial
    return TaskStore(load = { saved }, save = { saved = it })
}
```

## 9. The ViewModel: the only stateful class

```kotlin
// ui/tasks/TasksViewModel.kt
class TasksViewModel(private val store: TaskStore) : ViewModel() {

    private val _state = MutableStateFlow(TasksState())
    val state: StateFlow<TasksState> = _state.asStateFlow()

    init {
        viewModelScope.launch { dispatch(TasksAction.Loaded(store.load())) }
    }

    fun dispatch(action: TasksAction) {
        val next = reduce(_state.value, action)   // pure
        _state.value = next.state                 // side effect: publish state
        next.effects.forEach(::run)               // side effect: run effects
    }

    private fun run(effect: Effect) {
        when (effect) {
            is Effect.SaveTasks -> viewModelScope.launch { store.save(effect.tasks) }
        }
    }
}
```

Because `dispatch` is called only on the main thread (UI events plus `viewModelScope`, which uses `Dispatchers.Main`), reading and then writing `_state.value` can't race. If you dispatch from other threads, use `_state.update { }` and collect the effects inside it. See [05 — trade-offs](05-tradeoffs-and-limits.md#4-concurrency-and-ordering).

## 10. Wiring: one Activity, manual composition root

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = fileTaskStore(File(filesDir, "tasks.json"))
        setContent {
            TasksTheme { TasksRoute(store) }
        }
    }
}
```

The UI side of `TasksRoute` is in [04 — UI approach](04-ui-approach.md).

## Conventions checklist

- [ ] Nothing under `core/` imports `android.*`, `androidx.*`, `java.io`, or coroutines.
- [ ] Core functions are top-level `fun`s, not methods on manager or service classes.
- [ ] Time, IDs, and randomness are parameters, e.g. `fun markDone(task: Task, at: Instant)`.
- [ ] Every user intent is an `Action`, and every side effect is an `Effect`.
- [ ] Composables take `state` plus lambdas. They don't take ViewModels below the route level.
- [ ] Every `reduce` branch has a unit test.
