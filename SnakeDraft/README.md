# SnakeDraft

A text-based fantasy football snake draft for 2 to 12 teams, built in model / view / controller style. The turn order is managed by our own linked queue, `TurnQueue<T>`.

## Project layout

```
src/snakedraft/Main.java                     creates view, model, controller; starts the draft
src/snakedraft/model/Player.java             immutable player (name, position)
src/snakedraft/model/Team.java               team name + roster
src/snakedraft/model/TurnQueue.java          our own singly linked FIFO queue
src/snakedraft/model/DraftModel.java         all draft state and rules (the snake)
src/snakedraft/model/PlayerPool.java         fixed pool of 40 players for the MVP
src/snakedraft/model/ModelDriver.java        tiny driver: runs a whole draft with no view
src/snakedraft/view/DraftView.java           prints the board and reads raw text
src/snakedraft/controller/DraftController.java   setup prompts, draft loop, input checks
test/...                                     JUnit 4 tests
```

## How to run

All commands below are for Eclipse. The project uses JUnit 4 (already on the build path in `.classpath`).

**Model only (no view):** right-click `src/snakedraft/model/ModelDriver.java` > Run As > Java Application. It runs a 3-team, 3-round draft with no view and no input, prints every pick (the order is A B C, C B A, A B C), shows that a pick after the draft ends is rejected, and prints the rosters.

**Model tests:** right-click the `test` folder > Run As > JUnit Test. `TurnQueueTest`, `PlayerTeamTest`, and `DraftModelTest` exercise the model with no view at all. `DraftControllerTest` drives the controller with scripted input.

**Thin view (full app):** right-click `src/snakedraft/Main.java` > Run As > Java Application, then type in the Console:

1. Number of teams, number of rounds, then a name for each team.
2. On each turn, type a player's name exactly as listed (case and spacing do not matter, so `patrick mahomes` and `PatrickMahomes` both work).
3. When every round is done, the final rosters print.

To stop early, send end of input (Ctrl+D on Mac/Linux, Ctrl+Z then Enter on Windows). The program prints the rosters so far and exits cleanly.

## Reflection

### What changed from our specification

| Area | Spec said | What we built | Why |
|---|---|---|---|
| `TurnQueue` storage | Data & State listed a `Deque<T>` field as the storage | Our own singly linked chain of private `Node<T>` objects with `head`, `tail`, `size` | The project requires us to write the queue ourselves. The refill design only ever adds at the back and removes at the front, so a singly linked chain with a tail pointer is enough and every operation is O(1). |
| Setup | The spec never said which class reads team count, rounds, and names | Added `static DraftModel setUp(DraftView, List<Player>)` to `DraftController` | Setup is input handling, so it belongs in the controller. `Main` still has no logic. We found this gap while writing `Main`. |
| `DraftView.readLine()` | `String readLine()` | `String readLine(String prompt)`, returns `null` at end of input | Every read needs a prompt, and returning `null` lets the controller exit cleanly on Ctrl+D instead of catching a Scanner exception. |
| `DraftView.showBoard` | `showBoard(Team, int round, int pick, List<Player>)` | Added `int totalRounds` | The board reads "Round 2 of 5", which is clearer for users. The view still gets plain values only. |
| `DraftView.showMessage` | Not in spec | Added | Needed for setup prompts and the "input ended" message. |
| `DraftModel.getTotalRounds()` | Not in spec | Added (read-only getter) | The controller needs it to pass to `showBoard`. |
| New classes | Not in spec | `PlayerPool` (fixed MVP data) and `ModelDriver` (view-free driver) | `PlayerPool` keeps the data out of `Main`. `ModelDriver` proves the model runs alone. |
| Name matching | "case and extra spaces ignored" | Normalizing lowercases and removes all whitespace | This also handles the "missing space" bad input from our scope (`PatrickMahomes`). |
| Pool size | Revised scope said the constructor rejects a pool smaller than teams x rounds | Built as planned | The draft can always finish every round. |
| `getPickNumber()` | "Overall pick number" | Returns the number of the next pick | Clear while the draft runs. After the draft ends it is total picks + 1. |
| Number input | Format check for "wrong type" | `handlePick` rejects pure numbers like `42` or `4.5` with a specific message; setup rejects `five`, `4.5`, and values too large for an `int` | Covers the numeric bad-input cases from our scope. Huge values are capped by range (2 to 12 teams, rounds limited by pool size). |
| Tests | One test list per class | Player and Team tests share `PlayerTeamTest`; added a dedicated snake-order test (A B C C B A), already-drafted, draft-over, duplicate-name, read-only list, pool-too-small, and end-of-input tests | These were the gaps we found reviewing our own test plan. |

### Did the model really stand alone?

Yes. Nothing in `snakedraft.model` imports anything from `view` or `controller`, and the model never prints or reads. `ModelDriver` and the three model test classes run a full draft without creating a view. The one thing we had to place on the controller side of the line was setup: reading the team count, rounds, and names is input, so it went into `DraftController.setUp`, which then builds the model through its normal constructor.

