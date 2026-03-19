package nl.han.ica.datastructures;

public class HANStack<T> implements IHANStack<T> {
    private static class Node<T> {
        private final T value;
        private Node<T> next;

        private Node(T value) {
            this.value = value;
        }
    }

    private Node<T> top;
    private int size;

    @Override
    public void push(T value) {
        Node<T> n = new Node<>(value);
        n.next = top;
        top = n;
        size++;
    }

    @Override
    public T pop() {
        if (size == 0) {
            return null;
        }
        T value = top.value;
        top = top.next;
        size--;
        return value;
    }

    @Override
    public T peek() {
        if (size == 0) {
            return null;
        }
        return top.value;
    }
}

