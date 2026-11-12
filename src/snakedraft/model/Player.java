package snakedraft.model;

/**
 * One draftable player. Immutable: once created, a player never changes.
 */
public final class Player {

    private final String name;
    private final String position;

    /**
     * Creates a player.
     *
     * @param name     the player's full name
     * @param position the player's position (QB, RB, WR, TE, K, DEF)
     * @throws IllegalArgumentException if either value is null or blank
     */
    public Player(String name, String position) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be blank.");
        }
        if (position == null || position.trim().isEmpty()) {
            throw new IllegalArgumentException("Player position cannot be blank.");
        }
        this.name = name.trim();
        this.position = position.trim();
    }

    /** @return the player's name */
    public String getName() {
        return name;
    }

    /** @return the player's position */
    public String getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return name + " (" + position + ")";
    }
}
