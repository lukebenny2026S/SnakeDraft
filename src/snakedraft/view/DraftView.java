package snakedraft.view;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import snakedraft.model.Player;
import snakedraft.model.Team;

/**
 * Text view. Prints what it is handed and returns what the user types.
 * It has no reference to DraftModel or TurnQueue and holds no draft state.
 */
public class DraftView {

    private final Scanner in;
    private final PrintStream out;

    /** Creates a view on the console. */
    public DraftView() {
        this(System.in, System.out);
    }

    /**
     * Creates a view on the given streams (used by tests).
     *
     * @param input  where typed text comes from
     * @param output where text is printed
     */
    public DraftView(InputStream input, PrintStream output) {
        this.in = new Scanner(input);
        this.out = output;
    }

    /**
     * Prints a prompt and reads one line.
     *
     * @param prompt text to show before reading
     * @return the raw line typed, or null if input has ended (Ctrl+D)
     */
    public String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        try {
            return in.nextLine();
        }
        catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Prints a plain message.
     *
     * @param message the message
     */
    public void showMessage(String message) {
        out.println(message);
    }

    /**
     * Prints an error message.
     *
     * @param message the error text
     */
    public void showError(String message) {
        out.println("  ! " + message);
    }

    /**
     * Prints the draft board.
     *
     * @param onClock     team whose turn it is
     * @param round       current round
     * @param totalRounds total rounds
     * @param pick        overall pick number
     * @param available   players still in the pool
     */
    public void showBoard(Team onClock, int round, int totalRounds, int pick,
        List<Player> available) {
        out.println();
        out.println("==================================================");
        out.println("Round " + round + " of " + totalRounds + "  |  Pick "
            + pick + "  |  On the clock: " + onClock.getName());
        out.println("--------------------------------------------------");
        out.println("Available players:");
        for (int i = 0; i < available.size(); i++) {
            Player p = available.get(i);
            out.printf("  %-24s %-4s%n", p.getName(), p.getPosition());
        }
        out.println("==================================================");
    }

    /**
     * Prints the result of an accepted pick.
     *
     * @param team   the team that picked
     * @param player the player drafted
     */
    public void showPickResult(Team team, Player player) {
        out.println("  > " + team.getName() + " drafts " + player.getName()
            + " (" + player.getPosition() + ")");
    }

    /**
     * Prints how to use the program, with examples that use real team names.
     *
     * @param teamA       a team name for the examples
     * @param teamB       another team name for the examples
     * @param firstRound  the first round whose picks can be traded, or 0 if
     *                    none can
     * @param totalRounds the last round
     * @param limits      position limits per team (may be empty)
     */
    public void showHelp(String teamA, String teamB, int firstRound,
        int totalRounds, Map<String, Integer> limits) {
        out.println();
        out.println("------------------- HOW IT WORKS -------------------");
        out.println("* Teams take turns picking. The order reverses every "
            + "round (1 to N, then N to 1), so it snakes.");
        out.println("* To draft: type the player's name on your turn. "
            + "Capitals, spaces and punctuation don't matter.");
        if (limits.isEmpty()) {
            out.println("* There are no roster limits.");
        }
        else {
            out.println("* Roster limits per team: " + formatLimits(limits)
                + ". You can't go over a limit unless no other position is "
                + "left.");
        }
        if (firstRound == 0) {
            out.println("* Trading: there are no future rounds left, so "
                + "picks can't be traded now.");
        }
        else {
            int other = Math.min(firstRound + 1, totalRounds);
            out.println("* To trade picks: type trade and answer the "
                + "questions. Or do it in one line:");
            out.println("      trade <team giving> <its round> <other team> "
                + "<their round>");
            out.println("      Example: trade " + teamA + " " + firstRound
                + " " + teamB + " " + other);
            out.println("      means " + teamA + " gives its round "
                + firstRound + " pick to " + teamB + ", and " + teamB
                + " gives its round " + other + " pick to " + teamA + ".");
            out.println("  Only picks in future rounds can be traded, and "
                + "trading doesn't use up your turn.");
        }
        out.println("* order: see who picks when in each remaining round.");
        out.println("* help: show this guide again.");
        out.println("----------------------------------------------------");
    }

    /**
     * Prints how many players of each limited position a team has.
     *
     * @param team   the team
     * @param limits position limits per team (may be empty)
     */
    public void showTeamStatus(Team team, Map<String, Integer> limits) {
        if (limits.isEmpty()) {
            int size = team.getRoster().size();
            out.println(team.getName() + " has " + size
                + (size == 1 ? " player." : " players."));
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : limits.entrySet()) {
            int count = 0;
            for (Player p : team.getRoster()) {
                if (p.getPosition().equalsIgnoreCase(entry.getKey())) {
                    count++;
                }
            }
            if (sb.length() > 0) {
                sb.append("  ");
            }
            sb.append(entry.getKey()).append(' ').append(count).append('/')
                .append(entry.getValue());
            if (count >= entry.getValue()) {
                sb.append(" (full)");
            }
        }
        out.println(possessive(team.getName()) + " roster: " + sb);
    }

    /** @return the possessive form: "Bears'" or "New York's" */
    private static String possessive(String name) {
        return name.endsWith("s") || name.endsWith("S") ? name + "'"
            : name + "'s";
    }

    /** @return limits formatted like "QB 2, RB 4" */
    private static String formatLimits(Map<String, Integer> limits) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : limits.entrySet()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append(' ').append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * Prints the result of an accepted trade.
     *
     * @param teamA  first team's name
     * @param roundA round of the pick team A gave up
     * @param teamB  second team's name
     * @param roundB round of the pick team B gave up
     */
    public void showTradeResult(String teamA, int roundA, String teamB,
        int roundB) {
        out.println("  > Trade: " + teamA + " gives its round " + roundA
            + " pick to " + teamB + " for " + possessive(teamB) + " round "
            + roundB
            + " pick.");
    }

    /**
     * Prints the planned pick order for one round.
     *
     * @param round the round number
     * @param order the teams in pick order
     */
    public void showRoundOrder(int round, List<Team> order) {
        StringBuilder sb = new StringBuilder();
        for (Team team : order) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(team.getName());
        }
        out.println("    Round " + round + " order: " + sb);
    }

    /**
     * Prints every team's final roster.
     *
     * @param teams all teams
     */
    public void showFinalRosters(List<Team> teams) {
        out.println();
        out.println("================ FINAL ROSTERS ================");
        for (Team team : teams) {
            out.println(team.getName() + ":");
            List<Player> roster = team.getRoster();
            if (roster.isEmpty()) {
                out.println("  (no players)");
            }
            for (int i = 0; i < roster.size(); i++) {
                Player p = roster.get(i);
                out.println("  " + (i + 1) + ". " + p.getName() + " ("
                    + p.getPosition() + ")");
            }
        }
    }
}
