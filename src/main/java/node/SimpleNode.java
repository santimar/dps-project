package node;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class SimpleNode {
    private String id;
    private String ip;
    private int portForNodes;

    public SimpleNode() {
    }

    public SimpleNode(String id, String ip, int portForNodes) {
        this.id = id;
        this.ip = ip;
        this.portForNodes = portForNodes;
    }

    //Getter and setter
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

    public int getPortForNodes() {
        return portForNodes;
    }

    public void setPortForNodes(int portForNodes) {
        this.portForNodes = portForNodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleNode that = (SimpleNode) o;
        return portForNodes == that.portForNodes &&
                Objects.equals(id, that.id) &&
                Objects.equals(ip, that.ip);
    }
}
