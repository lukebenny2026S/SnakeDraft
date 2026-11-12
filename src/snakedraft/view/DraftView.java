package snakedraft.view;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
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