### What happened to our unknowns

- **Snake reversal with a deque (figured out).** We rebuild the queue at the end of each round: odd rounds enqueue teams 1 to N, even rounds N to 1. This needed only basic queue operations, and `snakeOrderIsABCCBA` tests it directly.
- **Loading real NFL player data (still open).** The MVP uses a fixed pool of 40 real player names in `PlayerPool`. Reading from a file and handling malformed rows is not built yet.
- **Trade confirmation and pending trades (still open, deferred).** Trading is a stretch goal and is not started.

### What doesn't work yet

- No position limits or roster balance rules, so a team can draft all QBs.
- No real data file; the pool is fixed.
- The full available-player list prints every turn, which gets long.
- No suggestions for misspelled names (the user just sees "No player named ...").
- No tests yet for `DraftView` output formatting beyond what `DraftControllerTest` checks.

## Next Steps

### What is left to finish the MVP (in order)

1. Shorten the board: show available players grouped by position instead of one long list. Owner: Jack.
2. Add view tests that check `showBoard`, `showError`, and `showFinalRosters` output. Owner: Shreyas.
3. Final pass on bad input from the scope doc using the full app, and fix anything found. Owner: Ishaan.

### Change feature: Undo the last pick

The user types `undo` on any turn and the most recent pick is reversed: the player goes back into the pool, comes off that team's roster, and that team is back on the clock (even if the undo crosses back into the previous round).

**Predicted to change:**

- `DraftModel`: new field `Deque<PickRecord> history` (or our own stack) recording team, player, and normalized key for each pick; `makePick` pushes a record; new `Player undoLastPick()` that pops it, puts the player back into `pool`, removes the key from `draftedNames`, decrements `pickNumber`, and rebuilds `turnQueue` for the correct round with a new private `rebuildQueueFor(int round, int picksMadeInRound)`. If the draft was over, `draftOver` goes back to false. Throws `IllegalStateException` if there is nothing to undo.
- `Team`: new `Player removeLastPlayer()`.
- `DraftController.handlePick`: recognize the word `undo` before calling `makePick`, call `model.undoLastPick()`, and catch the "nothing to undo" error.
- `DraftView`: new `showUndo(Team team, Player player)`.

**Predicted not to change:** `TurnQueue` (we rebuild it instead of adding a remove-from-back method), `Player`, `PlayerPool`, `Main`, and `ModelDriver`.

### Stretch goals

- **Committing to:** undo the last pick (our change feature), then position limits (for example max 2 QB, at least 1 K) if time allows.
- **Dropping:** auto-draft from a wish list, trading picks, ordering by previous-season performance, and loading real data from a file.

### New tests needed

- `undoLastPick`: undo mid-round restores the same team on the clock; undo at the first pick of a round goes back to the previous round and the correct team; undo after the draft is over reopens it; undo with no picks throws `IllegalStateException` and changes nothing; undone player is available again and can be re-drafted.
- `Team.removeLastPlayer`: normal case and empty-roster case.
- `DraftController.handlePick("undo")`: returns the right value and shows the right message, including `UNDO` and `  undo  `.
- If position limits are built: a pick that breaks a limit throws `IllegalArgumentException` with a reason and changes nothing.

### Who does what

| Item | Owner |
|---|---|
| Board grouped by position | Jack |
| View tests | Shreyas |
| Bad-input pass | Ishaan |
| Undo: `DraftModel` and `Team` changes | Luke |
| Undo: controller and view changes | Ishaan and Jack |
| Undo tests | Shreyas |
| Position limits (if time) | Luke |

## Attribution

| Class / test file | Who | How |
|---|---|---|
| `Player.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed, ran, and tested it |
| `Team.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed, ran, and tested it |
| `TurnQueue.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed, ran, and tested it |
| `DraftModel.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed, ran, and tested it |
| `PlayerPool.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed, ran, and tested it |
| `ModelDriver.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed, ran, and tested it |
| `DraftView.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed, ran, and tested it |
| `DraftController.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed, ran, and tested it |
| `Main.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed and ran it |
| `TurnQueueTest.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed, ran, and tested it |
| `PlayerTeamTest.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed, ran, and tested it |
| `DraftModelTest.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed, ran, and tested it |
| `DraftControllerTest.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed, ran, and tested it |

### GenAI note

We used Claude to generate a first version of the code from our Deliverable 2 specification and scope documents, and to draft this README. We gave it our spec (class design, method signatures, validation rules, and test plan) and asked it to follow the signatures closely and point out where it had to deviate. Every deviation is listed in the Reflection table above. We then ran the program, ran all JUnit tests, and reviewed each class. For the change feature, we described our classes to GenAI and asked which ones an undo feature would force us to edit; its list (`DraftModel`, `Team`, `DraftController`, `DraftView`) matched ours and did not require changing `TurnQueue`.
