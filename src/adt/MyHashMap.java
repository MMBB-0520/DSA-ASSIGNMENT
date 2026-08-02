/**
 * Author: Ng Yuen Qi (Student ID: 26WMR12898)
 * Class: MyHashMap
 * Description: Custom Non-Linear Hash Table (ADT) for storing and retrieving
 * guest booking records.
 */
package adt;

public class MyHashMap<K, V> {

    private static class Entry<K, V> {
        K key;
        V value;
        Entry<K, V> next;

        public Entry(K key, V value, Entry<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private Entry<K, V>[] table;
    private int size;
    private int capacity;
    private static final double LOAD_FACTOR_THRESHOLD = 0.75;

    @SuppressWarnings("unchecked")
    public MyHashMap() {
        this.capacity = 16;
        this.table = new Entry[this.capacity];
        this.size = 0;
    }

    private int hash(K key) {
        if (key == null)
            return 0;
        int hashCode = key.hashCode();
        // Ensure non-negative index
        return (hashCode & 0x7FFFFFFF) % capacity;
    }

    public void put(K key, V value) {
        if (key == null)
            return; // For simplicity, avoiding null keys

        if ((double) size / capacity >= LOAD_FACTOR_THRESHOLD) {
            resize();
        }

        int index = hash(key);
        Entry<K, V> current = table[index];

        while (current != null) {
            if (current.key.equals(key)) {
                current.value = value; // Update existing key
                return;
            }
            current = current.next;
        }

        // Key not found, insert at the beginning of the list
        table[index] = new Entry<>(key, value, table[index]);
        size++;
    }

    public V get(K key) {
        if (key == null)
            return null;

        int index = hash(key);
        Entry<K, V> current = table[index];

        while (current != null) {
            if (current.key.equals(key)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public void remove(K key) {
        if (key == null)
            return;

        int index = hash(key);
        Entry<K, V> current = table[index];
        Entry<K, V> prev = null;

        while (current != null) {
            if (current.key.equals(key)) {
                if (prev == null) {
                    table[index] = current.next;
                } else {
                    prev.next = current.next;
                }
                size--;
                return;
            }
            prev = current;
            current = current.next;
        }
    }

    public int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    private void resize() {
        int newCapacity = capacity * 2;
        Entry<K, V>[] newTable = new Entry[newCapacity];

        // Rehash all entries
        for (int i = 0; i < capacity; i++) {
            Entry<K, V> current = table[i];
            while (current != null) {
                Entry<K, V> next = current.next; // Store next before modifying current

                int newIndex = (current.key.hashCode() & 0x7FFFFFFF) % newCapacity;
                current.next = newTable[newIndex];
                newTable[newIndex] = current;

                current = next;
            }
        }

        this.table = newTable;
        this.capacity = newCapacity;
    }

    public Object[] values() {
        Object[] values = new Object[size];
        int index = 0;

        for (int i = 0; i < capacity; i++) {
            Entry<K, V> current = table[i];

            while (current != null) {
                values[index++] = current.value;
                current = current.next;
            }
        }

        return values;
    }
}
