package adt;

/**
 * Custom Non-Linear Binary Search Tree Interface (ADT).
 * Best Practice: Using interface as the variable type restricts available
 * methods
 * to abstract contract definitions, allowing implementation classes to be
 * replaced
 * without changing the rest of the application.
 *
 * @param <T> Element type that must implement Comparable interface.
 */
public interface BSTInterface<T extends Comparable<T>> {

    boolean isEmpty();

    int size();

    void clear();

    boolean insert(T newEntry);

    boolean contains(T anEntry);

    T search(T anEntry);

    T delete(T anEntry);

    T findMin();

    T findMax();

    Object[] inorder();

    Object[] preorder();

    Object[] postorder();

    java.util.Iterator<T> getIterator();
}
