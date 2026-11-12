package snakedraft.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The fixed MVP player pool. Loading real data from a file is a stretch goal.
 */
public final class PlayerPool {

    private static final String[][] DATA = {
        { "Patrick Mahomes", "QB" }, { "Josh Allen", "QB" },
        { "Lamar Jackson", "QB" }, { "Jalen Hurts", "QB" },
        { "Joe Burrow", "QB" }, { "C.J. Stroud", "QB" },
        { "Christian McCaffrey", "RB" }, { "Bijan Robinson", "RB" },
        { "Saquon Barkley", "RB" }, { "Jahmyr Gibbs", "RB" },
        { "Derrick Henry", "RB" }, { "Breece Hall", "RB" },
        { "Jonathan Taylor", "RB" }, { "Kyren Williams", "RB" },
        { "Ja'Marr Chase", "WR" }, { "Justin Jefferson", "WR" },
        { "CeeDee Lamb", "WR" }, { "Tyreek Hill", "WR" },
        { "Amon-Ra St. Brown", "WR" }, { "A.J. Brown", "WR" },
        { "Puka Nacua", "WR" }, { "Garrett Wilson", "WR" },
        { "Nico Collins", "WR" }, { "Malik Nabers", "WR" },
        { "Travis Kelce", "TE" }, { "Sam LaPorta", "TE" },
        { "George Kittle", "TE" }, { "Mark Andrews", "TE" },
        { "Trey McBride", "TE" }, { "Brock Bowers", "TE" },
        { "Justin Tucker", "K" }, { "Brandon Aubrey", "K" },
        { "Harrison Butker", "K" }, { "Jake Elliott", "K" },
        { "49ers Defense", "DEF" }, { "Ravens Defense", "DEF" },
        { "Cowboys Defense", "DEF" }, { "Jets Defense", "DEF" },
        { "Browns Defense", "DEF" }, { "Steelers Defense", "DEF" },
    };

    private PlayerPool() {
        // static helper only
    }

    /** @return a new list containing the fixed pool of 40 players */
    public static List<Player> defaultPlayers() {
        List<Player> players = new ArrayList<>();
        for (String[] row : DATA) {
            players.add(new Player(row[0], row[1]));
        }
        return players;
    }
}
