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
            else if (DraftModel.normalize(name).isEmpty()) {
                view.showError("Team name needs at least one letter or digit.");
            }
            else if (!used.add(DraftModel.normalize(name))) {
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
            view.showMessage("Type a player name, or trade picks with: "
                + "trade <team> <round> <team> <round>");
            String raw = view.readLine(model.getTeamOnClock().getName()
                + ", your move: ");
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
     * Trims and format-checks the input. A line starting with the word
     * "trade" goes to handleTrade; anything else is a pick for makePick.
     * Shows the result or the error.
     *
     * @param rawInput exactly what the user typed
     * @return true if a pick was accepted; false leaves the turn unchanged
     *         (including after a trade, which never uses up the turn)
     */
    public boolean handlePick(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) {
            view.showError("Please type a player's name.");
            return false;
        }
        String input = rawInput.trim();
        String[] words = input.split("\\s+");
        if (words[0].equalsIgnoreCase("trade")) {
            handleTrade(words);
            return false;
        }
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
     * Parses "trade <team> <round> <team> <round>" and calls tradePicks.
     * Team names may contain spaces ("trade New York 3 Bears 4"), so every
     * way of splitting the words around a round number is tried, and the
     * split whose two names are both real teams wins.
     *
     * @param words the trimmed input split on whitespace; words[0] is "trade"
     */
    private void handleTrade(String[] words) {
        String usage = "Usage: trade <team> <round> <team> <round>, "
            + "for example: trade Tigers 3 Bears 4";
        int last = words.length - 1;
        if (words.length < 5 || !isWholeNumber(words[last])) {
            view.showError(usage);
            return;
        }
        String teamA = null;
        String teamB = null;
        String roundAText = null;
        for (int i = 2; i <= last - 2; i++) {
            if (!isWholeNumber(words[i])) {
                continue;
            }
            String a = join(words, 1, i);
            String b = join(words, i + 1, last);
            if (teamA == null || (isTeam(a) && isTeam(b))) {
                teamA = a;
                teamB = b;
                roundAText = words[i];
            }
            if (isTeam(a) && isTeam(b)) {
                break;
            }
        }
        if (teamA == null) {
            view.showError(usage);
            return;
        }
        try {
            int roundA = Integer.parseInt(roundAText);
            int roundB = Integer.parseInt(words[last]);
            model.tradePicks(teamA, roundA, teamB, roundB);
            view.showTradeResult(canonicalName(teamA), roundA,
                canonicalName(teamB), roundB);
            view.showRoundOrder(roundA, model.getRoundOrder(roundA));
            if (roundB != roundA) {
                view.showRoundOrder(roundB, model.getRoundOrder(roundB));
            }
        }
        catch (NumberFormatException e) {
            view.showError("That round number is too large.");
        }
        catch (IllegalArgumentException | IllegalStateException e) {
            view.showError(e.getMessage());
        }
    }

    /** @return true if the word is an optional sign followed by digits */
    private static boolean isWholeNumber(String word) {
        return word.matches("[-+]?\\d+");
    }

    /** @return words[from..to) joined with single spaces */
    private static String join(String[] words, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(words[i]);
        }
        return sb.toString();
    }

    /** @return true if the name matches a team in the model */
    private boolean isTeam(String name) {
        return canonicalName(name) != null;
    }

    /** @return the team's real name for a typed name, or null if none */
    private String canonicalName(String name) {
        String key = DraftModel.normalize(name);
        for (Team team : model.getTeams()) {
            if (DraftModel.normalize(team.getName()).equals(key)) {
                return team.getName();
            }
        }
        return null;
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
