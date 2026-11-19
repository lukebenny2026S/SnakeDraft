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
 * When it empties, the next round refills it from that round's planned
 * order in roundOrders. Planned orders start as the snake (odd rounds 1..N,
 * even rounds N..1) and change only when teams trade future picks.
 */
public class DraftModel {

    private final List<Team> teams;
    private final Map<String, Player> pool;
    private final Set<String> draftedNames;
    private final TurnQueue<Team> turnQueue;
    private final List<List<Team>> roundOrders;
    private final int totalRounds;
    private final Map<String, Integer> positionLimits;
    private int currentRound;
    private int pickNumber;
    private boolean draftOver;

    /**
     * The roster limits the full program uses: at most 2 QB, 4 RB, 4 WR,
     * 2 TE, 1 K and 1 DEF per team.
     *
     * @return a new, modifiable map of position to maximum count
     */
    public static Map<String, Integer> defaultPositionLimits() {
        Map<String, Integer> limits = new LinkedHashMap<>();
        limits.put("QB", 2);
        limits.put("RB", 4);
        limits.put("WR", 4);
        limits.put("TE", 2);
        limits.put("K", 1);
        limits.put("DEF", 1);
        return limits;
    }

    /**
     * Builds a draft with no position limits.
     *
     * @param teams       the teams, in round-1 pick order
     * @param players     the draftable players
     * @param totalRounds how many rounds to draft
     * @throws IllegalArgumentException as in the four-argument constructor
     */
    public DraftModel(List<Team> teams, Collection<Player> players,
        int totalRounds) {
        this(teams, players, totalRounds, new LinkedHashMap<String, Integer>());
    }

