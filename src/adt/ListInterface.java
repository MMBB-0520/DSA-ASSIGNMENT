package adt;

import java.util.Iterator;

/**
 * Custom Linear List ADT Interface.
 * Defines the abstract operations for a generic ordered list collection.
 *
 * @param <T> The type of elements stored in the list.
 */
public interface ListInterface<T> extends Iterable<T> {

    /**
     * Adds a new entry to the end of this list.
     *
     * @param newEntry The object to be added.
     * @return True if the addition is successful.
     */
    boolean add(T newEntry);

    /**
     * Adds a new entry at a specified position within this list.
     *
     * @param newPosition An integer that specifies the desired position (1-based).
     * @param newEntry    The object to be added.
     * @return True if the addition is successful, false if position is invalid.
     */
    boolean add(int newPosition, T newEntry);

    /**
     * Removes the entry at a given position from this list.
     *
     * @param givenPosition An integer that specifies the position (1-based).
     * @return The removed object, or null if position is invalid.
     */
    T remove(int givenPosition);

    /**
     * Removes the first occurrence of a specific entry from this list.
     *
     * @param anEntry The object to be removed.
     * @return True if the entry was found and removed, false otherwise.
     */
    boolean remove(T anEntry);

    /**
     * Removes and returns the first entry from this list.
     * [Add-on method: Hu Qiao Feng]
     *
     * @return The first object in the list, or null if list is empty.
     */
    T removeFirst();

    /**
     * Removes and returns the last entry from this list.
     * [Add-on method: Pang Jia Yie]
     *
     * @return The last object in the list, or null if list is empty.
     */
    T removeLast();

    /**
     * Replaces the entry at a given position in this list with a new entry.
     *
     * @param givenPosition An integer that specifies the position (1-based).
     * @param newEntry    The replacement object.
     * @return True if replacement is successful, false if position is invalid.
     */
    boolean set(int givenPosition, T newEntry);

    /**
     * Retrieves the entry at a given position in this list.
     *
     * @param givenPosition An integer that specifies the position (1-based).
     * @return The object at the given position, or null if position is invalid.
     */
    T get(int givenPosition);

    /**
     * See whether this list contains a given entry.
     *
     * @param anEntry The object to locate.
     * @return True if the list contains the entry.
     */
    boolean contains(T anEntry);

    /**
     * Returns the 0-based index of the first occurrence of a given entry.
     *
     * @param anEntry The object to search for.
     * @return The index of the entry, or -1 if not found.
     */
    int indexOf(T anEntry);

    /**
     * Gets the number of entries currently in this list.
     *
     * @return The integer number of entries.
     */
    int size();

    /**
     * See whether this list is empty.
     *
     * @return True if the list is empty.
     */
    boolean isEmpty();

    /**
     * Removes all entries from this list.
     */
    void clear();

    /**
     * Converts the list entries into a raw Object array.
     * [Add-on method: Ng Yuen Qi]
     *
     * @return An array containing all entries in sequential order.
     */
    Object[] toArray();

    /**
     * Gets an iterator for traversing the entries in this list.
     *
     * @return An Iterator object.
     */
    Iterator<T> getIterator();
}
