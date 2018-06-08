package sensor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class NodeForSensor {
    private String id;
    private String ip;
    private int portForSensors;

    public NodeForSensor() {
    }

    public NodeForSensor(String id, String ip, int portForSensors) {
        this.id = id;
        this.ip = ip;
        this.portForSensors = portForSensors;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPortForSensors() {
        return portForSensors;
    }

    public void setPortForSensors(int portForSensors) {
        this.portForSensors = portForSensors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeForSensor node = (NodeForSensor) o;
        return portForSensors == node.portForSensors &&
                Objects.equals(id, node.id) &&
                Objects.equals(ip, node.ip);
    }
}
