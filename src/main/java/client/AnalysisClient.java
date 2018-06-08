package client;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import server.data.EdgeNode;
import server.data.Stat;
import server.data.responses.GlobalAndLocalStats;
import server.data.responses.MeanAndVariance;
import util.ErrorCodes;
import util.Log;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class AnalysisClient {
    private WebResource r;
    private Scanner scanner = new Scanner(System.in);

    public static void main(String... args) {
        if (args.length < 2) {
            Log.println(Log.ANSI_RED, "Some parameters are missing");
            Log.println(Log.ANSI_RED, "java AnalysisClient <server_ip> <port>");
            return;
        }
        new AnalysisClient().start(args[0], args[1]);
    }

    private void start(String host, String port) {
        ClientConfig cfg = new DefaultClientConfig();
        cfg.getClasses().add(JacksonJsonProvider.class);
        Client client = Client.create(cfg);

        r = client.resource("http://" + host + ":" + port + "/stats");

        String c = "h";
        Log.println("Hi Analyst, press the number of the command you want to use");
        while (!c.equals("q") && !c.equals("quit")) {
            switch (c) {
                case "h":
                    Log.println("1 - List active nodes");
                    Log.println("2 - Last <N> stats from a specific node");
                    Log.println("3 - Last <N> global and local city stats with timestamp");
                    Log.println("4 - Mean and Variance of last <N> stats of a single edge node");
                    Log.println("5 - Mean and Variance of last <N> globals stats");
                    Log.println("h - Print this help");
                    Log.println("q - quit");
                    break;
                case "1":
                    listActiveNodes();
                    break;
                case "2":
                    lastStatsFromSingleNode();
                    break;
                case "3":
                    lastGlobalsAndLocalsStats();
                    break;
                case "4":
                    meanAndVarianceFromSingleNode();
                    break;
                case "5":
                    meanAndVarianceFromCity();
                    break;
                default:
                    Log.println("Invalid command, try again");
            }
            Log.println("Insert command");
            c = scanner.next();
        }
    }

    private void listActiveNodes() {
        Log.i("Requesting online nodes...");
        List<EdgeNode> response;
        try {
            response = r.path("/nodes_position")
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(new GenericType<List<EdgeNode>>() {
                    });
        } catch (Exception e) {
            Log.e("Server not online!");
            return;
        }
        Log.i("%s nodes online", response.size());
        for (EdgeNode node : response)
            Log.i("Node: {id: %s, x: %s, y: %s}", node.getId(),
                    node.getX(), node.getY());
    }

    private void lastStatsFromSingleNode() {
        Log.print("Insert node id: ");
        String id = scanner.next();
        Log.print("Insert number of stats: ");
        String n = scanner.next();
        if (!isNumber(n)) {
            return;
        }
        Log.d("Starting request to the server");

        ClientResponse response;
        try {
            response = r
                    .path("/node")
                    .queryParam("id", id)
                    .queryParam("n", n)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(ClientResponse.class);
        } catch (Exception e) {
            Log.e("Server not online!");
            return;
        }
        switch (response.getStatus()) {
            case 200:
                List<Stat> stats = response.getEntity(new GenericType<List<Stat>>() {
                });
                if (Integer.parseInt(n) > stats.size()) {
                    Log.d("You requested %s stats but only %s are available", n, stats.size());
                }
                for (Stat s : stats)
                    Log.i("Response: {timestamp: %s, value: %s}", s.getTimestamp(), s.getValue());
                break;
            case ErrorCodes.INCORRECT_STAT_NUMBER_REQUESTED:
                Log.println(Log.ANSI_RED, "Stats number must be a positive number");
                break;
            case ErrorCodes.UNAVAILABLE_NODE:
                Log.println(Log.ANSI_RED, "Node with given id doesn't exists");
                break;
        }
    }

    private void lastGlobalsAndLocalsStats() {
        Log.print("Insert number of stats: ");
        String n = scanner.next();
        if (!isNumber(n)) {
            return;
        }
        Log.d("Starting request to the server");
        ClientResponse response;
        try {
            response = r
                    .path("/city")
                    .queryParam("n", n)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(ClientResponse.class);
        } catch (Exception e) {
            Log.e("Server not online!");
            return;
        }
        switch (response.getStatus()) {
            case 200:
                GlobalAndLocalStats globalAndLocalStats = response.getEntity(GlobalAndLocalStats.class);
                List<Stat> globals = globalAndLocalStats.getGlobals();
                Map<String, List<Stat>> locals = globalAndLocalStats.getLocals();

                if (globals == null || locals == null) {
                    Log.println(Log.ANSI_RED, "No stats on the server");
                    return;
                }

                for (Stat s : globals)
                    Log.i("global: {timestamp: %s, value: %s}", s.getTimestamp(), s.getValue());
                for (String key : locals.keySet()) {
                    for (Stat s : locals.get(key)) {
                        Log.i("%s: {timestamp: %s, value: %s}", key, s.getTimestamp(), s.getValue());
                    }
                }
                break;
            case ErrorCodes.INCORRECT_STAT_NUMBER_REQUESTED:
                Log.println(Log.ANSI_RED, "Stats number must be a positive number");
                break;
        }
    }

    private void meanAndVarianceFromSingleNode() {
        Log.print("Insert node id: ");
        String id = scanner.next();
        Log.print("Insert number of stats: ");
        String n = scanner.next();
        if (!isNumber(n)) {
            return;
        }
        Log.d("Starting request to the server");

        ClientResponse response;
        try {
            response = r
                    .path("/node_summary")
                    .queryParam("id", id)
                    .queryParam("n", n)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(ClientResponse.class);
        } catch (Exception e) {
            Log.e("Server not online!");
            return;
        }
        switch (response.getStatus()) {
            case 200:
                MeanAndVariance meanAndVariance = response.getEntity(MeanAndVariance.class);
                Log.i("Mean: %s, Variance: %s", meanAndVariance.getMean(), meanAndVariance.getVariance());
                break;
            case ErrorCodes.INCORRECT_STAT_NUMBER_REQUESTED:
                Log.println(Log.ANSI_RED, "Stats number must be a positive number");
                break;
            case ErrorCodes.UNAVAILABLE_NODE:
                Log.println(Log.ANSI_RED, "Node with given id doesn't exists");
                break;
        }
    }

    private void meanAndVarianceFromCity() {
        Log.print("Insert number of stats: ");
        String n = scanner.next();
        if (!isNumber(n)) {
            return;
        }
        Log.d("Starting request to the server");

        ClientResponse response;
        try {
            response = r
                    .path("/city_summary")
                    .queryParam("n", n)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(ClientResponse.class);
        } catch (Exception e) {
            Log.e("Server not online!");
            return;
        }
        switch (response.getStatus()) {
            case 200:
                MeanAndVariance meanAndVariance = response.getEntity(MeanAndVariance.class);
                Log.i("Global Mean: %s, Variance: %s", meanAndVariance.getMean(), meanAndVariance.getVariance());
                break;
            case ErrorCodes.INCORRECT_STAT_NUMBER_REQUESTED:
                Log.println(Log.ANSI_RED, "Stats number must be a positive number");
                break;
            case ErrorCodes.ZERO_NODES_CONNECTED:
                Log.println(Log.ANSI_RED, "There are 0 stats on the server, unable to compute mean and variance");
                break;
        }
    }

    private boolean isNumber(String number) {
        try {
            Integer.parseInt(number);
        } catch (NumberFormatException e) {
            Log.println(Log.ANSI_RED, "Typed value is not a number");
            return false;
        }
        return true;
    }
}
