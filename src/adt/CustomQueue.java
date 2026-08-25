package adt;

import java.lang.reflect.Array;

public class CustomQueue<T> implements QueueInterface<T> {
    private ListInterface<T> list = new CustomLinkedList<>();

    @Override
    public void enqueue(T element) {
        list.add(element);
    }

    @Override
    public T dequeue() {
        if (isEmpty()) return null;
        return list.remove(0);
    }

    @Override
    public T peek() {
        if (isEmpty()) return null;
        return list.get(0);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T[] toArray(T[] a) {
        T[] result = (T[]) Array.newInstance(a.getClass().getComponentType(), list.size());
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }
}