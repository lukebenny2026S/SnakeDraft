package snakedraft.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One team in the draft: a name and the roster of players it has drafted.
 */
public class Team {

    private final String name;
    private final List<Player> roster;

    /**
     * Creates a team with an empty roster.
     *
     * @param name the team name
     * @throws IllegalArgumentException if the name is null or blank
     */
    public Team(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Team name cannot be blank.");
        }
        this.name = name.trim();
        this.roster = new ArrayList<>();
    }

    /** @return the team name */
    public String getName() {
        return name;
    }

    /** @return a read-only view of the drafted players, in pick order */
    public List<Player> getRoster() {
        return Collections.unmodifiableList(roster);
    }

    /**
     * Adds a player to the end of the roster.
     *
     * @param player the player to add
     * @throws IllegalArgumentException if the player is null
     */
    public void addPlayer(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Cannot add a null player.");
        }
        roster.add(player);
    }

    @Override
    public String toString() {
        return name;
    }
}
