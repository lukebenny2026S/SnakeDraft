package snakedraft.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import snakedraft.model.Player;
import snakedraft.model.Team;

/**
 * Tests for {@link DraftView}.
 *
 * NOTE: Player and Team were not provided, so the helper methods at the
 * bottom (player, team) assume these APIs:
 *   new Player(String name, String position)
 *   new Team(String name) and team.addPlayer(Player)
 * Adjust only those helpers if your model differs.
 */
public class DraftViewTest {

    private static final String NL = System.lineSeparator();

    private ByteArrayOutputStream bytes;
    private PrintStream out;

    @Before
    public void setUp() {
        bytes = new ByteArrayOutputStream();
        out = new PrintStream(bytes, true);
    }

    // ---------- helpers ----------

    private DraftView viewWithInput(String input) {
        return new DraftView(
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), out);
    }

    private DraftView viewWithNoInput() {
        return viewWithInput("");
    }

    private String output() {
        out.flush();
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    private static Player player(String name, String position) {
        return new Player(name, position);
    }

    private static Team team(String name, Player... players) {
        Team team = new Team(name);
        for (Player p : players) {
            team.addPlayer(p);
        }
        return team;
    }

    // ---------- readLine ----------

    @Test
    public void readLine_printsPromptWithoutNewline() {
        DraftView view = viewWithInput("hello" + NL);
        view.readLine("Pick> ");
        assertEquals("Pick> ", output());
    }

    @Test
    public void readLine_returnsTypedLine() {
        DraftView view = viewWithInput("Jane Doe" + NL);
        assertEquals("Jane Doe", view.readLine("> "));
    }

    @Test
    public void readLine_returnsLineWithoutTerminator() {
        DraftView view = viewWithInput("abc\n");
        assertEquals("abc", view.readLine("> "));
    }

    @Test
    public void readLine_lastLineWithoutNewlineIsReturned() {
        DraftView view = viewWithInput("no newline");
        assertEquals("no newline", view.readLine("> "));
    }

    @Test
    public void readLine_emptyLineReturnsEmptyString() {
        DraftView view = viewWithInput("\n");
        assertEquals("", view.readLine("> "));
    }

    @Test
    public void readLine_preservesWhitespaceAndCase() {
        DraftView view = viewWithInput("  MiXeD Case  \n");
        assertEquals("  MiXeD Case  ", view.readLine("> "));
    }

    @Test
    public void readLine_readsSuccessiveLinesInOrder() {
        DraftView view = viewWithInput("one\ntwo\nthree\n");
        assertEquals("one", view.readLine("> "));
        assertEquals("two", view.readLine("> "));
        assertEquals("three", view.readLine("> "));
    }

    @Test
    public void readLine_returnsNullWhenInputEnded() {
        DraftView view = viewWithNoInput();
        assertNull(view.readLine("> "));
    }

    @Test
    public void readLine_returnsNullAfterLastLineConsumed() {
        DraftView view = viewWithInput("only\n");
        assertEquals("only", view.readLine("> "));
        assertNull(view.readLine("> "));
    }

    @Test
    public void readLine_stillPrintsPromptWhenInputEnded() {
        DraftView view = viewWithNoInput();
        view.readLine("Pick> ");
        assertEquals("Pick> ", output());
    }

    // ---------- showMessage / showError ----------

    @Test
    public void showMessage_printsMessageWithNewline() {
        viewWithNoInput().showMessage("Welcome");
        assertEquals("Welcome" + NL, output());
    }

    @Test
    public void showMessage_emptyStringPrintsBlankLine() {
        viewWithNoInput().showMessage("");
        assertEquals(NL, output());
    }

    @Test
    public void showError_prefixesMessage() {
        viewWithNoInput().showError("Player not found");
        assertEquals("  ! Player not found" + NL, output());
    }

    // ---------- showBoard ----------

    @Test
    public void showBoard_printsHeaderLine() {
        Team onClock = team("Vipers");
        viewWithNoInput().showBoard(onClock, 2, 5, 7,
            new ArrayList<Player>());
        assertTrue(output().contains(
            "Round 2 of 5  |  Pick 7  |  On the clock: Vipers" + NL));
    }

    @Test
    public void showBoard_listsAllAvailablePlayersInOrder() {
        List<Player> pool = Arrays.asList(
            player("Alice Smith", "QB"),
            player("Bob Jones", "RB"),
            player("Cara Lee", "WR"));
        viewWithNoInput().showBoard(team("Cobras"), 1, 3, 1, pool);

        String text = output();
        assertTrue(text.contains("Available players:" + NL));
        int a = text.indexOf("Alice Smith");
        int b = text.indexOf("Bob Jones");
        int c = text.indexOf("Cara Lee");
        assertTrue(a >= 0);
        assertTrue(b > a);
        assertTrue(c > b);
        assertTrue(text.contains("QB"));
        assertTrue(text.contains("RB"));
        assertTrue(text.contains("WR"));
    }

    @Test
    public void showBoard_formatsPlayerRowWithPaddedColumns() {
        List<Player> pool = Arrays.asList(player("Alice", "QB"));
        viewWithNoInput().showBoard(team("T"), 1, 1, 1, pool);
        String expectedRow = String.format("  %-24s %-4s%n", "Alice", "QB");
        assertTrue(output().contains(expectedRow));
    }

    @Test
    public void showBoard_emptyPoolPrintsNoPlayerRows() {
        viewWithNoInput().showBoard(team("T"), 1, 1, 1,
            new ArrayList<Player>());
        String bar = "==================================================" + NL;
        String expected = NL
            + bar
            + "Round 1 of 1  |  Pick 1  |  On the clock: T" + NL
            + "--------------------------------------------------" + NL
            + "Available players:" + NL
            + bar;
        assertEquals(expected, output());
    }

    @Test
    public void showBoard_startsWithBlankLineAndEndsWithSeparator() {
        viewWithNoInput().showBoard(team("T"), 1, 1, 1,
            Arrays.asList(player("Zed", "K")));
        String text = output();
        assertTrue(text.startsWith(NL + "=="));
        assertTrue(text.endsWith(
            "==================================================" + NL));
    }

    // ---------- showPickResult ----------

    @Test
    public void showPickResult_printsTeamPlayerAndPosition() {
        viewWithNoInput().showPickResult(team("Hawks"),
            player("Dana Ray", "TE"));
        assertEquals("  > Hawks drafts Dana Ray (TE)" + NL, output());
    }

    // ---------- showFinalRosters ----------

    @Test
    public void showFinalRosters_printsBannerAndTeamNames() {
        List<Team> teams = Arrays.asList(team("Hawks"), team("Owls"));
        viewWithNoInput().showFinalRosters(teams);

        String text = output();
        assertTrue(text.contains(
            "================ FINAL ROSTERS ================" + NL));
        assertTrue(text.contains("Hawks:" + NL));
        assertTrue(text.contains("Owls:" + NL));
    }

    @Test
    public void showFinalRosters_numbersPlayersFromOne() {
        Team hawks = team("Hawks",
            player("Alice", "QB"),
            player("Bob", "RB"));
        viewWithNoInput().showFinalRosters(Arrays.asList(hawks));

        String text = output();
        assertTrue(text.contains("  1. Alice (QB)" + NL));
        assertTrue(text.contains("  2. Bob (RB)" + NL));
        assertTrue(text.indexOf("1. Alice") < text.indexOf("2. Bob"));
    }

    @Test
    public void showFinalRosters_emptyRosterShowsNoPlayersNotice() {
        viewWithNoInput().showFinalRosters(Arrays.asList(team("Empty")));
        String text = output();
        assertTrue(text.contains("Empty:" + NL));
        assertTrue(text.contains("  (no players)" + NL));
    }

    @Test
    public void showFinalRosters_nonEmptyRosterOmitsNoPlayersNotice() {
        Team hawks = team("Hawks", player("Alice", "QB"));
        viewWithNoInput().showFinalRosters(Arrays.asList(hawks));
        assertTrue(!output().contains("(no players)"));
    }

    @Test
    public void showFinalRosters_keepsTeamOrderAndRestartsNumbering() {
        Team first = team("First", player("P1", "QB"));
        Team second = team("Second", player("P2", "RB"));
        viewWithNoInput().showFinalRosters(Arrays.asList(first, second));

        String text = output();
        assertTrue(text.indexOf("First:") < text.indexOf("Second:"));
        assertTrue(text.contains("  1. P1 (QB)" + NL));
        assertTrue(text.contains("  1. P2 (RB)" + NL));
    }

    @Test
    public void showFinalRosters_noTeamsPrintsOnlyBanner() {
        viewWithNoInput().showFinalRosters(new ArrayList<Team>());
        assertEquals(NL
            + "================ FINAL ROSTERS ================" + NL,
            output());
    }
}