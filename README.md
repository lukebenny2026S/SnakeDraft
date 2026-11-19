# SnakeDraft

A text-based fantasy football snake draft for 2 to 12 teams, built in model / view / controller style. Turn order is managed by our own linked queue, `TurnQueue<T>`. Teams can trade future picks during the draft.

## Project layout

Each test file sits in the same package as the class it tests.

```
src/snakedraft/Main.java                          creates view, model, controller; starts the draft
src/snakedraft/model/Player.java                  immutable player (name, position)
src/snakedraft/model/Team.java                    team name + roster
src/snakedraft/model/TurnQueue.java               our own singly linked FIFO queue
src/snakedraft/model/DraftModel.java              all draft state and rules (snake order, picks, trades)
src/snakedraft/model/PlayerPool.java              fixed pool of 40 players
src/snakedraft/model/ModelDriver.java             tiny driver: runs a whole draft with no view
src/snakedraft/view/DraftView.java                prints the board and reads raw text
src/snakedraft/controller/DraftController.java    setup prompts, draft loop, pick and trade commands

src/snakedraft/model/TurnQueueTest.java           queue contract, including interleaved enqueue/dequeue
src/snakedraft/model/PlayerTeamTest.java          Player and Team
src/snakedraft/model/DraftModelTest.java          picks, snake order, trades, every bad-input case
src/snakedraft/controller/DraftControllerTest.java   setup, pick and trade commands with scripted input
src/snakedraft/view/DraftViewTest.java            exact view output
```

## How to compile and run

The project is an Eclipse Java project with JUnit 4 on the build path (`.classpath`). Eclipse compiles automatically on save.

**Full program:** right-click `src/snakedraft/Main.java` > Run As > Java Application, then type in the Console:

1. Number of teams, number of rounds, then a name for each team.
2. On each turn, type a player's name as listed. Case, spaces and punctuation are ignored, so `cj stroud` and `PatrickMahomes` work.
3. To trade, type `trade <team> <round> <team> <round>` on any turn, for example `trade New York 2 Tigers 3`. The first team gives its pick in the first round, and the second team gives its pick in the second round. Only picks in rounds after the current one can be traded. A trade does not use up the turn, and the new orders for the affected rounds are printed.
4. When every round is done, the final rosters print. To stop early, send end of input (Ctrl+D on Mac, Ctrl+Z then Enter on Windows).

**Tests:** right-click the `src` folder > Run As > JUnit Test. All 80 tests should pass. `TurnQueueTest`, `PlayerTeamTest` and `DraftModelTest` test the model with no view. `DraftControllerTest` and `DraftViewTest` feed scripted input and capture output, so no keyboard is needed.

**Model with no view:** right-click `src/snakedraft/model/ModelDriver.java` > Run As > Java Application.

**Command line (optional):** with `junit-4.13.2.jar` and `hamcrest-core-1.3.jar` in the project folder:

```
javac -cp junit-4.13.2.jar -d bin $(find src -name "*.java")
java -cp bin snakedraft.Main
java -cp bin:junit-4.13.2.jar:hamcrest-core-1.3.jar org.junit.runner.JUnitCore snakedraft.model.TurnQueueTest snakedraft.model.PlayerTeamTest snakedraft.model.DraftModelTest snakedraft.controller.DraftControllerTest snakedraft.view.DraftViewTest
```

## Reflection

### How did our design absorb the change?

Our change feature is trading picks between teams, as named in our Deliverable 3 Next Steps.

| Class | D3 prediction | What actually happened |
|---|---|---|
| `DraftModel` | Change: `roundOrders`, `refillQueue()` reads it, `tradePicks`, `findTeam`, `getRoundOrder` | Changed exactly as predicted, plus one private helper, `checkFutureRound(int)` |
| `DraftController` | Change: `handlePick` parses `trade` | Changed. `handlePick` hands trade lines to a new private `handleTrade`, with helpers `isWholeNumber`, `join`, `isTeam` and `canonicalName` |
| `DraftView` | Change: `showTradeResult`, `showRoundOrder` | Changed exactly as predicted, with the predicted signatures |
| `TurnQueue` | No change | No change |
| `Team`, `Player`, `PlayerPool` | No change | No change |
| `Main`, `ModelDriver` | No change | No change |

