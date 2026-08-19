package adt;

import java.util.Iterator;

public interface ListInterface<T> extends Iterable<T> {

    boolean add(T newEntry);

    boolean add(int newPosition, T newEntry);

    boolean isEmpty();

    void clear();

    T remove(int givenPosition);

    boolean remove(T anEntry);

    T get(int givenPosition);

    int size();

    boolean set(int givenPosition, T newEntry);

    boolean contains(T anEntry);

    int indexOf(T anEntry);

    String toString();

    T removeFirst();

    T removeLast();

    Object[] toArray();

    Iterator<T> getIterator();
}
