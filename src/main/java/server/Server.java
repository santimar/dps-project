package server;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;
import server.data.Grid;
import util.Log;

import java.io.IOException;
import java.util.Scanner;

public class Server {
    private static final String HOST = "localhost";
    private static final int PORT = 1337;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServerFactory.create("http://" + HOST + ":" + PORT + "/");
        server.start();

        Log.i("Server running!");
        Log.i("Server started on: http://" + HOST + ":" + PORT);

        String c = "h";
        Scanner s = new Scanner(System.in);
        while (!c.equals("q") && !c.equals("quit")) {
            switch (c) {
                case "m":
                case "map":
                    Log.i("Current grid map");
                    Grid.getInstance()
                            .printMap();
                    break;
                case "h":
                case "help":
                    Log.i("List of available commands");
                    Log.i("m | map - Print the grid map");
                    Log.i("q | quit - Exit and stop server");
                    Log.i("h | help - Print this help");
                    break;
                default:
                    Log.i("Invalid command, try again");
                    break;
            }
            c = s.next();
        }
        Log.i("Stopping server");
        server.stop(0);
        Log.i("Server stopped");
    }
}