**What matched:** every class we said would change did, and every class we said would stay the same did. `TurnQueue` needed no edits because it never decided the order; it only serves whatever order it is filled with. Moving the order into `roundOrders` was the only structural change. Before, `refillQueue()` computed forward or reverse order on the fly. Now it reads the planned order for the round, and the snake is built once in the constructor.

**What surprised us:**
- **Parsing was the hardest part, not the model.** Team names can contain spaces (`New York`), so `trade New York 2 Tigers 3` cannot be split on a fixed position. The controller tries every way of splitting the words around a round number and keeps the split where both names are real teams. That meant the controller had to read `model.getTeams()` while parsing, which we had not predicted.
- **Trades can leave a team with two picks in one round** (and none in another). We had to decide what `tradePicks` does when that team trades again from that round: it trades its first pick in that round. A team with no pick in a round gets a clear error.
- **Trades should not use up the turn.** We had planned to parse trades inside `handlePick`, but its return value means "a pick was accepted." A trade now returns false so the same team stays on the clock and still makes its pick.

### Did the change cross the model/view line?

No. All trade rules (who owns which pick, which rounds can be traded, swapping slots) live in `DraftModel`. The view learned two new display methods. They receive only plain values (team name strings, round numbers and a read-only `List<Team>`), so the view still has no reference to the model and keeps no copy of model state. The controller is the only place the change rippled outside the model, which is expected because it is the layer that turns typed text into model calls. Nothing in the model knows the view exists, and `ModelDriver` still runs without it.

### What changed from our D3 plan

