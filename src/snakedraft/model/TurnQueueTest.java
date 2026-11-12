package snakedraft.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import java.util.NoSuchElementException;
import org.junit.Before;
import org.junit.Test;

public class TurnQueueTest {

    private TurnQueue<String> queue;

    @Before
    public void setUp() {
        queue = new TurnQueue<>();
    }

    @Test
    public void newQueueIsEmpty() {
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    public void enqueueAddsToBack() {
        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");
        assertEquals(3, queue.size());
        assertFalse(queue.isEmpty());
        assertEquals("A", queue.peek());
    }

    @Test
    public void enqueueNullThrowsAndLeavesQueueUnchanged() {
        queue.enqueue("A");
        assertThrows(IllegalArgumentException.class, () -> queue.enqueue(null));
        assertEquals(1, queue.size());
        assertEquals("A", queue.peek());
    }

    @Test
    public void dequeueIsFirstInFirstOut() {
        queue.enqueue("A");
        queue.enqueue("B");
        queue.enqueue("C");
        assertEquals("A", queue.dequeue());
        assertEquals("B", queue.dequeue());
        assertEquals("C", queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void dequeueEmptyThrows() {
        assertThrows(NoSuchElementException.class, () -> queue.dequeue());
        assertEquals(0, queue.size());
    }

    @Test
    public void peekDoesNotRemove() {
        queue.enqueue("A");
        assertEquals("A", queue.peek());
        assertEquals(1, queue.size());
    }

    @Test
    public void peekEmptyThrows() {
        assertThrows(NoSuchElementException.class, () -> queue.peek());
    }

    @Test
    public void reusableAfterEmptying() {
        queue.enqueue("A");
        queue.dequeue();
        queue.enqueue("B");
        assertEquals("B", queue.peek());
        assertEquals(1, queue.size());
    }
}
