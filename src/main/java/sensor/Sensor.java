package sensor;

import com.messages.proto.Ack;
import com.messages.proto.Measure;
import com.messages.proto.SensorServiceGrpc;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import util.ErrorCodes;
import util.Log;

import javax.ws.rs.core.MediaType;
import java.util.Random;

public class Sensor extends Thread implements SensorStream {
    private final int x, y;
    private final PM10Simulator pm10Simulator = new PM10Simulator(this);
    private WebResource r;
    private NodeForSensor node;
    private ManagedChannel channel;
    private SensorServiceGrpc.SensorServiceStub stub;
    private StreamObserver<Measure> stream;
    private volatile Status currentStatus = Status.DISCONNECTED;

    public static void main(String... args) {
        if (args.length < 3) {
            Log.println(Log.ANSI_RED, "Some parameters are missing");
            Log.println(Log.ANSI_RED, "java Sensor <n> <server_ip> <port>");
            return;
        }
        int sensorNumber = Integer.parseInt(args[0]);
        Log.d("Starting %s sensors", sensorNumber);
        for (int i = 0; i < sensorNumber; i++) {
            new Sensor(args[1], args[2]).start();
        }
    }

    private Sensor(String serverIp, String serverPort) {
        Random random = new Random();
        x = random.nextInt(100) + 1;
        y = random.nextInt(100) + 1;
        ClientConfig cfg = new DefaultClientConfig();
        cfg.getClasses().add(JacksonJsonProvider.class);
        final Client client = Client.create(cfg);

        r = client.resource("http://" + serverIp + ":" + serverPort + "/sensor");
        Log.d("Connecting to server http://%s:%s", serverIp, serverPort);
        Log.d("[%s] Sensor position is x:%s y:%s", pm10Simulator.getIdentifier(), x, y);

        requestNearestNode();
    }

    private void requestNearestNode() {
        synchronized (this) {
            if (currentStatus == Status.REQUESTING_NODE) {
                Log.w("[%s] Already in requesting node phase", pm10Simulator.getIdentifier());
                return;
            }
        }
        currentStatus = Status.REQUESTING_NODE;
        Log.d("[%s] Requesting nearest node", pm10Simulator.getIdentifier());
        ClientResponse response;
        try {
            response = r
                    .queryParam("x", String.valueOf(x))
                    .queryParam("y", String.valueOf(y))
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get(ClientResponse.class);
        } catch (ClientHandlerException ex) {
            currentStatus = Status.DISCONNECTED;
            Log.e("Unable to connect to server. Is it online?");
            return;
        }

        switch (response.getStatus()) {
            case 200:
                NodeForSensor nearest = response.getEntity(NodeForSensor.class);
                Log.d("[%s] Nearest node is %s@%s:%s", pm10Simulator.getIdentifier(),
                        nearest.getId(), nearest.getIp(), nearest.getPortForSensors());
                if (node == null) {
                    //Il sensore si è appena avviato o non ero connesso con nessuno
                    node = nearest;
                    openStreamToNode();
                } else if (!node.equals(nearest)) {
                    //Il nodo più vicino è cambiato, devo disconnettermi dal vecchio e collegarmi al nuovo
                    Log.i("[%s] New nearest node is changed (was: %s, now: %s)",
                            pm10Simulator.getIdentifier(), node.getId(), nearest.getId());
                    node = nearest;
                    openStreamToNode();
                } else {
                    //il nodo più vicino è lo stesso di prima, non devo fare nulla
                    Log.i("[%s] Nearest node is the same as before", pm10Simulator.getIdentifier());
                    currentStatus = Status.CONNECTED;
                }
                break;
            case ErrorCodes.ZERO_NODES_CONNECTED:
                Log.w("Zero nodes connected to the grid, unable to send data");
                currentStatus = Status.DISCONNECTED;
                break;
        }
    }

    private void openStreamToNode() {
        if (channel != null) {
            //ero già connesso a qualche nodo, provo a chiudere la connessione
            Log.i("[%s] I was already connected to a node, trying to close the connection",
                    pm10Simulator.getIdentifier());
            if (stream != null) {
                stream.onCompleted();
            }
            channel.shutdown();
        }
        Log.d("[%s] Opening stream to node %s@%s:%s", pm10Simulator.getIdentifier(),
                node.getId(), node.getIp(), node.getPortForSensors());
        channel = ManagedChannelBuilder.forAddress(node.getIp(), node.getPortForSensors())
                .usePlaintext(true)
                .build();
        stub = SensorServiceGrpc.newStub(channel);

        stream = stub.sendMeasure(new StreamObserver<Ack>() {
            @Override
            public void onNext(Ack ack) {
                //Il nodo non mi risponde mai
                //Mi invia un Ack solo per informarmi di aver ricevuto il messaggio di chiusura connessione
                //Lo gestisco nella onComplete()
            }

            @Override
            public void onError(Throwable throwable) {
                //Il nodo non è raggiungibile, ne devo richiedere uno nuovo
                Log.e("[%s] Current node is gone [%s]", pm10Simulator.getIdentifier(), throwable.getMessage());
                currentStatus = Status.DISCONNECTED;
                stream = null;
                node = null;
                requestNearestNode();
            }

            @Override
            public void onCompleted() {
                //Ho ricevuto una onComplete, il nodo mi ha confermato di aver chiuso la connessione
                //dopo che gliel'ho chiesto io
                Log.i("[%s] Node has correctly closed the connection", pm10Simulator.getIdentifier());
            }
        });
        currentStatus = Status.CONNECTED;
    }

    private void sendDataToNode(Measurement measurement) {
        if (currentStatus != Status.CONNECTED) {
            //Non sono connesso, questa misura è andata persa per sempre...
            Log.w("[%s] Not connected, measurement lost...", pm10Simulator.getIdentifier());
            requestNearestNode();
            return;
        }
        stream.onNext(Measure.newBuilder()
                .setId(measurement.getId())
                .setType(measurement.getType())
                .setValue(measurement.getValue())
                .setTimestamp(measurement.getTimestamp())
                .build());
    }


    @Override
    public void run() {
        pm10Simulator.start();

        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.d("[%s] 10 seconds passed, asking for new node", pm10Simulator.getIdentifier());
            requestNearestNode();
        }
    }

    @Override
    public void sendMeasurement(Measurement m) {
        Log.i("New measurement: id:%s time:%s value:%s", m.getId(), m.getTimestamp(), m.getValue());
        sendDataToNode(m);
    }

    private enum Status {DISCONNECTED, CONNECTED, REQUESTING_NODE}
}
