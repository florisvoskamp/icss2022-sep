package nl.han.ica.datastructures;

public class HANQueue<T> implements IHANQueue<T> {
    private static class Node<T> {
        private final T value;
        private Node<T> next;

        private Node(T value) {
            this.value = value;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    @Override
    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void enqueue(T value) {
        Node<T> n = new Node<>(value);
        if (size == 0) {
            head = n;
            tail = n;
            size = 1;
            return;
        }
        tail.next = n;
        tail = n;
        size++;
    }

    @Override
    public T dequeue() {
        if (size == 0) {
            return null;
        }
        T value = head.value;
        head = head.next;
        size--;
        if (size == 0) {
            tail = null;
        }
        return value;
    }

    @Override
    public T peek() {
        if (size == 0) {
            return null;
        }
        return head.value;
    }

    @Override
    public int getSize() {
        return size;
    }
}

