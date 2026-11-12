package snakedraft.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PlayerTeamTest {

    @Test
    public void playerStoresNameAndPosition() {
        Player p = new Player("Alice", "QB");
        assertEquals("Alice", p.getName());
        assertEquals("QB", p.getPosition());
    }

    @Test
    public void playerRejectsNullOrBlank() {
        assertThrows(IllegalArgumentException.class,
            () -> new Player(null, "QB"));
        assertThrows(IllegalArgumentException.class,
            () -> new Player("Alice", "  "));
    }

    @Test
    public void teamStartsEmpty() {
        Team t = new Team("Tigers");
        assertEquals("Tigers", t.getName());
        assertTrue(t.getRoster().isEmpty());
    }

    @Test
    public void teamRejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Team(""));
        assertThrows(IllegalArgumentException.class, () -> new Team(null));
    }

    @Test
    public void addPlayerAppendsToRoster() {
        Team t = new Team("Tigers");
        Player p = new Player("Alice", "QB");
        t.addPlayer(p);
        assertEquals(1, t.getRoster().size());
        assertEquals(p, t.getRoster().get(0));
    }

    @Test
    public void addNullPlayerThrows() {
        Team t = new Team("Tigers");
        assertThrows(IllegalArgumentException.class, () -> t.addPlayer(null));
    }

    @Test
    public void rosterIsReadOnly() {
        Team t = new Team("Tigers");
        assertThrows(UnsupportedOperationException.class,
            () -> t.getRoster().add(new Player("Bob", "RB")));
    }
}
