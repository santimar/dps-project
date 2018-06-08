package util;

import java.util.ArrayList;
import java.util.List;

public class Buffer<T> {

    protected List<T> list = new ArrayList<>();

    public synchronized void add(T element) {
        list.add(element);
    }

    public synchronized void addElementsAtBottom(List<T> elements) {
        list.addAll(0, elements);
    }

    public synchronized void remove(T element) {
        list.remove(element);
    }

    public synchronized void removeAll(List<T> elements) {
        list.removeAll(elements);
    }

    public synchronized List<T> getCopy() {
        return new ArrayList<>(list);
    }

    public synchronized int size() {
        return list.size();
    }
}
