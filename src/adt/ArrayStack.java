package adt;

import java.util.Arrays;

public class ArrayStack<T> implements StackInterface<T> {
    private T[] stack;
    private int topIndex;
    private static final int DEFAULT_CAPACITY = 25;

    public ArrayStack() {
        this(DEFAULT_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public ArrayStack(int initialCapacity) {
        stack = (T[]) new Object[initialCapacity];
        topIndex = -1;
    }

    @Override
    public void push(T newEntry) {
        ensureCapacity();
        topIndex++;
        stack[topIndex] = newEntry;
    }

    @Override
    public T pop() {
        if (isEmpty()) {
            return null;
        }
        T top = stack[topIndex];
        stack[topIndex] = null;
        topIndex--;
        return top;
    }

    @Override
    public T peek() {
        if (isEmpty()) {
            return null;
        }
        return stack[topIndex];
    }

    @Override
    public boolean isEmpty() {
        return topIndex < 0;
    }

    @Override
    public void clear() {
        while (topIndex >= 0) {
            stack[topIndex] = null;
            topIndex--;
        }
    }

    @Override
    public int size() {
        return topIndex + 1;
    }

    private void ensureCapacity() {
        if (topIndex == stack.length - 1) {
            stack = Arrays.copyOf(stack, stack.length * 2);
        }
    }
}
