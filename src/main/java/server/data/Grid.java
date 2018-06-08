package server.data;

import util.Log;

import java.util.ArrayList;

public class Grid {
    private final static Grid instance = new Grid();
    //La prima cella è la [1][1] l'ultima è la [100][100]
    private final static int ROW_NUMBER = 100;
    private final static int COL_NUMBER = 100;
    private final ArrayList<EdgeNode> nodes = new ArrayList<>();

    private Grid() {
    }

    public static Grid getInstance() {
        return instance;
    }

    public synchronized Status addNode(EdgeNode node) {
        int row = node.getX(), col = node.getY();
        if (row > ROW_NUMBER || row < 1 || col > COL_NUMBER || col < 1) {
            Log.e("Invalid position [%s, %s]", row, col);
            return Status.INVALID_POSITION;
        }
        for (EdgeNode n : nodes) {
            if (distanceBetween(n.getX(), n.getY(), row, col) < 20) {
                Log.e("Position [%s, %s] already taken or too near to other nodes", row, col);
                return Status.TAKEN;
            }
            if (node.getId().equals(n.getId())) {
                Log.e("ID '%s' already taken", node.getId());
                return Status.INVALID_IDENTIFIER;
            }
        }
        //Devo inserire il nodo nella lista
        nodes.add(node);
        Log.d("Node added in [%s, %s]", row, col);
        return Status.OK;
    }

    public synchronized Status removeNode(String id) {
        for (EdgeNode n : nodes) {
            if (n.getId().equals(id)) {
                Log.d("Node '%s' found", id);
                nodes.remove(n);
                return Status.OK;
            }
        }
        Log.e("Node '%s' not found", id);
        return Status.NOT_FOUND;
    }

    public synchronized ArrayList<EdgeNode> getNodes() {
        return new ArrayList<>(nodes);
    }

    public synchronized EdgeNode findNearest(int x, int y) {
        EdgeNode currentNearest = null;
        int minDistance = Integer.MAX_VALUE, distance;
        Log.d("Point P is [%s, %s]", x, y);
        for (EdgeNode n : nodes) {
            distance = distanceBetween(x, y, n.getX(), n.getY());
            Log.d("Distance between P and node '%s' is %s", n.getId(), distance);
            if (distance < minDistance) {
                currentNearest = n;
                minDistance = distance;
            }
        }
        return currentNearest == null ? null : new EdgeNode(currentNearest);
    }

    public void printMap() {
        //faccio una copia della griglia, poi la stampo
        ArrayList<EdgeNode> nodesCopy;
        synchronized (this) {
            nodesCopy = new ArrayList<>(nodes);
        }
        for (int i = 1; i <= ROW_NUMBER; i++) {
            for (int j = 1; j <= COL_NUMBER; j++) {
                String color = Log.ANSI_GREEN;
                for (EdgeNode node : nodesCopy) {
                    int distance = distanceBetween(node.getX(), node.getY(), i, j);
                    if (distance == 0) {
                        color = Log.ANSI_RED;
                        break;
                    } else if (distance < 20) {
                        color = Log.ANSI_YELLOW;
                        break;
                    }
                }
                Log.print(color, "0");
            }
            System.out.println();
        }
    }

    private int distanceBetween(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }


    /**
     * OK = Operazione conclusa con successo
     * TAKEN = Posizione già occupata o troppo vicina a qualche altro nodo
     * INVALID_POSITION = Fuori dai limiti della griglia
     * INVALID_IDENTIFIER = ID già preso
     * NOT_FOUND = Il nodo cercato non esiste
     */
    public enum Status {
        OK, TAKEN, INVALID_POSITION, INVALID_IDENTIFIER, NOT_FOUND
    }
}
