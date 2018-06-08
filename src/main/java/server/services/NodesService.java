package server.services;

import node.SimpleNode;
import server.data.EdgeNode;
import server.data.Grid;
import util.ErrorCodes;
import util.Log;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Path("node")
public class NodesService {

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response add(EdgeNode node) {
        Log.i("ADD");
        Log.i("Received {id: %s, ip: %s, nodePort: %s, sensorPort: %s, x: %s, y:%s}", node.getId(),
                node.getIp(), node.getPortForNodes(), node.getPortForSensors(), node.getX(), node.getY());

        Log.d("Trying to add node in the grid");
        Grid grid = Grid.getInstance();
        Grid.Status operationStatus = grid.addNode(node);
        switch (operationStatus) {
            case OK:
                List<EdgeNode> activeNodes = grid.getNodes();
                activeNodes.remove(node);
                //Li mescolo per fare in modo di non avere sempre il coordinatore al primo posto
                Collections.shuffle(activeNodes);
                //Restituisco solo i dati che servono veramente ai nodi
                List<SimpleNode> nodes = new ArrayList<>();
                for (EdgeNode n : activeNodes) {
                    nodes.add(new SimpleNode(n.getId(), n.getIp(), n.getPortForNodes()));
                }
                return Response.ok(nodes).build();
            case TAKEN:
                return Response.status(ErrorCodes.POSITION_ALREADY_TAKEN).build();
            case INVALID_IDENTIFIER:
                return Response.status(ErrorCodes.INVALID_NODE_IDENTIFIER).build();
            default:
            case INVALID_POSITION:
                return Response.status(ErrorCodes.INVALID_CELL_POSITION).build();
        }
    }

    @DELETE
    @Consumes("application/json")
    @Produces("application/json")
    public Response remove(SimpleNode node) {
        Log.i("REMOVE");
        Log.i("Received id: " + node.getId());
        Grid.Status result = Grid.getInstance().removeNode(node.getId());
        if (result == Grid.Status.OK) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
