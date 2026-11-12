package snakedraft.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiny driver that runs a full draft with no view, no controller, and no
 * input. It proves the model stands on its own. Each team simply takes the
 * first available player.
 */
public class ModelDriver {

    /**
     * Runs a 3-team, 3-round draft and prints the pick order.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        List<Team> teams = new ArrayList<>();
        teams.add(new Team("A"));
        teams.add(new Team("B"));
        teams.add(new Team("C"));
        DraftModel model = new DraftModel(teams, PlayerPool.defaultPlayers(),
            3);

        while (!model.isDraftOver()) {
            Team team = model.getTeamOnClock();
            int round = model.getCurrentRound();
            int pick = model.getPickNumber();
            String name = model.getAvailablePlayers().get(0).getName();
            Player p = model.makePick(name);
            System.out.println("Round " + round + ", pick " + pick + ": "
                + team.getName() + " takes " + p);
        }

        // Bad input: the model rejects it and nothing changes.
        try {
            model.makePick("Patrick Mahomes");
        }
        catch (IllegalStateException e) {
            System.out.println("Rejected after draft over: " + e.getMessage());
        }

        for (Team t : model.getTeams()) {
            System.out.println(t.getName() + ": " + t.getRoster());
        }
    }
}
