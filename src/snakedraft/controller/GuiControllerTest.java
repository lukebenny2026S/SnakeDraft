package snakedraft.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import snakedraft.model.DraftModel;
import snakedraft.model.Player;
import snakedraft.model.PlayerPool;
import snakedraft.model.Team;
import snakedraft.view.BoardState;

/** Tests the view-free part of the GUI controller: the turn display data. */
public class GuiControllerTest {

    private Team a;
    private Team b;
    private Team c;
    private DraftModel model;
    private List<Player> pool;

    @Before
    public void setUp() {
        a = new Team("A");
        b = new Team("B");
        c = new Team("C");
        pool = PlayerPool.defaultPlayers();
        List<Team> teams = new ArrayList<>();
        teams.add(a);
        teams.add(b);
        teams.add(c);
        model = new DraftModel(teams, pool, 3);
    }

    @Test
    public void oddRoundIsForwardEvenRoundIsReversed() {
        List<Team> teams = model.getTeams();
        assertEquals("[A, B, C]", GuiController.turnOrder(teams, 1).toString());
        assertEquals("[C, B, A]", GuiController.turnOrder(teams, 2).toString());
        assertEquals("[A, B, C]", GuiController.turnOrder(teams, 3).toString());
    }

    @Test
    public void clockIndexFollowsEveryPick() {
        // Expected team on the clock for 9 picks: A B C C B A A B C
        Team[] expected = { a, b, c, c, b, a, a, b, c };
        for (int i = 0; i < expected.length; i++) {
            BoardState s = GuiController.boardState(model, null);
            assertSame("pick " + (i + 1), expected[i], s.onClock());
            assertEquals(i + 1, s.pick);
            assertEquals(i / 3 + 1, s.round);
            assertEquals((i / 3) % 2 == 1, s.reversed);
            assertFalse(s.over);
            model.makePick(model.getAvailablePlayers().get(0).getName());
        }
        BoardState end = GuiController.boardState(model, null);
        assertTrue(end.over);
        assertEquals(end.turnOrder.size(), end.clockIndex);
    }

    @Test
    public void availableListShrinksAsPlayersAreDrafted() {
        int before = GuiController.boardState(model, null).available.size();
        model.makePick(pool.get(0).getName());
        assertEquals(before - 1, GuiController.boardState(model, a).available.size());
    }
}
