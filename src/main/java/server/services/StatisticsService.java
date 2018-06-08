package server.services;

import server.data.Grid;
import server.data.Stat;
import server.data.Stats;
import server.data.responses.GlobalAndLocalStats;
import server.data.responses.MeanAndVariance;
import util.ErrorCodes;
import util.Log;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("stats")
public class StatisticsService {

    @GET
    @Path("nodes_position")
    @Produces("application/json")
    public Response getNodesPosition() {
        //Posizione dei vari nodi della città (e direi anche degli id)
        Log.i("Requested online nodes position");
        return Response.ok(Grid.getInstance().getNodes()).build();
    }


    @GET
    @Path("node")
    @Produces("application/json")
    public Response getStatsFromNode(@QueryParam("n") int n, @QueryParam("id") String nodeId) {
        //Ultime n statistiche con timestamp di uno specifico nodo edge
        Log.i("Request for last %s stats from node with id %s", n, nodeId);
        if (n < 1) {
            return Response.status(ErrorCodes.INCORRECT_STAT_NUMBER_REQUESTED).build();
        }
        List<Stat> nodeStats = Stats.getInstance().getLocals(nodeId, n);
        if (nodeStats == null) {
            //Il nodo non esiste
            return Response.status(ErrorCodes.UNAVAILABLE_NODE).build();
        } else {
            return Response.ok(nodeStats).build();
        }
    }

    @GET
    @Path("city")
    @Produces("application/json")
    public Response getStatsFromCity(@QueryParam("n") int n) {
        //Ultime n statistiche con timestamp globali e locali della città
        Log.i("Requested last %s globals and locals stats from city", n);
        if (n < 1) {
            return Response.status(ErrorCodes.INCORRECT_STAT_NUMBER_REQUESTED).build();
        }
        //Recupero le ultime n statistiche globali e locali
        List<Stat> globals = Stats.getInstance().getGlobals(n);
        Map<String, List<Stat>> locals = Stats.getInstance().getLocals(n);

        return Response
                .ok()
                .entity(new GlobalAndLocalStats(globals, locals))
                .build();
    }

    @GET
    @Path("node_summary")
    @Produces("application/json")
    public Response getSummaryFromNode(@QueryParam("n") int n, @QueryParam("id") String nodeId) {
        //Deviazione standard e media delle ultime n statistiche prodotte da uno specifico nodo edge
        Log.i("Request for summary of last %s stats from node with id %s", n, nodeId);
        if (n < 1) {
            return Response.status(ErrorCodes.INCORRECT_STAT_NUMBER_REQUESTED).build();
        }
        List<Stat> nodeStats = Stats.getInstance().getLocals(nodeId, n);
        if (nodeStats == null) {
            //Il nodo non esiste oppure ho 0 statistiche
            return Response.status(ErrorCodes.UNAVAILABLE_NODE).build();
        } else {
            double mean = calculateMean(nodeStats);
            double variance = calculateVariance(nodeStats, mean);
            return Response
                    .ok(new MeanAndVariance(mean, variance))
                    .build();
        }
    }

    @GET
    @Path("city_summary")
    @Produces("application/json")
    public Response getSummaryFromCity(@QueryParam("n") int n) {
        //Deviazione standard e media delle ultime n statistiche globali della città
        Log.i("Request for summary of last %s stats from the city", n);
        if (n < 1) {
            return Response.status(ErrorCodes.INCORRECT_STAT_NUMBER_REQUESTED).build();
        }
        List<Stat> globalStats = Stats.getInstance().getGlobals(n);
        if (globalStats == null) {
            //Non ci sono nodi
            return Response.status(ErrorCodes.ZERO_NODES_CONNECTED).build();
        } else {
            double mean = calculateMean(globalStats);
            double variance = calculateVariance(globalStats, mean);
            return Response
                    .ok(new MeanAndVariance(mean, variance))
                    .build();
        }
    }

    private double calculateMean(List<Stat> stats) {
        double mean = 0;
        if (stats.size() == 0) {
            return 0;
        }
        for (Stat s : stats) {
            mean += s.getValue();
        }
        return mean / stats.size();
    }

    private double calculateVariance(List<Stat> stats, double average) {
        double err = 0;
        if (stats.size() == 0) {
            return 0;
        }
        for (Stat s : stats) {
            err += Math.pow(average - s.getValue(), 2);
        }
        return Math.sqrt(err / stats.size());
    }

    @POST
    @Path("add_local")
    @Consumes("application/json")
    public Response addLocalStat(@QueryParam("id") String id, Stat stat) {
        Log.i("Request to add a local stat {id: %s, timestamp: %s, value: %s}",
                id, stat.getTimestamp(), stat.getValue());
        Stats.getInstance().addLocal(id, stat);
        return Response.ok().build();
    }

    @POST
    @Path("add_global")
    @Consumes("application/json")
    public Response addGlobalStat(Stat stat) {
        Log.i("Request to add a global stat {timestamp: %s, value: %s}", stat.getTimestamp(), stat.getValue());
        Stats.getInstance().addGlobal(stat);
        return Response.ok().build();
    }
}
