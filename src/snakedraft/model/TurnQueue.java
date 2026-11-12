package snakedraft.model;

import java.util.NoSuchElementException;

/**
 * A first-in, first-out queue built on our own singly linked chain of nodes.
 * It knows nothing about players or drafts, so it can be tested with Strings.
 *
 * @param <T> the type of item waiting in line
 */
public class TurnQueue<T> {

    /** One link in the chain. */
    private static class Node<T> {
        private final T data;
        private Node<T> next;

        Node(T data) {
            this.data = data;
        }
    }

    private Node<T> head; // front: next item out
    private Node<T> tail; // back: last item in
    private int size;

    /** Creates an empty queue. */
    public TurnQueue() {
        head = null;
        tail = null;
        size = 0;
    }

    /**
     * Adds an item at the back.
     *
     * @param item the item to add
     * @throws IllegalArgumentException if the item is null (queue unchanged)
     */
    public void enqueue(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Cannot enqueue null.");
        }
        Node<T> node = new Node<>(item);
        if (isEmpty()) {
            head = node;
        }
        else {
            tail.next = node;
        }
        tail = node;
        size++;
    }

    /**
     * Removes and returns the front item.
     *
     * @return the front item
     * @throws NoSuchElementException if the queue is empty (queue unchanged)
     */
    public T dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty.");
        }
        T item = head.data;
        head = head.next;
        if (head == null) {
            tail = null;
        }
        size--;
        return item;
    }

    /**
     * Returns the front item without removing it.
     *
     * @return the front item
     * @throws NoSuchElementException if the queue is empty
     */
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty.");
        }
        return head.data;
    }

    /** @return true if no items are waiting */
    public boolean isEmpty() {
        return size == 0;
    }

    /** @return the number of items waiting */
    public int size() {
        return size;
    }
}
