package node;

import util.Log;

import java.util.ArrayList;
import java.util.List;

class NodeList {
    private List<SimpleNode> nodes = new ArrayList<>();

    synchronized void add(SimpleNode node) {
        for (SimpleNode n : nodes) {
            if (n.getId().equals(node.getId())) {
                Log.w("New node already in the list, updating");
                n.setIp(node.getIp());
                n.setPortForNodes(node.getPortForNodes());
                return;
            }
        }
        nodes.add(node);
    }

    synchronized void addAll(List<SimpleNode> list) {
        nodes.addAll(list);
    }

    synchronized void remove(SimpleNode node) {
        nodes.remove(node);
    }

    synchronized ArrayList<SimpleNode> getCopy() {
        return new ArrayList<>(nodes);
    }

    synchronized int size() {
        return nodes.size();
    }

}
