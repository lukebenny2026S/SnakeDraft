package snakedraft.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import snakedraft.model.DraftModel;
import snakedraft.model.Player;
import snakedraft.model.Team;
import snakedraft.view.DraftView;

public class DraftControllerTest {

    private ByteArrayOutputStream output;
    private List<Player> players;

    @Before
    public void setUp() {
        output = new ByteArrayOutputStream();
        players = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            players.add(new Player("Player " + i, "RB"));
        }
    }

    private DraftView viewWithInput(String typed) {
        return new DraftView(new ByteArrayInputStream(typed.getBytes()),
            new PrintStream(output));
    }

    private DraftModel newModel() {
        return new DraftModel(Arrays.asList(new Team("A"), new Team("B")),
            players, 2);
    }

    @Test
    public void constructorRejectsNull() {
        DraftView view = viewWithInput("");
        assertThrows(IllegalArgumentException.class,
            () -> new DraftController(null, view));
        assertThrows(IllegalArgumentException.class,
            () -> new DraftController(newModel(), null));
    }

    @Test
    public void handlePickAcceptsValidName() {
        DraftModel model = newModel();
        DraftController c = new DraftController(model, viewWithInput(""));
        assertTrue(c.handlePick("  player 1  "));
        assertEquals(2, model.getPickNumber());
        assertTrue(output.toString().contains("A drafts Player 1"));
    }

    @Test
    public void handlePickRejectsBlankNumberUnknownAndRepeat() {
        DraftModel model = newModel();
        DraftController c = new DraftController(model, viewWithInput(""));
        assertFalse(c.handlePick(""));
        assertFalse(c.handlePick("   "));
        assertFalse(c.handlePick(null));
        assertFalse(c.handlePick("42"));
        assertFalse(c.handlePick("4.5"));
        assertFalse(c.handlePick("Nobody"));
        assertEquals(1, model.getPickNumber());
        assertTrue(c.handlePick("Player 1"));
        assertFalse(c.handlePick("Player 1"));
        assertEquals(2, model.getPickNumber());
        assertEquals("B", model.getTeamOnClock().getName());
    }

    @Test
    public void runCompletesDraftWithBadInputMixedIn() {
        DraftModel model = newModel();
        String typed = "\nPlayer 1\nPlayer 1\n7\nplayer 2\nPLAYER 3\n"
            + "player4\n";
        new DraftController(model, viewWithInput(typed)).run();
        assertTrue(model.isDraftOver());
        assertTrue(output.toString().contains("FINAL ROSTERS"));
        // A picks 1, B picks 2, B picks 3, A picks 4 (snake)
        assertEquals("Player 4",
            model.getTeams().get(0).getRoster().get(1).getName());
        assertEquals("Player 3",
            model.getTeams().get(1).getRoster().get(1).getName());
    }

    @Test
    public void runExitsCleanlyWhenInputEnds() {
        DraftModel model = newModel();
        new DraftController(model, viewWithInput("Player 1\n")).run();
        assertFalse(model.isDraftOver());
        assertTrue(output.toString().contains("Input ended"));
    }

    @Test
    public void setUpHandlesBadNumbersAndDuplicateNames() {
        String typed = "five\n4.5\n999999999999\n1\n2\n0\nabc\n2\n"
            + "Tigers\n\ntigers\nBears\n";
        DraftModel model = DraftController.setUp(viewWithInput(typed),
            players);
        assertNotNull(model);
        assertEquals(2, model.getTeams().size());
        assertEquals(2, model.getTotalRounds());
        assertEquals("Bears", model.getTeams().get(1).getName());
    }

    @Test
    public void setUpReturnsNullWhenInputEnds() {
        assertNull(DraftController.setUp(viewWithInput("3\n"), players));
    }
}
