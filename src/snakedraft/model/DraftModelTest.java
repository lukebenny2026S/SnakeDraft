package snakedraft.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class DraftModelTest {

    private Team a;
    private Team b;
    private Team c;
    private List<Team> teams;
    private List<Player> players;
    private DraftModel model;

    @Before
    public void setUp() {
        a = new Team("A");
        b = new Team("B");
        c = new Team("C");
        teams = Arrays.asList(a, b, c);
        players = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            players.add(new Player("Player " + i, "WR"));
        }
        model = new DraftModel(teams, players, 2);
    }

    @Test
    public void startsAtRoundOnePickOne() {
        assertEquals(1, model.getCurrentRound());
        assertEquals(1, model.getPickNumber());
        assertEquals(a, model.getTeamOnClock());
        assertFalse(model.isDraftOver());
        assertEquals(6, model.getAvailablePlayers().size());
    }

    @Test
    public void snakeOrderIsABCCBA() {
        Team[] expected = { a, b, c, c, b, a };
        for (int i = 0; i < expected.length; i++) {
            assertEquals("pick " + (i + 1), expected[i],
                model.getTeamOnClock());
            model.makePick("Player " + (i + 1));
        }
        assertTrue(model.isDraftOver());
    }

    @Test
    public void makePickMovesPlayerFromPoolToRoster() {
        Player p = model.makePick("Player 1");
        assertEquals("Player 1", p.getName());
        assertEquals(1, a.getRoster().size());
        assertFalse(model.isAvailable("Player 1"));
        assertEquals(5, model.getAvailablePlayers().size());
        assertEquals(2, model.getPickNumber());
        assertEquals(b, model.getTeamOnClock());
    }

    @Test
    public void roundAdvancesAfterLastTeam() {
        model.makePick("Player 1");
        model.makePick("Player 2");
        model.makePick("Player 3");
        assertEquals(2, model.getCurrentRound());
        assertEquals(c, model.getTeamOnClock());
    }

    @Test
    public void nameMatchingIgnoresCaseAndSpaces() {
        assertTrue(model.isAvailable("  player   1 "));
        assertTrue(model.isAvailable("PLAYER1"));
        assertEquals("Player 1", model.makePick("pLaYeR  1").getName());
        assertTrue(model.isAvailable("player-2."));
    }

    @Test
    public void unknownPlayerThrowsAndChangesNothing() {
        assertThrows(IllegalArgumentException.class,
            () -> model.makePick("Unknown Player"));
        assertEquals(1, model.getPickNumber());
        assertEquals(a, model.getTeamOnClock());
        assertTrue(a.getRoster().isEmpty());
    }

    @Test
    public void alreadyDraftedThrowsAndChangesNothing() {
        model.makePick("Player 1");
        assertThrows(IllegalArgumentException.class,
            () -> model.makePick("player 1"));
        assertEquals(2, model.getPickNumber());
        assertEquals(b, model.getTeamOnClock());
        assertTrue(b.getRoster().isEmpty());
    }

    @Test
    public void nullOrBlankPickThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> model.makePick(null));
        assertThrows(IllegalArgumentException.class,
            () -> model.makePick("   "));
        assertFalse(model.isAvailable(null));
        assertFalse(model.isAvailable(""));
    }

    @Test
    public void pickAfterDraftOverThrows() {
        for (int i = 1; i <= 6; i++) {
            model.makePick("Player " + i);
        }
        assertTrue(model.isDraftOver());
        assertThrows(IllegalStateException.class,
            () -> model.makePick("Player 1"));
        assertThrows(IllegalStateException.class,
            () -> model.getTeamOnClock());
    }

    @Test
    public void constructorRejectsBadSetup() {
        assertThrows(IllegalArgumentException.class,
            () -> new DraftModel(null, players, 2));
        assertThrows(IllegalArgumentException.class,
            () -> new DraftModel(teams, null, 2));
        assertThrows(IllegalArgumentException.class,
            () -> new DraftModel(Arrays.asList(a), players, 2));
        assertThrows(IllegalArgumentException.class,
            () -> new DraftModel(teams, players, 0));
        // 3 teams * 3 rounds = 9 > 6 players
        assertThrows(IllegalArgumentException.class,
            () -> new DraftModel(teams, players, 3));
    }

    @Test
    public void constructorRejectsDuplicates() {
        List<Team> dupTeams = Arrays.asList(new Team("A"), new Team("a"));
        assertThrows(IllegalArgumentException.class,
            () -> new DraftModel(dupTeams, players, 1));
        List<Player> dupPlayers = new ArrayList<>(players);
        dupPlayers.add(new Player("player 1", "QB"));
        assertThrows(IllegalArgumentException.class,
            () -> new DraftModel(teams, dupPlayers, 1));
    }

    @Test
    public void readOnlyLists() {
        assertThrows(UnsupportedOperationException.class,
            () -> model.getTeams().clear());
        assertThrows(UnsupportedOperationException.class,
            () -> model.getAvailablePlayers().clear());
        assertEquals(Arrays.asList(a, b, c), model.getTeams());
    }

    @Test
    public void defaultPoolHasFortyPlayers() {
        assertEquals(40, PlayerPool.defaultPlayers().size());
    }

    // ---------- trading picks (Deliverable 4 change feature) ----------

    /** 3 teams, 3 rounds, 9 players: orders A B C / C B A / A B C. */
    private DraftModel threeRoundModel() {
        List<Player> nine = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            nine.add(new Player("Player " + i, "WR"));
        }
        return new DraftModel(teams, nine, 3);
    }

    /** Makes picks until the draft ends and returns who picked, in order. */
    private List<Team> draftEverything(DraftModel m) {
        List<Team> served = new ArrayList<>();
        int next = 1;
        while (!m.isDraftOver()) {
            served.add(m.getTeamOnClock());
            while (!m.isAvailable("Player " + next)) {
                next++;
            }
            m.makePick("Player " + next);
        }
        return served;
    }

    @Test
    public void roundOrdersStartAsSnake() {
        DraftModel m = threeRoundModel();
        assertEquals(Arrays.asList(a, b, c), m.getRoundOrder(1));
        assertEquals(Arrays.asList(c, b, a), m.getRoundOrder(2));
        assertEquals(Arrays.asList(a, b, c), m.getRoundOrder(3));
    }

    @Test
    public void noTradesKeepsSnakeOrder() {
        assertEquals(Arrays.asList(a, b, c, c, b, a, a, b, c),
            draftEverything(threeRoundModel()));
    }

    @Test
    public void tradeSwapsPicksAcrossRounds() {
        DraftModel m = threeRoundModel();
        m.tradePicks("A", 2, "C", 3);
        assertEquals(Arrays.asList(c, b, c), m.getRoundOrder(2));
        assertEquals(Arrays.asList(a, b, a), m.getRoundOrder(3));
        assertEquals(Arrays.asList(a, b, c, c, b, c, a, b, a),
            draftEverything(m));
        // A gave up round 2 but owns two round 3 picks; C the reverse.
        assertEquals(3, a.getRoster().size());
        assertEquals(3, c.getRoster().size());
    }

    @Test
    public void tradeInSameRoundSwapsOrder() {
        DraftModel m = threeRoundModel();
        m.tradePicks("a", 2, "  C ", 2);
        assertEquals(Arrays.asList(a, b, c), m.getRoundOrder(2));
        assertEquals(Arrays.asList(a, b, c), m.getRoundOrder(3));
    }

    @Test
    public void tradeDoesNotChangeCurrentRoundQueue() {
        DraftModel m = threeRoundModel();
        m.makePick("Player 1");
        m.tradePicks("B", 3, "C", 2);
        assertEquals(b, m.getTeamOnClock());
        m.makePick("Player 2");
        assertEquals(c, m.getTeamOnClock());
    }

    @Test
    public void tradeCurrentOrPastRoundRejected() {
        DraftModel m = threeRoundModel();
        assertThrows(IllegalArgumentException.class,
            () -> m.tradePicks("A", 1, "B", 2));
        m.makePick("Player 1");
        m.makePick("Player 2");
        m.makePick("Player 3");
        assertThrows(IllegalArgumentException.class,
            () -> m.tradePicks("A", 1, "B", 3));
        assertThrows(IllegalArgumentException.class,
            () -> m.tradePicks("A", 2, "B", 3));
        assertEquals(Arrays.asList(c, b, a), m.getRoundOrder(2));
        assertEquals(Arrays.asList(a, b, c), m.getRoundOrder(3));
    }

    @Test
    public void tradeRoundOutOfRangeRejected() {
        DraftModel m = threeRoundModel();
        assertThrows(IllegalArgumentException.class,
            () -> m.tradePicks("A", 0, "B", 2));
        assertThrows(IllegalArgumentException.class,
            () -> m.tradePicks("A", 2, "B", 4));
        assertThrows(IllegalArgumentException.class,
            () -> m.tradePicks("A", -5, "B", 2));
    }

    @Test
    public void tradeBadTeamNamesRejected() {
        DraftModel m = threeRoundModel();
        assertThrows(IllegalArgumentException.class,
            () -> m.tradePicks("Nobody", 2, "B", 3));
        assertThrows(IllegalArgumentException.class,
            () -> m.tradePicks(null, 2, "B", 3));
        assertThrows(IllegalArgumentException.class,
            () -> m.tradePicks("A", 2, "  ", 3));
        assertThrows(IllegalArgumentException.class,
            () -> m.tradePicks("A", 2, "a", 3));
        assertEquals(Arrays.asList(c, b, a), m.getRoundOrder(2));
    }

    @Test
    public void tradePickTeamNoLongerOwnsRejected() {
        DraftModel m = threeRoundModel();
        m.tradePicks("A", 2, "C", 3);
        assertThrows(IllegalArgumentException.class,
            () -> m.tradePicks("A", 2, "B", 3));
        assertEquals(Arrays.asList(c, b, c), m.getRoundOrder(2));
        assertEquals(Arrays.asList(a, b, a), m.getRoundOrder(3));
    }

    @Test
    public void tradeLastRoundOrAfterDraftRejected() {
        draftEverything(model);
        assertThrows(IllegalStateException.class,
            () -> model.tradePicks("A", 2, "B", 2));
        DraftModel m = threeRoundModel();
        for (int i = 1; i <= 6; i++) {
            m.makePick("Player " + i);
        }
        assertThrows(IllegalArgumentException.class,
            () -> m.tradePicks("A", 3, "B", 3));
    }

    @Test
    public void getRoundOrderBadRoundAndReadOnly() {
        DraftModel m = threeRoundModel();
        assertThrows(IllegalArgumentException.class, () -> m.getRoundOrder(0));
        assertThrows(IllegalArgumentException.class, () -> m.getRoundOrder(4));
        assertThrows(UnsupportedOperationException.class,
            () -> m.getRoundOrder(2).clear());
    }

    // ---------- position limits ----------

    /** 2 teams, 3 rounds; QB limit 1. Pool: 3 QBs, 3 RBs. */
    private DraftModel limitedModel() {
        List<Player> pool = Arrays.asList(new Player("QB One", "QB"),
            new Player("QB Two", "QB"), new Player("QB Three", "QB"),
            new Player("RB One", "RB"), new Player("RB Two", "RB"),
            new Player("RB Three", "RB"));
        Map<String, Integer> limits = new HashMap<>();
        limits.put("qb", 1);
        return new DraftModel(Arrays.asList(a, b), pool, 3, limits);
    }

    @Test
    public void pickUnderLimitIsAccepted() {
        DraftModel m = limitedModel();
        m.makePick("QB One");
        assertEquals(1, m.getPositionCount(a, "QB"));
        assertEquals(1, m.getPositionCount(a, "qb"));
        assertEquals(0, m.getPositionCount(a, "RB"));
    }

    @Test
    public void pickOverLimitRejectedAndChangesNothing() {
        DraftModel m = limitedModel();
        m.makePick("QB One");   // A, round 1
        m.makePick("RB One");   // B, round 1
        m.makePick("QB Two");   // B, round 2 (B's first QB)
        // A is on the clock and already has a QB
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class, () -> m.makePick("QB Three"));
        assertTrue(e.getMessage().contains("already has 1 QB"));
        assertEquals(a, m.getTeamOnClock());
        assertEquals(4, m.getPickNumber());
        assertTrue(m.isAvailable("QB Three"));
        m.makePick("RB Two");
        assertEquals(2, a.getRoster().size());
    }

    @Test
    public void limitWaivedWhenNoLegalPickLeft() {
        List<Player> pool = Arrays.asList(new Player("QB One", "QB"),
            new Player("QB Two", "QB"), new Player("QB Three", "QB"),
            new Player("QB Four", "QB"));
        Map<String, Integer> limits = new HashMap<>();
        limits.put("QB", 1);
        DraftModel m = new DraftModel(Arrays.asList(a, b), pool, 2, limits);
        m.makePick("QB One");
        m.makePick("QB Two");
        m.makePick("QB Three"); // only QBs left, so the limit is waived
        m.makePick("QB Four");
        assertTrue(m.isDraftOver());
        assertEquals(2, a.getRoster().size());
    }

    @Test
    public void noLimitsByDefault() {
        assertTrue(model.getPositionLimits().isEmpty());
        for (int i = 1; i <= 6; i++) {
            model.makePick("Player " + i); // all WR
        }
        assertTrue(model.isDraftOver());
    }

    @Test
    public void defaultLimitsAreTheProgramRules() {
        Map<String, Integer> d = DraftModel.defaultPositionLimits();
        assertEquals(Integer.valueOf(2), d.get("QB"));
        assertEquals(Integer.valueOf(4), d.get("RB"));
        assertEquals(Integer.valueOf(4), d.get("WR"));
        assertEquals(Integer.valueOf(2), d.get("TE"));
        assertEquals(Integer.valueOf(1), d.get("K"));
        assertEquals(Integer.valueOf(1), d.get("DEF"));
    }

    @Test
    public void badLimitsRejected() {
        Map<String, Integer> zero = new HashMap<>();
        zero.put("QB", 0);
        assertThrows(IllegalArgumentException.class,
            () -> new DraftModel(teams, players, 2, zero));
        Map<String, Integer> blank = new HashMap<>();
        blank.put(" ", 2);
        assertThrows(IllegalArgumentException.class,
            () -> new DraftModel(teams, players, 2, blank));
        Map<String, Integer> nullValue = new HashMap<>();
        nullValue.put("QB", null);
        assertThrows(IllegalArgumentException.class,
            () -> new DraftModel(teams, players, 2, nullValue));
        assertThrows(IllegalArgumentException.class,
            () -> new DraftModel(teams, players, 2, null));
    }

    @Test
    public void positionLimitsAreReadOnlyAndUppercased() {
        DraftModel m = limitedModel();
        assertEquals(Integer.valueOf(1), m.getPositionLimits().get("QB"));
        assertThrows(UnsupportedOperationException.class,
            () -> m.getPositionLimits().put("RB", 1));
    }

    // ---------- tradeable rounds ----------

    @Test
    public void tradeableRoundsListsFuturePicksOwned() {
        DraftModel m = threeRoundModel();
        assertEquals(Arrays.asList(2, 3), m.getTradeableRounds("a"));
        m.tradePicks("A", 2, "C", 3);
        assertEquals(Arrays.asList(3), m.getTradeableRounds("A"));
        assertEquals(Arrays.asList(2), m.getTradeableRounds("C"));
    }

    @Test
    public void tradeableRoundsEmptyInLastRoundOrAfterDraft() {
        DraftModel m = threeRoundModel();
        for (int i = 1; i <= 6; i++) {
            m.makePick("Player " + i);
        }
        assertTrue(m.getTradeableRounds("A").isEmpty());
        draftEverything(m);
        assertTrue(m.getTradeableRounds("A").isEmpty());
    }

    @Test
    public void tradeableRoundsBadTeamRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> model.getTradeableRounds("Nobody"));
        assertThrows(IllegalArgumentException.class,
            () -> model.getTradeableRounds(null));
        assertThrows(IllegalArgumentException.class,
            () -> model.getTradeableRounds(""));
    }

    // ---------- additional edge cases & unit tests ----------

    @Test
    public void testTeamEqualsAndHashCode() {
        Team t1 = new Team("Alpha");
        Team t2 = new Team("Alpha");
        Team t3 = new Team("Beta");
        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertFalse(t1.equals(t3));
        assertFalse(t1.equals(null));
        assertFalse(t1.equals("Alpha"));
        assertEquals("Alpha", t1.getName());
    }

    @Test
    public void testPlayerEqualsAndHashCode() {
        Player p1 = new Player("Tom Brady", "QB");
        Player p2 = new Player("Tom Brady", "QB");
        Player p3 = new Player("Patrick Mahomes", "QB");
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertFalse(p1.equals(p3));
        assertFalse(p1.equals(null));
        assertFalse(p1.equals("Tom Brady"));
        assertEquals("Tom Brady", p1.getName());
        assertEquals("QB", p1.getPosition());
    }

    @Test
    public void selfTradeRejected() {
        DraftModel m = threeRoundModel();
        // A team cannot trade a pick with itself
        assertThrows(IllegalArgumentException.class,
            () -> m.tradePicks("A", 2, "A", 3));
        assertThrows(IllegalArgumentException.class,
            () -> m.tradePicks("A", 2, "  a  ", 2));
    }

    @Test
    public void positionCountForInvalidOrMissingInputs() {
        DraftModel m = limitedModel();
        // Requesting count for positions not drafted or empty
        assertEquals(0, m.getPositionCount(a, "KICKER"));
        assertEquals(0, m.getPositionCount(a, ""));

        // Null checks for team or position string
        assertThrows(IllegalArgumentException.class,
            () -> m.getPositionCount(null, "QB"));
        assertThrows(IllegalArgumentException.class,
            () -> m.getPositionCount(a, null));
        assertThrows(IllegalArgumentException.class,
            () -> m.getPositionCount(null, null));
    }

    @Test
    public void teamRosterIsReadOnly() {
        model.makePick("Player 1");
        // Attempting to modify the roster directly from outside should throw an exception
        assertThrows(UnsupportedOperationException.class,
            () -> a.getRoster().clear());
    }

    @Test
    public void availablePlayersListIsUnmodifiable() {
        List<Player> available = model.getAvailablePlayers();
        assertThrows(UnsupportedOperationException.class,
            () -> available.remove(0));
    }
}