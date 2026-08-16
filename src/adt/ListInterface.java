package adt;

import java.util.Iterator;

public interface ListInterface<T> {

    // HU QIAO FENG
    boolean add(T newEntry);

    boolean add(int newPosition, T newEntry);

    boolean isEmpty();

    void clear();

    // PANG JIA YIE
    T remove(int givenPosition);

    boolean remove(T anEntry);

    T get(int givenPosition);

    int size();

    // [add-on method] PANG JIA YIE: Removes and returns the last entry (used for Rollback / Undo in Housekeeping)
    T removeLast();

    // NG YUEN QI
    boolean set(int givenPosition, T newEntry);

    boolean contains(T anEntry);

    int indexOf(T anEntry);

    String toString();

    // [add-on method] NG YUEN QI: Exporting as a raw array facilitates importing
    // into a BST (Non-linear ADT) for building a fast search index.
    Object[] toArray();

    Iterator<T> getIterator();
}