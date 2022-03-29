package edu.tamu.cse.lenss.edgeKeeper.orch;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * A FIFO queue, written by Amran Haroon.
 *
 * As its name implies, this is a first-in, first-out queue.
 *
 * No need to add a `reverse` method here as you can do that in Java with
 * `Collections.reverse(list)`.
 *
 */
public class FifoQCustomSize<E> implements Serializable {

    /**
	 *  No idea why we must add serial version ID.
	 */
	private static final long serialVersionUID = 1L;
	private List<E> list = new LinkedList<E>();
    private int maxSize = 3;

    public FifoQCustomSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public void put(E e) {
        list.add(e);
        if (list.size() > maxSize) list.remove(0);
    }

    /**
     * can return `null`
     */
    public E pop() {
        if (list.size() > 0) {
            E e = list.get(0);
            list.remove(0);
            return e;
        } else {
            return null;
        }
    }

    /**
     * @throws
     */
    public E peekOldest() {
        return list.get(0);
    }

    public E peekLatest(){
        if(!list.isEmpty()){
            return list.get(list.size()-1);
        }
        return null;
    }

    public boolean contains(E e) {
        return list.contains(e);
    }

    /**
     * @throws
     */
    public E get(int i) {
        return list.get(i);
    }

    public List<E> getBackingList() {
        // return a copy of the list
        return new LinkedList<E>(list);
    }

    public void clear() {
        list.clear();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    // mostly needed for testing
    public int size() {
        return list.size();
    }

}