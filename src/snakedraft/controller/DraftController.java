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
        return new DraftModel(teams, players, rounds,
            DraftModel.defaultPositionLimits());
    }

    /**
     * Loops until the draft is over: shows the board, reads a pick, handles
     * it. Then shows the final rosters. Exits cleanly if input ends.
     */
    public void run() {
        showHelp();
        while (!model.isDraftOver()) {
            view.showBoard(model.getTeamOnClock(), model.getCurrentRound(),
                model.getTotalRounds(), model.getPickNumber(),
                model.getAvailablePlayers());
            view.showTeamStatus(model.getTeamOnClock(),
                model.getPositionLimits());
            view.showMessage("Type a player's name to draft, or a command: "
                + "trade, order, help");
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
            if (words.length == 1) {
                guidedTrade();
            }
            else {
                handleTrade(words);
            }
            return false;
        }
        if (input.equalsIgnoreCase("help")) {
            showHelp();
            return false;
        }
        if (input.equalsIgnoreCase("order")) {
            showOrder();
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
        String usage = "Usage: trade <team giving> <its round> "
            + "<other team> <their round>. Type just \"trade\" to be "
            + "walked through it step by step.";
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
            view.showMessage("    Type \"order\" to see who owns each pick, "
                + "or just \"trade\" for step-by-step help.");
        }
    }

    /**
     * Walks the user through a trade one question at a time. Typing
     * "cancel" or a blank line at any step cancels; nothing changes until
     * the user confirms.
     */
    private void guidedTrade() {
        if (model.getCurrentRound() >= model.getTotalRounds()) {
            view.showError("This is the last round, so there are no future "
                + "picks left to trade.");
            return;
        }
        view.showMessage("Trade picks (type cancel at any step to stop).");
        String teamA = askTeam("Which team is giving up a pick? ", null);
        if (teamA == null) {
            return;
        }
        int roundA = askRound(teamA);
        if (roundA < 0) {
            return;
        }
        String teamB = askTeam("Which team gets " + possessive(teamA) + " round "
            + roundA + " pick? ", teamA);
        if (teamB == null) {
            return;
        }
        int roundB = askRound(teamB);
        if (roundB < 0) {
            return;
        }
        String answer = view.readLine("Confirm: " + teamA + " gives its round "
            + roundA + " pick to " + teamB + ", and " + teamB
            + " gives its round " + roundB + " pick to " + teamA
            + ". Type yes to confirm: ");
        if (answer == null || !(answer.trim().equalsIgnoreCase("yes")
            || answer.trim().equalsIgnoreCase("y"))) {
            view.showMessage("Trade canceled. Nothing changed.");
            return;
        }
        handleTrade(("trade " + teamA + " " + roundA + " " + teamB + " "
            + roundB).split("\\s+"));
    }

    /**
     * Asks for a team name until a real one is typed.
     *
     * @param prompt  the question to show
     * @param exclude a team name that is not allowed (or null)
     * @return the team's real name, or null if the user canceled
     */
    private String askTeam(String prompt, String exclude) {
        StringBuilder choices = new StringBuilder();
        for (Team team : model.getTeams()) {
            if (!team.getName().equals(exclude)) {
                if (choices.length() > 0) {
                    choices.append(", ");
                }
                choices.append(team.getName());
            }
        }
        while (true) {
            String raw = view.readLine(prompt + "(" + choices + "): ");
            if (isCancel(raw)) {
                view.showMessage("Trade canceled. Nothing changed.");
                return null;
            }
            String name = canonicalName(raw);
            if (name == null) {
                view.showError("No team named \"" + raw.trim()
                    + "\". Choose one of: " + choices + ".");
            }
            else if (name.equals(exclude)) {
                view.showError("A team cannot trade with itself. Choose "
                    + "one of: " + choices + ".");
            }
            else {
                return name;
            }
        }
    }

    /**
     * Asks which of the team's future picks to trade, listing the rounds
     * where it still owns one.
     *
     * @param teamName the team's real name
     * @return the chosen round, or -1 if canceled or the team has none
     */
    private int askRound(String teamName) {
        List<Integer> rounds = model.getTradeableRounds(teamName);
        if (rounds.isEmpty()) {
            view.showError(teamName + " has no picks left in future rounds, "
                + "so it has nothing to trade. Trade canceled.");
            return -1;
        }
        String list = rounds.toString().replaceAll("[\\[\\]]", "");
        while (true) {
            String raw = view.readLine("Which round's pick does " + teamName
                + " give? (" + list + "): ");
            if (isCancel(raw)) {
                view.showMessage("Trade canceled. Nothing changed.");
                return -1;
            }
            String text = raw.trim().replaceFirst("(?i)^round\\s*", "");
            if (isWholeNumber(text) && text.length() < 10
                && rounds.contains(Integer.parseInt(text))) {
                return Integer.parseInt(text);
            }
            view.showError(teamName + " can only trade its picks in rounds "
                + list + ". Type one of those numbers.");
        }
    }

    /** @return the possessive form: "Bears'" or "New York's" */
    private static String possessive(String name) {
        return name.endsWith("s") || name.endsWith("S") ? name + "'"
            : name + "'s";
    }

    /** @return true for end of input, a blank line, or the word cancel */
    private static boolean isCancel(String raw) {
        return raw == null || raw.trim().isEmpty()
            || raw.trim().equalsIgnoreCase("cancel");
    }

    /** Shows the how-to-play guide, with examples using real team names. */
    private void showHelp() {
        List<Team> teams = model.getTeams();
        int next = model.getCurrentRound() + 1;
        view.showHelp(teams.get(0).getName(), teams.get(1).getName(),
            next <= model.getTotalRounds() ? next : 0,
            model.getTotalRounds(), model.getPositionLimits());
    }

    /** Shows the planned pick order for the current and later rounds. */
    private void showOrder() {
        view.showMessage("Who picks when (trades included):");
        for (int round = model.getCurrentRound();
            round <= model.getTotalRounds(); round++) {
            view.showRoundOrder(round, model.getRoundOrder(round));
        }
        view.showMessage("    (Round " + model.getCurrentRound()
            + " is in progress; " + model.getTeamOnClock().getName()
            + " is on the clock.)");
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
