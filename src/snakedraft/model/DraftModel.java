package snakedraft.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * All state and rules of a snake draft. Never prints and never reads input.
 *
 * The TurnQueue holds the teams still waiting to pick in the current round.
 * When it empties, the next round refills it: odd rounds in order 1..N,
 * even rounds in reverse N..1. That refill is the snake.
 */
public class DraftModel {

    private final List<Team> teams;
    private final Map<String, Player> pool;
    private final Set<String> draftedNames;
    private final TurnQueue<Team> turnQueue;
    private final int totalRounds;
    private int currentRound;
    private int pickNumber;
    private boolean draftOver;

    /**
     * Builds the pool and fills the queue with the teams in round-1 order.
     *
     * @param teams       the teams, in round-1 pick order
     * @param players     the draftable players
     * @param totalRounds how many rounds to draft
     * @throws IllegalArgumentException for null arguments or elements, fewer
     *         than 2 teams, totalRounds < 1, duplicate team or player names, or
     *         fewer players than teams * rounds
     */
    public DraftModel(List<Team> teams, Collection<Player> players,
        int totalRounds) {
        if (teams == null || players == null) {
            throw new IllegalArgumentException("Teams and players are required.");
        }
        if (teams.size() < 2) {
            throw new IllegalArgumentException("A draft needs at least 2 teams.");
        }
        if (totalRounds < 1) {
            throw new IllegalArgumentException("A draft needs at least 1 round.");
        }

        Set<String> teamKeys = new HashSet<>();
        for (Team team : teams) {
            if (team == null) {
                throw new IllegalArgumentException("Team list contains null.");
            }
            if (!teamKeys.add(normalize(team.getName()))) {
                throw new IllegalArgumentException(
                    "Duplicate team name: " + team.getName());
            }
        }

        this.pool = new LinkedHashMap<>();
        for (Player player : players) {
            if (player == null) {
                throw new IllegalArgumentException("Player list contains null.");
            }
            String key = normalize(player.getName());
            if (pool.containsKey(key)) {
                throw new IllegalArgumentException(
                    "Duplicate player name: " + player.getName());
            }
            pool.put(key, player);
        }

        if (pool.size() < (long)teams.size() * totalRounds) {
            throw new IllegalArgumentException("Not enough players: need "
                + ((long)teams.size() * totalRounds) + ", have " + pool.size()
                + ".");
        }

        this.teams = new ArrayList<>(teams);
        this.draftedNames = new HashSet<>();
        this.turnQueue = new TurnQueue<>();
        this.totalRounds = totalRounds;
        this.currentRound = 1;
        this.pickNumber = 1;
        this.draftOver = false;
        refillQueue();
    }

    /**
     * Drafts the named player to the team on the clock and advances the turn.
     *
     * @param playerName the player's name (case and spacing ignored)
     * @return the drafted player
     * @throws IllegalArgumentException if the name is null, blank, unknown, or
     *         already drafted (no state changes)
     * @throws IllegalStateException if the draft is over (no state changes)
     */
    public Player makePick(String playerName) {
        if (draftOver) {
            throw new IllegalStateException("The draft is over.");
        }
        if (playerName == null || playerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter a player name.");
        }
        String key = normalize(playerName);
        if (draftedNames.contains(key)) {
            throw new IllegalArgumentException(
                "\"" + playerName.trim() + "\" has already been drafted.");
        }
        if (!pool.containsKey(key)) {
            throw new IllegalArgumentException(
                "No player named \"" + playerName.trim() + "\" in the pool.");
        }

        // All checks passed: now change state.
        Team team = turnQueue.dequeue();
        Player player = pool.remove(key);
        draftedNames.add(key);
        team.addPlayer(player);
        pickNumber++;
        advanceRoundIfNeeded();
        return player;
    }

    /**
     * @return the team whose turn it is
     * @throws IllegalStateException if the draft is over
     */
    public Team getTeamOnClock() {
        if (draftOver) {
            throw new IllegalStateException("The draft is over.");
        }
        return turnQueue.peek();
    }

    /** @return the current round, starting at 1 (the last round once over) */
    public int getCurrentRound() {
        return currentRound;
    }

    /** @return the overall number of the next pick, starting at 1 */
    public int getPickNumber() {
        return pickNumber;
    }

    /** @return the total number of rounds in this draft */
    public int getTotalRounds() {
        return totalRounds;
    }

    /** @return true when every round is complete */
    public boolean isDraftOver() {
        return draftOver;
    }

    /**
     * @param playerName a player name (case and spacing ignored)
     * @return true if the player is still in the pool; false for null/blank
     */
    public boolean isAvailable(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return false;
        }
        return pool.containsKey(normalize(playerName));
    }

    /** @return a read-only list of players still in the pool */
    public List<Player> getAvailablePlayers() {
        return Collections.unmodifiableList(new ArrayList<>(pool.values()));
    }

    /** @return a read-only list of all teams in round-1 order */
    public List<Team> getTeams() {
        return Collections.unmodifiableList(teams);
    }

    /**
     * Lowercases and removes all whitespace, so "Patrick  Mahomes",
     * "patrick mahomes" and "PatrickMahomes" all match.
     */
    static String normalize(String name) {
        return name.toLowerCase().replaceAll("\\s+", "");
    }

    /** Moves to the next round (or ends the draft) when the queue empties. */
    private void advanceRoundIfNeeded() {
        if (!turnQueue.isEmpty()) {
            return;
        }
        if (currentRound == totalRounds) {
            draftOver = true;
            return;
        }
        currentRound++;
        refillQueue();
    }

    /** Fills the queue for the current round: odd 1..N, even N..1. */
    private void refillQueue() {
        if (currentRound % 2 == 1) {
            for (int i = 0; i < teams.size(); i++) {
                turnQueue.enqueue(teams.get(i));
            }
        }
        else {
            for (int i = teams.size() - 1; i >= 0; i--) {
                turnQueue.enqueue(teams.get(i));
            }
        }
    }
}
