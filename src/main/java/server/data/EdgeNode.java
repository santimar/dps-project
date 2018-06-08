package server.data;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
//@JsonIgnoreProperties(ignoreUnknown=true)
public class EdgeNode {
    private String id;
    private String ip;
    private int portForSensors, portForNodes;
    private int x;
    private int y;

    public EdgeNode() {
    }

    public EdgeNode(String id, String ip, int portForSensors, int portForNodes, int x, int y) {
        this.id = id;
        this.ip = ip;
        this.portForSensors = portForSensors;
        this.portForNodes = portForNodes;
        this.x = x;
        this.y = y;
    }

    public EdgeNode(EdgeNode node) {
        this.id = node.id;
        this.ip = node.ip;
        this.portForSensors = node.portForSensors;
        this.portForNodes = node.portForNodes;
        this.x = node.x;
        this.y = node.y;
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

    public int getPortForSensors() {
        return portForSensors;
    }

    public void setPortForSensors(int portForSensors) {
        this.portForSensors = portForSensors;
    }

    public int getPortForNodes() {
        return portForNodes;
    }

    public void setPortForNodes(int portForNodes) {
        this.portForNodes = portForNodes;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
