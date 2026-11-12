package snakedraft.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import snakedraft.model.DraftModel;
import snakedraft.model.Player;
import snakedraft.model.Team;
import snakedraft.view.DraftView;

/**
 * Sits between the view and the model. Reads raw text from the view,
 * format-checks it, calls the model, and tells the view what to show.
 */
public class DraftController {

    /** Most teams allowed at setup (guards against huge values). */
    public static final int MAX_TEAMS = 12;

    private final DraftModel model;
    private final DraftView view;

    /**
     * Stores the model and view it connects.
     *
     * @param model the draft model
     * @param view  the text view
     * @throws IllegalArgumentException if either is null
     */
    public DraftController(DraftModel model, DraftView view) {
        if (model == null || view == null) {
            throw new IllegalArgumentException("Model and view are required.");
        }
        this.model = model;
        this.view = view;
    }

    /**
     * Runs setup prompts (team count, rounds, team names) and builds a model.
     * Re-prompts on bad input.
     *
     * @param view    the view to prompt through
     * @param players the player pool
     * @return the new model, or null if input ended during setup
     */
    public static DraftModel setUp(DraftView view, List<Player> players) {
        view.showMessage("=== SnakeDraft ===");
        int maxTeams = Math.min(MAX_TEAMS, players.size());
        int teamCount = readInt(view, "Number of teams (2-" + maxTeams + "): ",
            2, maxTeams);
        if (teamCount < 0) {
            return null;
        }
        int maxRounds = players.size() / teamCount;
        int rounds = readInt(view, "Number of rounds (1-" + maxRounds + "): ",
            1, maxRounds);
        if (rounds < 0) {
            return null;
        }

        List<Team> teams = new ArrayList<>();
        Set<String> used = new HashSet<>();
        while (teams.size() < teamCount) {
            String raw = view.readLine("Name for team " + (teams.size() + 1)
                + ": ");
            if (raw == null) {
                return null;
            }
            String name = raw.trim();
            if (name.isEmpty()) {
                view.showError("Team name cannot be blank.");
            }
            else if (!used.add(name.toLowerCase())) {
                view.showError("A team is already named \"" + name + "\".");
            }
            else {
                teams.add(new Team(name));
            }
        }
        return new DraftModel(teams, players, rounds);
    }

    /**
     * Loops until the draft is over: shows the board, reads a pick, handles
     * it. Then shows the final rosters. Exits cleanly if input ends.
     */
    public void run() {
        while (!model.isDraftOver()) {
            view.showBoard(model.getTeamOnClock(), model.getCurrentRound(),
                model.getTotalRounds(), model.getPickNumber(),
                model.getAvailablePlayers());
            String raw = view.readLine(model.getTeamOnClock().getName()
                + ", type a player name: ");
            if (raw == null) {
                view.showMessage("\nInput ended. Draft stopped early.");
                view.showFinalRosters(model.getTeams());
                return;
            }
            handlePick(raw);
        }
        view.showFinalRosters(model.getTeams());
    }

    /**
     * Trims and format-checks the input, calls makePick, and shows the
     * result or the error.
     *
     * @param rawInput exactly what the user typed
     * @return true if the pick was accepted; false leaves the turn unchanged
     */
    public boolean handlePick(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            view.showError("Please type a player's name.");
            return false;
        }
        String input = rawInput.trim();
        if (input.matches("[-+]?\\d+(\\.\\d+)?")) {
            view.showError("Type the player's name, not a number.");
            return false;
        }
        try {
            Team team = model.getTeamOnClock();
            Player player = model.makePick(input);
            view.showPickResult(team, player);
            return true;
        }
        catch (IllegalArgumentException | IllegalStateException e) {
            view.showError(e.getMessage());
            return false;
        }
    }

    /**
     * Reads a whole number in [min, max], re-prompting on bad input.
     *
     * @return the number, or -1 if input ended
     */
    private static int readInt(DraftView view, String prompt, int min,
        int max) {
        while (true) {
            String raw = view.readLine(prompt);
            if (raw == null) {
                return -1;
            }
            try {
                int value = Integer.parseInt(raw.trim());
                if (value >= min && value <= max) {
                    return value;
                }
                view.showError("Enter a number from " + min + " to " + max
                    + ".");
            }
            catch (NumberFormatException e) {
                view.showError("\"" + raw.trim()
                    + "\" is not a whole number. Try again.");
            }
        }
    }
}
