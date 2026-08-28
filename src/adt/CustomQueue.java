package adt;

import java.lang.reflect.Array;

public class CustomQueue<T> implements QueueInterface<T> {
    private ListInterface<T> list;

    public CustomQueue() {
        this.list = new CustomLinkedList<>();
    }

    @Override
    public void enqueue(T newEntry) {
        list.add(newEntry);
    }

    @Override
    public T dequeue() {
        if (isEmpty()) {
            return null;
        }
        return list.remove(1);
    }

    @Override
    public T peek() {
        if (isEmpty()) {
            return null;
        }
        return list.get(1);
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T[] toArray(T[] a) {
        T[] result = (T[]) Array.newInstance(a.getClass().getComponentType(), list.size());
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i + 1);
        }
        return result;
    }
}