// Author: <your name>
// Custom Linear ADT (Queue) - array-based implementation
// No java.util.List / ArrayList / Collections Framework classes used, per assignment rules.

package adt; // adjust package to match your NetBeans project structure

import entity.Booking;

public class MyQueue<T> {

    private Object[] items;
    private int front;
    private int rear;
    private int size;
    private int capacity;

    public MyQueue() {
        this(20); // default starting capacity
    }

    public MyQueue(int capacity) {
        this.capacity = capacity;
        this.items = new Object[capacity];
        this.front = 0;
        this.rear = -1;
        this.size = 0;
    }

    // Add a booking to the back of the queue (arrival order)
    public boolean enqueue(T item) {
        if (isFull()) {
            resize();
        }
        rear = (rear + 1) % capacity;
        items[rear] = item;
        size++;
        return true;
    }

    // Remove and return the booking at the front (next to be processed)
    @SuppressWarnings("unchecked")
    public T dequeue() {
        if (isEmpty()) {
            return null;
        }
        T item = (T) items[front];
        items[front] = null;
        front = (front + 1) % capacity;
        size--;
        return item;
    }

    // Look at the front item without removing it
    @SuppressWarnings("unchecked")
    public T peekFront() {
        if (isEmpty()) {
            return null;
        }
        return (T) items[front];
    }

    // Remove a specific booking out of order (e.g. guest cancels while waiting)
    @SuppressWarnings("unchecked")
    public T removeById(String bookingId) {
        for (int i = 0; i < size; i++) {
            int index = (front + i) % capacity;
            Booking b = (Booking) items[index];
            if (b != null && b.getBookingId().equals(bookingId)) {
                T removed = (T) items[index];
                // shift everything after it back by one position
                for (int j = i; j < size - 1; j++) {
                    int cur = (front + j) % capacity;
                    int next = (front + j + 1) % capacity;
                    items[cur] = items[next];
                }
                int lastIndex = (front + size - 1) % capacity;
                items[lastIndex] = null;
                size--;
                rear = (front + size - 1 + capacity) % capacity;
                return removed;
            }
        }
        return null; // not found
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean isFull() {
        return size == capacity;
    }

    public int getSize() {
        return size;
    }

    // Read-only access to an item by its position in queue order (0 = front).
    // Safer than a generic toArray(), which fails at runtime due to type erasure.
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return (T) items[(front + index) % capacity];
    }

    // Double the internal array size when full
    private void resize() {
        int newCapacity = capacity * 2;
        Object[] newItems = new Object[newCapacity];
        for (int i = 0; i < size; i++) {
            newItems[i] = items[(front + i) % capacity];
        }
        items = newItems;
        front = 0;
        rear = size - 1;
        capacity = newCapacity;
    }
}
