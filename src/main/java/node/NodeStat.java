package node;

import server.data.Stat;

import java.util.Objects;

public class NodeStat extends Stat {
    private String id;

    NodeStat(String id, double value, long timestamp) {
        super(value, timestamp);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeStat stat = (NodeStat) o;
        return Objects.equals(id, stat.id)
                && Double.compare(stat.value, value) == 0
                && timestamp == stat.timestamp;
    }
}
