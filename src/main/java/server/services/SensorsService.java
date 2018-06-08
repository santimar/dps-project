package server.services;

import sensor.NodeForSensor;
import server.data.EdgeNode;
import server.data.Grid;
import util.ErrorCodes;
import util.Log;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("sensor")
public class SensorsService {

    @GET
    @Produces("application/json")
    public Response getNearestNode(@QueryParam("x") int x, @QueryParam("y") int y) {
        Log.i("Received x: %s, Y: %s", x, y);
        if (x < 1 || x > 100 || y < 1 || y > 100) {
            Log.i("Incorrect params");
            return Response.status(ErrorCodes.INVALID_CELL_POSITION).build();
        }
        EdgeNode nearest = Grid.getInstance().findNearest(x, y);
        if (nearest == null) {
            Log.w("Zero nodes connected right now");
            return Response.status(ErrorCodes.ZERO_NODES_CONNECTED).build();
        } else {
            Log.i("Nearest node is %s", nearest.getId());
            NodeForSensor node = new NodeForSensor(nearest.getId(), nearest.getIp(), nearest.getPortForSensors());
            return Response.ok(node).build();
        }
    }
}
