package adt;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Custom Non-Linear Binary Search Tree (BST) ADT Implementation.
 * Uses internal Node structure (data, left, right) for standard O(log n) tree
 * operations.
 *
 * Source Acknowledgement: Adapted and enhanced from sample code
 * BinarySearchTree.
 * 
 * @author Ng Yuen Qi
 * @param <T> Element type implementing Comparable interface.
 */
public class MyBinarySearchTree<T extends Comparable<T>> implements BSTInterface<T> {

    private Node root;
    private int size;

    private class Node {
        private T data;
        private Node left;
        private Node right;

        public Node(T data) {
            this.data = data;
            this.left = null;
            this.right = null;
        }
    }

    public MyBinarySearchTree() {
        this.root = null;
        this.size = 0;
    }

    @Override
    public boolean insert(T newEntry) {
        if (newEntry == null) {
            return false;
        }
        int initialSize = size;
        root = insertHelper(root, newEntry);
        return size > initialSize;
    }

    private Node insertHelper(Node current, T newEntry) {
        if (current == null) {
            size++;
            return new Node(newEntry);
        }

        int comp = newEntry.compareTo(current.data);
        if (comp < 0) {
            current.left = insertHelper(current.left, newEntry);
        } else if (comp > 0) {
            current.right = insertHelper(current.right, newEntry);
        } else {
            // Duplicate confirmation number found: don't insert
            return current;
        }

        return current;
    }

    @Override
    public boolean contains(T anEntry) {
        return search(anEntry) != null;
    }

    @Override
    public T search(T anEntry) {
        if (anEntry == null || root == null) {
            return null;
        }
        return searchHelper(root, anEntry);
    }

    private T searchHelper(Node current, T anEntry) {
        if (current == null) {
            return null;
        }

        int comp = anEntry.compareTo(current.data);
        if (comp == 0) {
            return current.data;
        } else if (comp < 0) {
            return searchHelper(current.left, anEntry);
        } else {
            return searchHelper(current.right, anEntry);
        }
    }

    @Override
    public T delete(T anEntry) {
        if (anEntry == null || root == null) {
            return null;
        }
        Object[] removed = new Object[1];
        root = deleteHelper(root, anEntry, removed);
        if (removed[0] != null) {
            size--;
        }
        @SuppressWarnings("unchecked")
        T result = (T) removed[0];
        return result;
    }

    private Node deleteHelper(Node current, T anEntry, Object[] removed) {
        if (current == null) {
            return null;
        }

        int comp = anEntry.compareTo(current.data);
        if (comp < 0) {
            current.left = deleteHelper(current.left, anEntry, removed);
        } else if (comp > 0) {
            current.right = deleteHelper(current.right, anEntry, removed);
        } else {
            removed[0] = current.data;

            // Case 1 & 2: 0 or 1 child
            if (current.left == null) {
                return current.right;
            } else if (current.right == null) {
                return current.left;
            }

            // Case 3: 2 children -> replace with in-order successor (min in right subtree)
            T successorData = findMinHelper(current.right);
            current.data = successorData;
            current.right = deleteHelper(current.right, successorData, new Object[1]);
        }
        return current;
    }

    @Override
    public boolean isEmpty() {
        return root == null;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        root = null;
        size = 0;
    }

    @Override
    public T findMin() {
        if (isEmpty()) {
            return null;
        }
        return findMinHelper(root);
    }

    private T findMinHelper(Node current) {
        while (current.left != null) {
            current = current.left;
        }
        return current.data;
    }

    @Override
    public T findMax() {
        if (isEmpty()) {
            return null;
        }
        Node current = root;
        while (current.right != null) {
            current = current.right;
        }
        return current.data;
    }

    @Override
    public int getHeight() {
        return getHeightHelper(root);
    }

    private int getHeightHelper(Node current) {
        if (current == null) {
            return 0;
        }
        return 1 + Math.max(getHeightHelper(current.left), getHeightHelper(current.right));
    }

    @Override
    public void rebalance() {
        if (isEmpty()) {
            return;
        }
        Object[] sortedElements = inorder();
        clear();
        buildBalancedTreeHelper(sortedElements, 0, sortedElements.length - 1);
    }

    private void buildBalancedTreeHelper(Object[] elements, int start, int end) {
        if (start > end) {
            return;
        }
        int mid = start + (end - start) / 2;
        @SuppressWarnings("unchecked")
        T midData = (T) elements[mid];
        insert(midData);
        buildBalancedTreeHelper(elements, start, mid - 1);
        buildBalancedTreeHelper(elements, mid + 1, end);
    }

    @Override
    public Object[] inorder() {
        Object[] result = new Object[size];
        int[] index = new int[1];
        inorderHelper(root, result, index);
        return result;
    }

    private void inorderHelper(Node current, Object[] result, int[] index) {
        if (current != null) {
            inorderHelper(current.left, result, index);
            result[index[0]++] = current.data;
            inorderHelper(current.right, result, index);
        }
    }

    @Override
    public Object[] preorder() {
        Object[] result = new Object[size];
        int[] index = new int[1];
        preorderHelper(root, result, index);
        return result;
    }

    private void preorderHelper(Node current, Object[] result, int[] index) {
        if (current != null) {
            result[index[0]++] = current.data;
            preorderHelper(current.left, result, index);
            preorderHelper(current.right, result, index);
        }
    }

    @Override
    public Object[] postorder() {
        Object[] result = new Object[size];
        int[] index = new int[1];
        postorderHelper(root, result, index);
        return result;
    }

    private void postorderHelper(Node current, Object[] result, int[] index) {
        if (current != null) {
            postorderHelper(current.left, result, index);
            postorderHelper(current.right, result, index);
            result[index[0]++] = current.data;
        }
    }

    @Override
    public Iterator<T> getIterator() {
        return new InOrderIterator();
    }

    private class InOrderIterator implements Iterator<T> {
        private Object[] elements;
        private int currentIndex;

        public InOrderIterator() {
            this.elements = inorder();
            this.currentIndex = 0;
        }

        @Override
        public boolean hasNext() {
            return currentIndex < elements.length;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more elements in Binary Search Tree iterator.");
            }
            return (T) elements[currentIndex++];
        }
    }
}
