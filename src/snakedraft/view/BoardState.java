package snakedraft.view;

import java.util.List;
import snakedraft.model.Player;
import snakedraft.model.Team;

/**
 * Plain values the GUI needs to draw one frame of the draft. The controller
 * builds one after every change; the view never sees the DraftModel.
 */
public final class BoardState {

    /** Teams in pick order for the current round (already snaked). */
    public final List<Team> turnOrder;
    /** Index into turnOrder of the team on the clock; turnOrder.size() if the round/draft is done. */
    public final int clockIndex;
    public final int round;
    public final int totalRounds;
    public final int pick;
    /** True when the current round runs in reverse (even rounds). */
    public final boolean reversed;
    public final boolean over;
    public final List<Player> available;
    /** All teams in round-1 order (for the roster panel). */
    public final List<Team> teams;
    /** Team that just made a pick, or null. */
    public final Team lastPicker;

    public BoardState(List<Team> turnOrder, int clockIndex, int round,
        int totalRounds, int pick, boolean reversed, boolean over,
        List<Player> available, List<Team> teams, Team lastPicker) {
        this.turnOrder = turnOrder;
        this.clockIndex = clockIndex;
        this.round = round;
        this.totalRounds = totalRounds;
        this.pick = pick;
        this.reversed = reversed;
        this.over = over;
        this.available = available;
        this.teams = teams;
        this.lastPicker = lastPicker;
    }

    /** @return the team on the clock, or null if the draft is over */
    public Team onClock() {
        return over || clockIndex >= turnOrder.size() ? null
            : turnOrder.get(clockIndex);
    }
}
