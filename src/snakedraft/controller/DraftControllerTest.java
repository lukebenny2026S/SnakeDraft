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
    public void setUpRejectsNearDuplicateAndSymbolOnlyNames() {
        String typed = "2\n1\nNew York\nNewYork\n!!!\nBoston\n";
        DraftModel model = DraftController.setUp(viewWithInput(typed),
            players);
        assertNotNull(model);
        assertEquals("Boston", model.getTeams().get(1).getName());
        assertTrue(output.toString().contains("already named"));
        assertTrue(output.toString().contains("at least one letter"));
    }

    @Test
    public void setUpReturnsNullWhenInputEnds() {
        assertNull(DraftController.setUp(viewWithInput("3\n"), players));
    }

    // ---------- trade command ----------

    private DraftModel threeRoundModel() {
        List<Player> nine = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            nine.add(new Player("Player " + i, "RB"));
        }
        return new DraftModel(Arrays.asList(new Team("New York"),
            new Team("Bears"), new Team("Tigers")), nine, 3);
    }

    @Test
    public void tradeCommandSwapsPicksWithoutUsingTurn() {
        DraftModel model = threeRoundModel();
        DraftController c = new DraftController(model, viewWithInput(""));
        assertFalse(c.handlePick("trade New York 2 Tigers 3"));
        assertEquals("Tigers",
            model.getRoundOrder(2).get(2).getName());
        assertEquals("New York",
            model.getRoundOrder(3).get(2).getName());
        assertEquals(1, model.getPickNumber());
        assertEquals("New York", model.getTeamOnClock().getName());
        assertTrue(output.toString().contains("Trade: New York gives"));
        assertTrue(output.toString().contains("Round 2 order:"));
    }

    @Test
    public void tradeCommandIgnoresCaseAndSpacing() {
        DraftModel model = threeRoundModel();
        DraftController c = new DraftController(model, viewWithInput(""));
        c.handlePick("  TRADE   newyork  2   tigers 3 ");
        assertEquals("Tigers",
            model.getRoundOrder(2).get(2).getName());
    }

    @Test
    public void badTradeCommandsShowErrorsAndChangeNothing() {
        DraftModel model = threeRoundModel();
        DraftController c = new DraftController(model, viewWithInput(""));
        String[] bad = { "trade", "trade Bears 2", "trade Bears two Tigers 3",
            "trade Bears 2 Tigers three", "trade Ghosts 2 Tigers 3",
            "trade Bears 1 Tigers 2", "trade Bears 2 Bears 3",
            "trade Bears 99999999999 Tigers 3", "trade Bears 2 Tigers 9" };
        for (String input : bad) {
            assertFalse(input, c.handlePick(input));
        }
        assertEquals("Tigers", model.getRoundOrder(2).get(0).getName());
        assertEquals("New York", model.getRoundOrder(3).get(0).getName());
        assertEquals(1, model.getPickNumber());
        String text = output.toString();
        assertTrue(text.contains("Usage: trade"));
        assertTrue(text.contains("No team named"));
        assertTrue(text.contains("too large"));
        assertTrue(text.contains("cannot trade with itself"));
    }

    @Test
    public void runAcceptsTradeThenFinishesDraft() {
        DraftModel model = newModel();
        String typed = "trade A 2 B 2\nPlayer 1\nPlayer 2\nPlayer 3\n"
            + "Player 4\n";
        new DraftController(model, viewWithInput(typed)).run();
        assertTrue(model.isDraftOver());
        // Round 2 was B, A; after the trade it is A, B.
        assertEquals("Player 3",
            model.getTeams().get(0).getRoster().get(1).getName());
    }
}
