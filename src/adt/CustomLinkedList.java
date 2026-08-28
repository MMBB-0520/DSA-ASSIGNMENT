package adt;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Custom Doubly/Singly Linked List ADT Implementation.
 * Provides custom in-place sorting and linear collection management.
 *
 * @author Ng Yuen Qi
 * @author Hu Qiao Feng
 * @author Pang Jia Yie
 * @param <T> Element type stored in list.
 */
public class CustomLinkedList<T> implements ListInterface<T> {

    private class Node {
        private T data;
        private Node next;

        private Node(T data) {
            this(data, null);
        }

        private Node(T data, Node next) {
            this.data = data;
            this.next = next;
        }
    }

    private Node firstNode;
    private Node lastNode;
    private int numberOfEntries;

    private Node getNodeAt(int givenPosition) {
        if (givenPosition == numberOfEntries && lastNode != null) {
            return lastNode;
        }
        Node currentNode = firstNode;
        for (int counter = 1; counter < givenPosition; counter++) {
            currentNode = currentNode.next;
        }
        return currentNode;
    }

    public CustomLinkedList() {
        clear();
    }

    @Override
    public boolean add(T newEntry) {
        Node newNode = new Node(newEntry);
        if (isEmpty()) {
            firstNode = newNode;
            lastNode = newNode;
        } else {
            lastNode.next = newNode;
            lastNode = newNode;
        }
        numberOfEntries++;
        return true;
    }

    @Override
    public boolean add(int newPosition, T newEntry) {
        if (newPosition < 1 || newPosition > numberOfEntries + 1) {
            return false;
        }
        Node newNode = new Node(newEntry);
        if (isEmpty()) {
            firstNode = newNode;
            lastNode = newNode;
        } else if (newPosition == 1) {
            newNode.next = firstNode;
            firstNode = newNode;
        } else if (newPosition == numberOfEntries + 1) {
            lastNode.next = newNode;
            lastNode = newNode;
        } else {
            Node nodeBefore = getNodeAt(newPosition - 1);
            newNode.next = nodeBefore.next;
            nodeBefore.next = newNode;
        }
        numberOfEntries++;
        return true;
    }

    @Override
    public boolean isEmpty() {
        return numberOfEntries == 0;
    }

    @Override
    public void clear() {
        firstNode = null;
        lastNode = null;
        numberOfEntries = 0;
    }

    @Override
    public T remove(int givenPosition) {
        if (givenPosition < 1 || givenPosition > numberOfEntries) {
            return null;
        }
        T result;
        if (givenPosition == 1) {
            result = firstNode.data;
            firstNode = firstNode.next;
            if (numberOfEntries == 1) {
                lastNode = null;
            }
        } else {
            Node nodeBefore = getNodeAt(givenPosition - 1);
            Node nodeToRemove = nodeBefore.next;
            result = nodeToRemove.data;
            nodeBefore.next = nodeToRemove.next;
            if (givenPosition == numberOfEntries) {
                lastNode = nodeBefore;
            }
        }
        numberOfEntries--;
        return result;
    }

    @Override
    public boolean remove(T anEntry) {
        if (isEmpty()) {
            return false;
        }

        Node currentNode = firstNode;
        Node previousNode = null;

        while (currentNode != null) {
            boolean isMatch = (anEntry == null)
                    ? (currentNode.data == null)
                    : anEntry.equals(currentNode.data);

            if (isMatch) {
                if (previousNode == null) { // Removing head node
                    firstNode = currentNode.next;
                    if (numberOfEntries == 1) {
                        lastNode = null;
                    }
                } else { // Removing middle or tail node
                    previousNode.next = currentNode.next;
                    if (currentNode == lastNode) { // Removing tail node
                        lastNode = previousNode;
                    }
                }
                numberOfEntries--;
                return true;
            }

            previousNode = currentNode;
            currentNode = currentNode.next;
        }

        return false;
    }

    @Override
    public T get(int givenPosition) {
        if (givenPosition < 1 || givenPosition > numberOfEntries) {
            return null;
        }
        return getNodeAt(givenPosition).data;
    }

    @Override
    public int size() {
        return numberOfEntries;
    }

    @Override
    public boolean set(int givenPosition, T newEntry) {
        if (givenPosition < 1 || givenPosition > numberOfEntries) {
            return false;
        }
        Node targetNode = getNodeAt(givenPosition);
        targetNode.data = newEntry;
        return true;
    }

    @Override
    public boolean contains(T anEntry) {
        return indexOf(anEntry) != -1;
    }

    @Override
    public int indexOf(T anEntry) {
        Node currentNode = firstNode;
        int index = 0;
        while (currentNode != null) {
            if (anEntry == null) {
                if (currentNode.data == null) {
                    return index;
                }
            } else if (anEntry.equals(currentNode.data)) {
                return index;
            }
            currentNode = currentNode.next;
            index++;
        }
        return -1;
    }

    /**
     * [add-on method] HU QIAO FENG: Removes and returns the first entry (mirrors
     * removeLast();
     * used in Walk-In Registration to pop the front room off a temporary
     * available-rooms list when auto-assigning).
     */
    @Override
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        }
        return remove(1);
    }

    /**
     * [add-on method] PANG JIA YIE: Removes and returns the last entry
     * (used for Rollback / Undo in Housekeeping)
     */
    @Override
    public T removeLast() {
        if (isEmpty()) {
            return null;
        }
        return remove(numberOfEntries);
    }

    /**
     * [Add-on method] NG YUEN QI: Exporting as a raw array facilitates importing
     * into a BST (Non-linear ADT) for building a fast search index.
     */
    @Override
    public Object[] toArray() {
        Object[] result = new Object[numberOfEntries];
        Node curr = firstNode;
        int index = 0;
        while (curr != null) {
            result[index++] = curr.data;
            curr = curr.next;
        }
        return result;
    }

    /**
     * [Add-on method] NG YUEN QI: In-place Insertion Sort algorithm directly on
     * Linked List node pointers.
     * Re-links node pointers without allocating any temporary array or duplicate
     * objects.
     *
     * @param comparator The comparator specifying the sort ordering.
     */
    @Override
    public void sort(Comparator<T> comparator) {
        if (numberOfEntries <= 1 || firstNode == null || comparator == null) {
            return;
        }

        Node sortedHead = null;
        Node current = firstNode;

        while (current != null) {
            Node nextNode = current.next;

            // Insert current node into sortedHead chain
            if (sortedHead == null || comparator.compare(current.data, sortedHead.data) < 0) {
                current.next = sortedHead;
                sortedHead = current;
            } else {
                Node search = sortedHead;
                while (search.next != null && comparator.compare(current.data, search.next.data) >= 0) {
                    search = search.next;
                }
                current.next = search.next;
                search.next = current;
            }

            current = nextNode;
        }

        firstNode = sortedHead;
        Node tail = firstNode;
        while (tail != null && tail.next != null) {
            tail = tail.next;
        }
        lastNode = tail;
    }

    @Override
    public Iterator<T> getIterator() {
        return iterator();
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node currentNode = firstNode;

            @Override
            public boolean hasNext() {
                return currentNode != null;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more elements in LinkedList iterator.");
                }
                T data = currentNode.data;
                currentNode = currentNode.next;
                return data;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Node curr = firstNode;
        while (curr != null) {
            sb.append(curr.data);
            if (curr.next != null)
                sb.append(" -> ");
            curr = curr.next;
        }
        sb.append("]");
        return sb.toString();
    }
}