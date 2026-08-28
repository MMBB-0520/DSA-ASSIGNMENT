package adt;

import java.util.Iterator;

/**
 * Custom Non-Linear Binary Search Tree Interface (ADT).
 * Best Practice: Using interface as the variable type restricts available
 * methods to abstract contract definitions, allowing implementation classes to
 * be replaced without changing the rest of the application.
 *
 * Source Acknowledgement: Adapted and enhanced from sample code
 * BinarySearchTreeInterface.
 * 
 * @author Ng Yuen Qi
 * @param <T> Element type that must implement Comparable interface.
 */
public interface BSTInterface<T extends Comparable<T>> {

    /**
     * Inserts a new entry into this binary search tree while maintaining BST
     * ordering.
     *
     * @param newEntry The object to be inserted.
     * @return True if insertion is successful, false if entry is null or duplicate.
     */
    boolean insert(T newEntry);

    /**
     * Searches whether a specific entry exists in this binary search tree.
     *
     * @param anEntry The target object to locate.
     * @return True if matching entry exists, false otherwise.
     */
    boolean contains(T anEntry);

    /**
     * Performs an O(log n) average binary search to retrieve a matching entry.
     *
     * @param anEntry The search key object containing the search target.
     * @return The matching object stored in the tree, or null if not found.
     */
    T search(T anEntry);

    /**
     * Deletes a matching entry from this binary search tree and restructures nodes.
     *
     * @param anEntry The object to be removed.
     * @return The removed object, or null if entry not found.
     */
    T delete(T anEntry);

    /**
     * Sees whether this binary search tree is empty.
     *
     * @return True if the tree contains no nodes, false otherwise.
     */
    boolean isEmpty();

    /**
     * Gets the total number of nodes currently stored in this binary search tree.
     *
     * @return The integer total node count.
     */
    int size();

    /**
     * Removes all nodes and clears this binary search tree.
     */
    void clear();

    /**
     * Locates and retrieves the minimum key entry in this binary search tree.
     *
     * @return The minimum element according to compareTo, or null if tree is empty.
     */
    T findMin();

    /**
     * Locates and retrieves the maximum key entry in this binary search tree.
     *
     * @return The maximum element according to compareTo, or null if tree is empty.
     */
    T findMax();

    /**
     * Calculates and returns the maximum height/depth of this binary search tree.
     *
     * @return The maximum height (number of levels), or 0 if tree is empty.
     */
    int getHeight();

    /**
     * Rebalances this binary search tree to achieve minimum balanced height using
     * divide-and-conquer algorithm.
     */
    void rebalance();

    /**
     * Performs an in-order traversal (Left -> Node -> Right) and returns elements
     * as an array.
     *
     * @return Object array containing elements in sorted order.
     */
    Object[] inorder();

    /**
     * Performs a pre-order traversal (Node -> Left -> Right) and returns elements
     * as an array.
     *
     * @return Object array containing elements in pre-order.
     */
    Object[] preorder();

    /**
     * Performs a post-order traversal (Left -> Right -> Node) and returns elements
     * as an array.
     *
     * @return Object array containing elements in post-order.
     */
    Object[] postorder();

    /**
     * Retrieves an in-order iterator for traversing the elements in sorted
     * sequence.
     *
     * @return Iterator object traversing BST entries in-order.
     */
    Iterator<T> getIterator();
}