- **Bug fix from the D3 grading session:** `DraftModel.normalize` now removes everything except letters and digits and is public. The controller uses it for team names at setup. This fixed a crash where `New York` and `NewYork` passed setup but made the model constructor throw.
- **Planned before trading, done:** view tests (Jack's `DraftViewTest`) and the bad-input pass.
- **Planned but not done:** grouping the available players by position on the board. Trading took priority.
- **Signatures:** `tradePicks(String, int, String, int)`, `getRoundOrder(int)`, `showTradeResult(String, int, String, int)` and `showRoundOrder(int, List<Team>)` match the plan. The new private helpers are listed above.
- **Validation:** `tradePicks` rejects an unknown or blank team name, a team trading with itself, a round that is not in the future, a round past the end, a pick the team no longer owns, and any trade after the draft is over. Every rejection leaves the round orders unchanged. The controller rejects a missing part, a non-numeric round and a round number too large for an `int`.
- **Tests:** we added trade tests in the model, controller and view, plus two queue tests for interleaved enqueues and dequeues. Jack moved each test file into the same package as its class, so the `test` folder is gone. We removed an empty, fully commented-out `PlayerTest.java`, because `PlayerTeamTest` already covers `Player`. We also stopped tracking `bin/` and `.DS_Store` files in git.

### What doesn't work yet

- The board still prints all available players in one long list instead of grouping them by position.
- Picks in the current round cannot be traded. This was a design choice, so the queue never changes mid-round.
- When a team owns two picks in the same round, it can only trade the first one.
- If two different ways of splitting a trade command both name real teams (only possible with unusual names, like teams named `Team` and `Team 2`), the first split wins.
- Undo, real player data from a file, and position limits are not built.
- All 80 tests pass. There are no known failing tests.

### What design change would make the next feature cheaper

Our likely next feature is undo the last pick. It would be much cheaper if `DraftModel` kept a pick log (`List<PickRecord>` holding team, player and round) instead of only the queue and counters. With `roundOrders` and a log, the queue for any moment can be rebuilt from the round's planned order minus the picks already made in that round. Undo then becomes "remove the last log entry, put the player back in the pool, rebuild the queue," with no special case for undoing across a round boundary. The same log would also make it easy to show a full pick history on the board.

## Next Steps

### What is left for the final build (in order)

1. Group available players by position on the board. (Jack)
2. Add an `order` command that prints the planned order of every remaining round, so users can see the effect of trades before picking. (Ishaan)
3. Add a pick log to `DraftModel`, then use it for undo the last pick. (Luke)
4. Tests for the pick log and undo, normal and bad-input cases, then a final run of every scope bad-input case. (Shreyas)
5. Rehearse the 3-minute demo below, timed. (Everyone)

### Stretch goals

- **Committing to:** undo the last pick (built on the pick log).
- **Dropping:** auto-draft from a wish list, position limits, ordering by previous-season performance, and loading real data from a file.

### 3-minute demo

| Time | What we show |
|---|---|
| 0:00 to 0:30 | Run `Main`. Setup: type `five` for teams (rejected), then `3` teams and `3` rounds. Name teams `New York`, then `NewYork` (rejected as a duplicate), then `Bears` and `Tigers`. |
| 0:30 to 1:10 | Round 1: try `Mahomes` (unknown player), `42` (a number), and blank input, then draft `patrick mahomes`. Bears try `Patrick Mahomes` (already drafted), then draft `josh allen`. |
| 1:10 to 1:50 | With Tigers on the clock, trade: `trade New York 2 Tigers 3`. The printed orders show round 2 as Tigers, Bears, Tigers and round 3 as New York, Bears, New York. Then `trade Bears 1 Tigers 2` is rejected (round 1 is current) and `trade Bears 2 Bears 3` is rejected (same team). Tigers are still on the clock and draft `cj stroud`. |
| 1:50 to 2:30 | Finish rounds 2 and 3 quickly. The board's "On the clock" shows the traded order, and the final rosters print. |
| 2:30 to 3:00 | Run JUnit on `src` (green bar, 80 tests). Point to `tradePicks` in `DraftModel` and note that `TurnQueue` did not change. |

### Who does what

| Item | Owner |
|---|---|
| Board grouped by position | Jack |
| `order` command | Ishaan |
| Pick log and undo | Luke |
| Undo tests and final bad-input run | Shreyas |
| Demo rehearsal | Everyone |

## Attribution

| Class / test file | Who | How |
|---|---|---|
| `Main.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed and ran it |
| `Player.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed, ran and tested it |
| `Team.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed, ran and tested it |
| `TurnQueue.java` | Ishaan | Generated a first version with GenAI (Claude) from our spec, then reviewed, ran and tested it |
| `DraftModel.java` | Ishaan | Generated with GenAI (Claude) from our spec (D3), then the trade feature from our D3 Next Steps (D4); reviewed, ran and tested |
| `PlayerPool.java` | Ishaan | Generated a first version with GenAI (Claude), then reviewed it |
| `ModelDriver.java` | Ishaan | Generated a first version with GenAI (Claude), then reviewed and ran it |
| `DraftView.java` | Ishaan | Generated with GenAI (Claude) (D3), then the trade display methods (D4); reviewed and tested |
| `DraftController.java` | Ishaan | Generated with GenAI (Claude) (D3), then trade command parsing (D4); reviewed, ran and tested |
| `TurnQueueTest.java` | Ishaan | Generated with GenAI (Claude), including the D4 interleaved enqueue/dequeue tests; reviewed and ran |
| `PlayerTeamTest.java` | Ishaan | Generated with GenAI (Claude), reviewed and ran |
| `DraftModelTest.java` | Ishaan | Generated with GenAI (Claude), including the D4 trade tests; reviewed and ran |
| `DraftControllerTest.java` | Ishaan | Generated with GenAI (Claude), including the D4 trade command tests; reviewed and ran |
| `DraftViewTest.java` | Jack (Ishaan added two D4 tests) | Jack wrote the view tests and moved every test file into its class's package. Ishaan added the `showTradeResult` and `showRoundOrder` tests with GenAI (Claude). |

### GenAI note

We used Claude to generate a first version of the code from our Deliverable 2 specification (D3), and to implement the trade feature from our Deliverable 3 Next Steps (D4). For D4, we gave it our existing classes and the D3 prediction (`DraftModel`, `DraftController` and `DraftView` change; `TurnQueue`, `Team`, `Player`, `PlayerPool`, `Main` and `ModelDriver` do not). We asked it to stay inside that prediction and to tell us if anything outside it had to change. Nothing did. We also asked it for candidate queue tests (dequeue on empty, peek on empty, order after interleaved enqueues). We kept only the tests that check the queue's contract rather than a detail of the current view. Claude also drafted this README. We ran the program and all 80 JUnit tests, and reviewed each change before committing.
