package util;

public class Counter {
    private int count = 0, goal;
    private boolean valid = true;

    public Counter(int goal) {
        this.goal = goal;
    }

    /**
     * Incrementa il contatore
     *
     * @return true se il contatore ha raggiunto l'obbiettivo, falso altrimenti
     */
    public synchronized boolean increase() {
        if (count < goal) {
            count++;
        }
        return valid && count == goal;
    }

    /**
     * Invalida il contatore, che non restituira' mai true
     */
    public synchronized void invalidate() {
        valid = false;
    }
}
