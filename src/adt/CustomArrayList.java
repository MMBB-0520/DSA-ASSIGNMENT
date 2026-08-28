package adt;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

public class CustomArrayList<T> implements ListInterface<T> {
    private Object[] array;
    private int size = 0;

    public CustomArrayList() {
        array = new Object[10];
    }

    @Override
    public boolean add(T element) {
        if (size == array.length) {
            array = Arrays.copyOf(array, array.length * 2);
        }
        array[size++] = element;
        return true;
    }

    @Override
    public boolean add(int index, T element) {
        if (index < 0 || index > size)
            return false;
        if (size == array.length) {
            array = Arrays.copyOf(array, array.length * 2);
        }
        for (int i = size; i > index; i--) {
            array[i] = array[i - 1];
        }
        array[index] = element;
        size++;
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size)
            return null;
        return (T) array[index];
    }

    @Override
    public boolean set(int index, T element) {
        if (index < 0 || index >= size)
            return false;
        array[index] = element;
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T remove(int index) {
        if (index < 0 || index >= size)
            return null;
        T removed = (T) array[index];
        for (int i = index; i < size - 1; i++) {
            array[i] = array[i + 1];
        }
        array[--size] = null;
        return removed;
    }

    @Override
    public boolean remove(T element) {
        int index = indexOf(element);
        if (index != -1) {
            remove(index);
            return true;
        }
        return false;
    }

    @Override
    public T removeFirst() {
        if (isEmpty())
            return null;
        return remove(0);
    }

    @Override
    public T removeLast() {
        if (isEmpty())
            return null;
        return remove(size - 1);
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            array[i] = null;
        }
        size = 0;
    }

    @Override
    public boolean contains(T element) {
        return indexOf(element) != -1;
    }

    @Override
    public int indexOf(T element) {
        if (element == null) {
            for (int i = 0; i < size; i++) {
                if (array[i] == null)
                    return i;
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (element.equals(array[i]))
                    return i;
            }
        }
        return -1;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T[] toArray() {
        Object[] copy = new Object[size];
        System.arraycopy(array, 0, copy, 0, size);
        return (T[]) copy;
    }

    @Override
    public Iterator<T> iterator() {
        return getIterator();
    }

    @Override
    public Iterator<T> getIterator() {
        return new Iterator<T>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            @SuppressWarnings("unchecked")
            public T next() {
                return (T) array[cursor++];
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public void sort(Comparator<T> comparator) {
        if (size <= 1 || comparator == null)
            return;
        for (int i = 1; i < size; i++) {
            T key = (T) array[i];
            int j = i - 1;
            while (j >= 0 && comparator.compare((T) array[j], key) > 0) {
                array[j + 1] = array[j];
                j--;
            }
            array[j + 1] = key;
        }
    }
}