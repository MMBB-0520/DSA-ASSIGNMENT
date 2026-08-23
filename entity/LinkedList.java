// Team Collection ADT - Custom generic Linked List (submitted as the team's ONE Collection ADT)
// Module 1 (add, add(index), isEmpty, clear) authored by: <your name>
// Module 2 (remove, remove(element), get, size) - placeholder, to be authored by teammate
// Module 3 (set, contains, indexOf, toString) - placeholder, to be authored by teammate
package entity;

public class LinkedList<T> {

    private class Node {
        T data;
        Node next;

        Node(T data) {
            this.data = data;
            this.next = null;
        }
    }

    private Node head;
    private Node tail;
    private int size;

    public LinkedList() {
        head = null;
        tail = null;
        size = 0;
    }

    // ===================== MODULE 1 (yours) =====================

    public void add(T element) {
        Node newNode = new Node(element);
        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            tail.next = newNode;
            tail = newNode;
        }
        size++;
    }

    public void add(int index, T element) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        if (index == size) {
            add(element);
            return;
        }
        Node newNode = new Node(element);
        if (index == 0) {
            newNode.next = head;
            head = newNode;
            if (tail == null) {
                tail = newNode;
            }
        } else {
            Node prev = getNode(index - 1);
            newNode.next = prev.next;
            prev.next = newNode;
        }
        size++;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    private Node getNode(int index) {
        Node current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current;
    }

    // ===================== MODULE 2 (teammate - placeholder impl for now) =====================

    // Removes and returns the element at the given index
    public T remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        T removedData;
        if (index == 0) {
            removedData = head.data;
            head = head.next;
            if (head == null) {
                tail = null;
            }
        } else {
            Node prev = getNode(index - 1);
            Node target = prev.next;
            removedData = target.data;
            prev.next = target.next;
            if (target == tail) {
                tail = prev;
            }
        }
        size--;
        return removedData;
    }

    // Removes the first occurrence of a matching element
    public boolean remove(T element) {
        if (head == null) {
            return false;
        }
        if (head.data.equals(element)) {
            head = head.next;
            if (head == null) {
                tail = null;
            }
            size--;
            return true;
        }
        Node prev = head;
        Node current = head.next;
        while (current != null) {
            if (current.data.equals(element)) {
                prev.next = current.next;
                if (current == tail) {
                    tail = prev;
                }
                size--;
                return true;
            }
            prev = current;
            current = current.next;
        }
        return false;
    }

    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return getNode(index).data;
    }

    public int size() {
        return size;
    }

    // ===================== MODULE 3 (teammate - placeholder impl for now) =====================

    public void set(int index, T element) {
        Node node = getNode(index);
        node.data = element;
    }

    public boolean contains(T element) {
        return indexOf(element) != -1;
    }

    public int indexOf(T element) {
        Node current = head;
        int index = 0;
        while (current != null) {
            if (current.data.equals(element)) {
                return index;
            }
            current = current.next;
            index++;
        }
        return -1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Node current = head;
        while (current != null) {
            sb.append(current.data);
            if (current.next != null) {
                sb.append(", ");
            }
            current = current.next;
        }
        sb.append("]");
        return sb.toString();
    }
}
