package server.data;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Stat {
    protected double value;
    protected long timestamp;

    public Stat() {
    }

    public Stat(double value, long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stat stat = (Stat) o;
        return Double.compare(stat.value, value) == 0 &&
                timestamp == stat.timestamp;
    }
}
