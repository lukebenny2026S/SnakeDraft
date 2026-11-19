package snakedraft.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import snakedraft.model.DraftModel;
import snakedraft.model.Player;
import snakedraft.model.PlayerPool;
import snakedraft.model.Team;
import snakedraft.view.BoardState;
import snakedraft.view.DraftGui;
import snakedraft.view.SetupDialog;

/**
 * Controller for the graphical view. Same job as DraftController: it takes
 * what the user did, calls the model, and tells the view what to show. All
 * draft rules stay in DraftModel.
 */
public class GuiController {

    private final DraftGui view;
    private DraftModel model;

    /**
     * Connects a new GUI to the controller.
     *
     * @param view the graphical view
     * @throws IllegalArgumentException if view is null
     */
    public GuiController(DraftGui view) {
        if (view == null) {
            throw new IllegalArgumentException("View is required.");
        }
        this.view = view;
        view.setPickListener(this::handlePick);
        view.setNewDraftListener(this::restart);
    }

    /**
     * Runs setup, then shows the window.
     *
     * @return false if the user cancelled setup (nothing is shown)
     */
    public boolean start() {
        if (!newDraft()) {
            view.dispose();
            return false;
        }
        view.setVisible(true);
        return true;
    }

    /** Asks for new settings; keeps the current draft if cancelled. */
    private void restart() {
        newDraft();
    }

    private boolean newDraft() {
        while (true) {
            SetupDialog.Result r = SetupDialog.show(
                view.isVisible() ? view : null, PlayerPool.defaultPlayers().size());
            if (r == null) {
                return false;
            }
            try {
                List<Team> teams = new ArrayList<>();
                for (String name : r.teamNames) {
                    teams.add(new Team(name));
                }
                model = new DraftModel(teams, PlayerPool.defaultPlayers(),
                    r.rounds);
                view.resetFilters();
                refresh(null);
                return true;
            }
            catch (IllegalArgumentException e) {
                view.showErrorDialog(e.getMessage());
            }
        }
    }

    /**
     * Drafts the named player for the team on the clock and refreshes the
     * view, or shows the model's error message.
     *
     * @param playerName the player's name
     * @return true if the pick was accepted
     */
    public boolean handlePick(String playerName) {
        try {
            Team team = model.getTeamOnClock();
            Player player = model.makePick(playerName);
            refresh(team);
            view.showPickResult(team, player);
            return true;
        }
        catch (IllegalArgumentException | IllegalStateException e) {
            view.showError(e.getMessage());
            return false;
        }
    }

    private void refresh(Team lastPicker) {
        view.showState(boardState(model, lastPicker));
    }

    /**
     * Builds the plain-value snapshot the view draws.
     *
     * @param model      the draft
     * @param lastPicker team that just picked, or null
     * @return the snapshot
     */
    static BoardState boardState(DraftModel model, Team lastPicker) {
        List<Team> teams = model.getTeams();
        int round = model.getCurrentRound();
        List<Team> order = turnOrder(teams, round);
        int picksBefore = (model.getPickNumber() - 1) - (round - 1) * teams.size();
        int clock = model.isDraftOver() ? order.size()
            : Math.max(0, Math.min(picksBefore, order.size() - 1));
        return new BoardState(order, clock, round, model.getTotalRounds(),
            model.getPickNumber(), round % 2 == 0, model.isDraftOver(),
            model.getAvailablePlayers(), teams, lastPicker);
    }

    /**
     * The pick order for one round of the snake: odd rounds go 1..N, even
     * rounds N..1 (the same rule DraftModel uses to refill its queue).
     *
     * @param teams teams in round-1 order
     * @param round 1-based round number
     * @return the teams in the order they pick this round
     */
    static List<Team> turnOrder(List<Team> teams, int round) {
        List<Team> order = new ArrayList<>(teams);
        if (round % 2 == 0) {
            Collections.reverse(order);
        }
        return order;
    }
}