    /**
     * Builds the pool and fills the queue with the teams in round-1 order.
     *
     * @param teams          the teams, in round-1 pick order
     * @param players        the draftable players
     * @param totalRounds    how many rounds to draft
     * @param positionLimits most players of each position one team may
     *                       draft (positions not listed have no limit)
     * @throws IllegalArgumentException for null arguments or elements, fewer
     *         than 2 teams, totalRounds < 1, duplicate team or player names,
     *         fewer players than teams * rounds, or a limit below 1
     */
    public DraftModel(List<Team> teams, Collection<Player> players,
        int totalRounds, Map<String, Integer> positionLimits) {
        if (teams == null || players == null || positionLimits == null) {
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
        this.roundOrders = new ArrayList<>();
        for (int round = 1; round <= totalRounds; round++) {
            List<Team> order = new ArrayList<>(this.teams);
            if (round % 2 == 0) {
                Collections.reverse(order);
            }
            roundOrders.add(order);
        }
        this.positionLimits = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : positionLimits.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()
                || entry.getValue() == null || entry.getValue() < 1) {
                throw new IllegalArgumentException(
                    "Each position limit needs a position and a value of "
                    + "at least 1.");
            }
            this.positionLimits.put(entry.getKey().trim().toUpperCase(),
                entry.getValue());
        }
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
     * @throws IllegalArgumentException if the name is null, blank, unknown,
     *         already drafted, or would put the team over a position limit
     *         while it still has a legal pick (no state changes)
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
        Team onClock = turnQueue.peek();
        Player wanted = pool.get(key);
        if (isFull(onClock, wanted.getPosition()) && hasLegalPick(onClock)) {
            String pos = wanted.getPosition().toUpperCase();
            throw new IllegalArgumentException(onClock.getName()
                + " already has " + getPositionCount(onClock, pos) + " " + pos
                + (getPositionCount(onClock, pos) == 1 ? "" : "s")
                + ", the most allowed. Pick a different position.");
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

    /**
     * Swaps two future picks: team A's pick in roundA goes to team B, and
     * team B's pick in roundB goes to team A. If a team owns two picks in a
     * round (from an earlier trade), its first one in that round is traded.
     *
     * @param teamA  name of the first team (case and spacing ignored)
     * @param roundA round of the first team's pick
     * @param teamB  name of the second team
     * @param roundB round of the second team's pick
     * @throws IllegalArgumentException if a name is null, blank or unknown,
     *         both names are the same team, a round is not after the current
     *         round or is past the last round, or a team owns no pick in its
     *         round (no state changes)
     * @throws IllegalStateException if the draft is over (no state changes)
     */
    public void tradePicks(String teamA, int roundA, String teamB,
        int roundB) {
        if (draftOver) {
            throw new IllegalStateException("The draft is over.");
        }
        Team a = findTeam(teamA);
        Team b = findTeam(teamB);
        if (a == b) {
            throw new IllegalArgumentException(
                "A team cannot trade with itself.");
        }
        checkFutureRound(roundA);
        checkFutureRound(roundB);
        List<Team> orderA = roundOrders.get(roundA - 1);
        List<Team> orderB = roundOrders.get(roundB - 1);
        int slotA = orderA.indexOf(a);
        int slotB = orderB.indexOf(b);
        if (slotA < 0) {
            throw new IllegalArgumentException(a.getName()
                + " has no pick in round " + roundA + ".");
        }
        if (slotB < 0) {
            throw new IllegalArgumentException(b.getName()
                + " has no pick in round " + roundB + ".");
        }

        // All checks passed: now change state.
        orderA.set(slotA, b);
        orderB.set(slotB, a);
    }

    /**
     * @param round a round from 1 to the total number of rounds
     * @return a read-only copy of that round's planned pick order
     * @throws IllegalArgumentException if the round is out of range
     */
    public List<Team> getRoundOrder(int round) {
        if (round < 1 || round > totalRounds) {
            throw new IllegalArgumentException("Round must be from 1 to "
                + totalRounds + ".");
        }
        return Collections.unmodifiableList(
            new ArrayList<>(roundOrders.get(round - 1)));
    }

    /**
     * @return a read-only map of position to the most players of that
     *         position one team may draft (empty if there are no limits)
     */
    public Map<String, Integer> getPositionLimits() {
        return Collections.unmodifiableMap(positionLimits);
    }

    /**
     * @param team     a team
     * @param position a position such as "QB" (case ignored)
     * @return how many players of that position the team has drafted
     */
    public int getPositionCount(Team team, String position) {
        int count = 0;
        for (Player p : team.getRoster()) {
            if (p.getPosition().equalsIgnoreCase(position)) {
                count++;
            }
        }
        return count;
    }

    /**
     * @param teamName a team name (case and spacing ignored)
     * @return the future rounds (after the current one) in which that team
     *         owns at least one pick, in order; empty if none
     * @throws IllegalArgumentException if the name is null, blank or unknown
     */
    public List<Integer> getTradeableRounds(String teamName) {
        Team team = findTeam(teamName);
        List<Integer> rounds = new ArrayList<>();
        if (draftOver) {
            return rounds;
        }
        for (int round = currentRound + 1; round <= totalRounds; round++) {
            if (roundOrders.get(round - 1).contains(team)) {
                rounds.add(round);
            }
        }
        return rounds;
    }

    /** @return a read-only list of all teams in round-1 order */
    public List<Team> getTeams() {
        return Collections.unmodifiableList(teams);
    }

    /**
     * Lowercases and removes everything except letters and digits, so
     * "C.J. Stroud", "cj stroud" and "CJStroud" all match.
     *
     * @param name a name
     * @return the normalized key
     */
    public static String normalize(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "");
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

    /** Fills the queue with the current round's planned order. */
    private void refillQueue() {
        for (Team team : roundOrders.get(currentRound - 1)) {
            turnQueue.enqueue(team);
        }
    }

    /**
     * @param name a team name (case and spacing ignored)
     * @return the matching team
     * @throws IllegalArgumentException if the name is null, blank or unknown
     */
    private Team findTeam(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Enter a team name.");
        }
        String key = normalize(name);
        for (Team team : teams) {
            if (normalize(team.getName()).equals(key)) {
                return team;
            }
        }
        throw new IllegalArgumentException(
            "No team named \"" + name.trim() + "\".");
    }

    /** @return true if the team has reached its limit for this position */
    private boolean isFull(Team team, String position) {
        Integer limit = positionLimits.get(position.toUpperCase());
        return limit != null && getPositionCount(team, position) >= limit;
    }

    /**
     * @return true if some player still in the pool fits under the team's
     *         limits. If none does, limits are waived so the draft can't stall.
     */
    private boolean hasLegalPick(Team team) {
        for (Player p : pool.values()) {
            if (!isFull(team, p.getPosition())) {
                return true;
            }
        }
        return false;
    }

    /** Throws unless round is after the current round and not past the end. */
    private void checkFutureRound(int round) {
        if (round <= currentRound || round > totalRounds) {
            if (currentRound >= totalRounds) {
                throw new IllegalArgumentException(
                    "No future rounds are left to trade.");
            }
            throw new IllegalArgumentException("Only picks in rounds "
                + (currentRound + 1) + " to " + totalRounds
                + " can be traded.");
        }
    }
}
