package node;

import com.messages.proto.*;
import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import javafx.util.Pair;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import sensor.Measurement;
import server.data.EdgeNode;
import server.data.Stat;
import util.Buffer;
import util.Counter;
import util.ErrorCodes;
import util.Log;

import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Node implements NewStat {
    private static boolean goon = true;
    private final Object key = new Object();
    private final NodeList nodeList = new NodeList();
    private SimpleNode me, coordinator;
    private Buffer<NodeStat> meanBuffer = new Buffer<>();
    private volatile Status currentStatus = Status.NORMAL;
    private AdvancedBuffer measurementBuffer;
    private WebResource r;
    private Server serverForNodes, serverForSensors;
    private NodeServiceImplementation nodeServiceImplementation = new NodeServiceImplementation(this);
    private double lastGlobalMean = 0;
    private long lastGlobalTimestamp = 0;
    private final Thread coordinatorStatSender = new Thread(new Runnable() {
        @Override
        public void run() {
            Log.i("Stat sending to server enabled");
            while (goon) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Log.w("Stats sending stopped");
                    return;
                }
                sendStatsToServer();
            }
        }
    });
    private Map<String, Pair<ManagedChannel, NodeServiceGrpc.NodeServiceStub>> connections = new LinkedHashMap<>();

    public static void main(String... args) {
        if (args.length < 5) {
            Log.println(Log.ANSI_RED, "Some parameters are missing");
            Log.println(Log.ANSI_RED, "java Node <node_id> <node_port> <sensor_port> <server_ip> <port>");
            return;
        }
        new Node().start(args[0], args[1], args[2], args[3], args[4]);
    }

    private void start(String id, String nodePort, String sensorPort, String serverIp, String serverPort) {
        Random random = new Random();
        measurementBuffer = new AdvancedBuffer(this);
        ClientResponse response;
        me = new SimpleNode(id, "127.0.0.1", Integer.parseInt(nodePort));
        int x = random.nextInt(100) + 1, y = random.nextInt(100) + 1;

        //Per prima cosa, avvio i server per ricevere i messaggi dai nodi e dai sensori
        Log.d("Starting server for nodes on port %s", me.getPortForNodes());
        serverForNodes = ServerBuilder.forPort(me.getPortForNodes())
                .addService(nodeServiceImplementation)
                .build();
        Log.d("Starting server for sensors on port %s", sensorPort);
        serverForSensors = ServerBuilder.forPort(Integer.parseInt(sensorPort))
                .addService(new SensorServiceImplementation(this))
                .build();
        try {
            serverForNodes.start();
            serverForSensors.start();
        } catch (IOException e) {
            Log.e("Unable to bind with ports %s or %s, maybe they're already in use",
                    me.getPortForNodes(), sensorPort);
            return;
        }

        ClientConfig cfg = new DefaultClientConfig();
        cfg.getClasses().add(JacksonJsonProvider.class);
        final Client client = Client.create(cfg);

        r = client.resource("http://" + serverIp + ":" + serverPort);
        Log.d("Connecting to server http://%s:%s", serverIp, serverPort);

        int attempt = 0;
        boolean connected = false;
        while (attempt++ < 10 && !connected) {
            Log.d("Attempt nr. %s", attempt);
            Log.d("Random position generated [%s, %s]", x, y);
            EdgeNode myself = new EdgeNode(me.getId(), me.getIp(), Integer.parseInt(sensorPort), me.getPortForNodes(), x, y);
            Log.i("Node: {id: %s, ip: %s, np: %s, ns: %s, x: %s, y: %s}", myself.getId(), myself.getIp(),
                    myself.getPortForNodes(), myself.getPortForSensors(), myself.getX(), myself.getY());
            response = r
                    .path("/node")
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .entity(myself, MediaType.APPLICATION_JSON_TYPE)
                    .post(ClientResponse.class);
            switch (response.getStatus()) {
                case 200:
                    connected = true;
                    Log.g("Ok, i've been added to the grid");
                    List<SimpleNode> list = response.getEntity(new GenericType<List<SimpleNode>>() {
                    });
                    if (list.size() == 0) {
                        Log.d("It seem that i'm the only one in the grid");
                    } else {
                        Log.d("This is the list of nodes in the grid");
                        nodeList.addAll(list);
                        for (SimpleNode node : list)
                            Log.i("Node: {id: %s, ip: %s, nodePort: %s}", node.getId(),
                                    node.getIp(), node.getPortForNodes());
                    }
                    break;
                case ErrorCodes.POSITION_ALREADY_TAKEN:
                    Log.w("Position already taken");
                    x = random.nextInt(100) + 1;
                    y = random.nextInt(100) + 1;
                    break;
                case ErrorCodes.INVALID_NODE_IDENTIFIER:
                    Log.e("Incorrect node identifier or already taken");
                    return;
            }
        }
        if (!connected) {
            Log.e("Max attempts reached!");
            return;
        }

        //ok, ora sono connesso!
        if (nodeList.size() == 0) {
            //Il coordinatore sono io
            Log.g("I am the coordinator!");
            coordinator = me;
            coordinatorStatSender.start();
            nodeServiceImplementation.startAcceptElections();
        } else {
            Log.d("I'm not the coordinator");
            findCoordinator();
        }

        //questo thread serve per leggere input, appena legge qualcosa fa terminare il nodo
        new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                try {
                    br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                close();
            }
        }).start();

        try {
            serverForNodes.awaitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void findCoordinator() {
        final Counter replyCounter = new Counter(nodeList.size());
        for (final SimpleNode node : nodeList.getCopy()) {
            Log.d("Contacting node %s @ %s:%s", node.getId(), node.getIp(), node.getPortForNodes());

            NodeServiceGrpc.NodeServiceStub stub = getStub(node.getId());

            stub.areYouCoordinator(com.messages.proto.Node.newBuilder()
                    .setId(me.getId())
                    .setIp(me.getIp())
                    .setPort(me.getPortForNodes())
                    .build(), new StreamObserver<Coordinator>() {
                @Override
                public void onNext(Coordinator coordinator) {
                    Log.d("Received %s from %s ", coordinator.getIsCoordinator(), node.getId());
                    if(replyCounter.increase()){
                        nodeServiceImplementation.startAcceptElections();
                    }
                    if (coordinator.getIsCoordinator()) {
                        Log.d("Node %s is the coordinator", node.getId());
                        newCoordinatorFound(node);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e("Error with node %s, assuming is offline", node.getId());
                    if(replyCounter.increase()){
                        nodeServiceImplementation.startAcceptElections();
                    }
                    nodeList.remove(node);
                    if (nodeList.size() == 0) {
                        Log.w("All nodes from server list are down, i am the coordinator");
                        newCoordinatorFound(me);
                    }
                }

                @Override
                public void onCompleted() {

                }
            });
        }
    }

    private void sendStatsToCoordinator() {
        if (coordinator == null) {
            //Il coordinatore non cè
            //Può capitare se durante la findCoordinator() il coordinatore è crashato, ma
            // non se n'è ancora accorto nessuno
            Log.e("Coordinator unknown, starting election");
            startElection();
            return;
        }
        if (currentStatus == Status.ELECTION) {
            //Sono in fase di elezione, non invio i dati
            Log.w("Currently in election phase, unable to send data");
            return;
        }
        if (me.equals(coordinator)) {
            //Sono già il coordinatore, non devo inviare i dati a nessuno
            Log.d("I am the coordinator, i don't need to send data");
            return;
        }
        //Mi salvo il coordinatore che c'è in questo istante, poichè potrebbe essere offline
        //e prima di ricevere l'errore, qualcuno mi comunica che è cambiato, quindi lo eliminerei
        //dalla lista dei nodi attivi
        final SimpleNode currentCoordinator = coordinator;
        //Recupero tutte le statistiche da mandare al coordinatore
        final List<NodeStat> currentStats = meanBuffer.getCopy();
        Log.d("Contacting coordinator %s@%s:%s", currentCoordinator.getId(),
                currentCoordinator.getIp(), currentCoordinator.getPortForNodes());


        NodeServiceGrpc.NodeServiceStub stub = getStub(currentCoordinator.getId());

        for (int i = 0; i < currentStats.size(); i++) {
            final NodeStat stat = currentStats.get(i);
            Log.d("Message nr %s {id: %s, mean: %s}", i, stat.getId(), stat.getValue());
            stub.sendLocalStats(Stats.newBuilder()
                    .setId(stat.getId())
                    .setTimestamp(stat.getTimestamp())
                    .setMean(stat.getValue())
                    .build(), new StreamObserver<Mean>() {
                @Override
                public void onNext(Mean mean) {
                    Log.g("Received global mean %s", mean.getMean());
                    lastGlobalMean = mean.getMean();
                    //Il coordinatore mi ha risposto, quindi ha ricevuto la media e posso rimuoverla dal buffer
                    meanBuffer.remove(stat);
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e("Coordinator not available!");
                    //Elimino quel nodo dalla lista dei nodi attivi
                    nodeList.remove(currentCoordinator);

                    if (currentCoordinator.equals(coordinator)) {
                        //Il coordinatore non è cambiato da quando l'ho contattato,
                        startElection();
                    }
                }

                @Override
                public void onCompleted() {

                }
            });
        }

    }

    private void sendStatsToServer() {
        Log.i("Sending stats to server");
        //per prima cosa mi faccio una copia delle statistiche, così sono coerenti
        List<NodeStat> stats = meanBuffer.getCopy();

        if (stats.size() == 0) {
            Log.w("No data to send");
            return;
        }

        //poi calcolo la media globale (la media delle medie ricevute)
        lastGlobalMean = calculateGlobalMean(stats);
        lastGlobalTimestamp = System.currentTimeMillis();
        Log.i("New global mean %s calculated at %s", lastGlobalMean, lastGlobalTimestamp);

        try {
            r.path("stats/add_global")
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .entity(new Stat(lastGlobalMean, lastGlobalTimestamp), MediaType.APPLICATION_JSON_TYPE)
                    .post();
        } catch (ClientHandlerException ex) {
            Log.e("Unable to connect to server. Is it online?");
            return;
        }
        Log.g("Global stat %s sent to server", lastGlobalMean);

        //invio le singole statistiche locali
        for (NodeStat stat : stats) {
            r.path("stats/add_local")
                    .queryParam("id", stat.getId())
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .entity(new Stat(stat.getValue(), stat.getTimestamp()), MediaType.APPLICATION_JSON_TYPE)
                    .post(ClientResponse.class);
            Log.d("Sent local stat of %s {mean:%s, time:%s}", stat.getId(), stat.getValue(), stat.getTimestamp());
        }

        //Ho inviato tutte le statistiche in mio possesso al server, le posso togliere dal buffer
        meanBuffer.removeAll(stats);

    }

    double getLastGlobalMean() {
        return lastGlobalMean;
    }

    private double calculateGlobalMean(List<NodeStat> list) {
        double mean = 0;
        if (list.size() == 0) {
            return 0;
        }
        for (NodeStat stat : list) {
            mean += stat.getValue();
        }
        return mean / list.size();
    }

    private synchronized NodeServiceGrpc.NodeServiceStub getStub(String id) {
        if (!connections.containsKey(id)) {
            //Devo trovare il nodo
            for (SimpleNode n : nodeList.getCopy()) {
                if (n.getId().equals(id)) {
                    Log.d("Opening channel with %s", n.getId());
                    ManagedChannel channel = ManagedChannelBuilder.forAddress(n.getIp(), n.getPortForNodes())
                            .usePlaintext(true)
                            .build();

                    NodeServiceGrpc.NodeServiceStub stub = NodeServiceGrpc.newStub(channel);
                    connections.put(id, new Pair<>(channel, stub));
                    return stub;
                }
            }
        }
        return connections.get(id).getValue();
    }

    boolean isCoordinator() {
        //Qualcuno vuole sapere se sono il coordinatore
        //Se non lo so (fase di elezione) allora metto il richiedente in wait
        //e risponderò solo quando l'elezione sarà finita (e quindi so se sono il coordinatore)
        synchronized (key) {
            Log.d("Requesting if i am the coordinator...");
            if (currentStatus == Status.ELECTION) {
                Log.w("Election running, deferring response");
                try {
                    key.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.d("Ok, now i know if i am the coordinator");
            }
            Log.d("Replying if i am the coordinator");
            return me.equals(coordinator);
        }
    }

    synchronized void newNode(SimpleNode node) {
        /* Un nuovo nodo si è collegato quindi devo aggiungerlo alla lista
         * Controllo se ho già in memoria il suo id (quindi è un nodo che si è disconnesso
         * e poi riconnesso) e se quel nodo era il coordinatore, poichè in quel caso vuole dire
         * che il coordinatore si è disconnesso e riconnesso, senza che nessuno se ne sia accorto
         * e devo quindi avviare un elezione
         */
        Log.d("Node %s added", node.getId());
        nodeList.add(node);
        if (coordinator != null && coordinator.getId().equals(node.getId())) {
            Log.w("New node was the coordinator!");
            startElection();
        }
    }

    synchronized String getId() {
        return me.getId();
    }

    void startElection() {
        synchronized (Node.this) {
            if (currentStatus == Status.ELECTION) {
                Log.w("Already in election phase");
                return;
            }
            changeStatus(Status.ELECTION);
        }

        //Recupero tutti i nodi con id maggiore del mio
        final ArrayList<SimpleNode> biggerNodes = new ArrayList<>();
        for (SimpleNode node : nodeList.getCopy()) {
            if (node.getId().compareTo(me.getId()) > 0) {
                biggerNodes.add(node);
                Log.d("%s is bigger than %s, adding", node.getId(), me.getId());
            }
        }

        //Controllo che la lista non sia già vuota, nel caso sarei io il coordinatore
        if (biggerNodes.size() == 0) {
            Log.w("There aren't nodes with bigger id");
            newCoordinatorFound(me);
            return;
        }

        //L'elezione termina solo quando tutti i nodi mi rispondono (con una onComplete o con un errore)
        final Counter replyCounter = new Counter(biggerNodes.size());

        //Provo a contattare tutti i nodi con id maggiore del mio
        final List<SimpleNode> biggerNodesCopy = new ArrayList<>(biggerNodes);
        for (final SimpleNode node : biggerNodes) {
            NodeServiceGrpc.NodeServiceStub stub = getStub(node.getId());
            Log.d("Contacting node %s", node.getId());

            stub.startElection(Ack.newBuilder().setId(me.getId()).build(), new StreamObserver<Ack>() {
                @Override
                public void onNext(Ack ack) {
                    //Ho ricevuto una risposta, non sarò sicuramente io il coordinatore
                    Log.i("Node %s is active, so i won't be the coordinator", ack.getId());
                }

                @Override
                public void onError(Throwable throwable) {
                    if(Objects.equals(throwable.getMessage(), "PERMISSION_DENIED: Can't became coordinator yet")){
                        //Il nodo contattato non vuole diventare coordinatore
                        Log.e("Node %s don't want to became the coordinator", node.getId());
                    }else {
                        Log.e("Unable to reach node %s, assuming is offline", node.getId());
                        nodeList.remove(node);
                    }
                    synchronized (biggerNodesCopy) {
                        biggerNodesCopy.remove(node);
                    }
                    if (biggerNodesCopy.size() == 0) {
                        //Sono io il coordinatore, l'elezione non terminerà quando tutti mi rispondono,
                        // ma solo quando io avrò informato tutti
                        replyCounter.invalidate();
                        newCoordinatorFound(me);
                    }
                    if (replyCounter.increase()) {
                        changeStatus(Status.NORMAL);
                    }
                }

                @Override
                public void onCompleted() {
                    Log.i("%s ended election", node.getId());
                    if (replyCounter.increase()) {
                        changeStatus(Status.NORMAL);
                    }
                }
            });
        }
    }

    void newCoordinatorFound(SimpleNode newCoordinator) {
        if (newCoordinator.equals(me)) {
            //Il nuovo coordinatore sono io, devo informare gli altri
            Log.g("I am the new coordinator!");
            coordinator = me;
            //Ho scoperto che il coordinatore sono io, farò terminare l'elezione quando tutti mi rispondono
            List<SimpleNode> copy = nodeList.getCopy();
            if (copy.size() == 0) {
                //Non c'è nessuno da informare
                changeStatus(Status.NORMAL);
            }
            final Counter replies = new Counter(copy.size());
            for (final SimpleNode n : copy) {
                Log.d("Contacting node %s", n.getId());
                NodeServiceGrpc.NodeServiceStub stub = getStub(n.getId());
                stub.newCoordinator(com.messages.proto.Node.newBuilder()
                        .setId(me.getId())
                        .setIp(me.getIp())
                        .setPort(me.getPortForNodes())
                        .build(), new StreamObserver<Ack>() {
                    @Override
                    public void onNext(Ack ack) {
                        Log.d("Node %s now knows that i'm the new coordinator", n.getId());
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("Error while contacting node %s (assuming is offline)", n.getId());
                        nodeList.remove(n);
                        if (replies.increase()) {
                            changeStatus(Status.NORMAL);
                            nodeServiceImplementation.unlockQueue();
                        }
                    }

                    @Override
                    public void onCompleted() {
                        if (replies.increase()) {
                            changeStatus(Status.NORMAL);
                            nodeServiceImplementation.unlockQueue();
                        }
                    }
                });
            }

            //Avvio il Thread che manda le statistiche al server ogni 5 secondi
            if (!coordinatorStatSender.isAlive()) {
                coordinatorStatSender.start();
            }
        } else {
            //Il coordinatore non sono io, quindi lo cerco nella lista

            for (SimpleNode n : nodeList.getCopy()) {
                if (n.equals(newCoordinator)) {
                    Log.i("The coordinator is %s", n.getId());
                    coordinator = n;
                    break;
                }
            }
        }
    }

    private synchronized void changeStatus(Status status) {
        currentStatus = status;
        if (currentStatus == Status.ELECTION) {
            Log.i("=====Election started=====");
        } else {
            Log.i("=====Election ended=====");
            synchronized (key) {
                //È possibile ci siano Thread che stanno aspettando di sapere se io sono il coordinatore o no
                //A questo punto lo so, quindi posso risvegliarli
                Log.d("Notifying waiters...!");
                key.notifyAll();
            }
        }
    }

    synchronized Status getCurrentStatus() {
        return currentStatus;
    }

    @Override
    public synchronized void newStat(NodeStat stat) {
        //La aggiungo al buffer delle medie
        Log.d("New stat {mean: %s, ts: %s} %s", stat.getValue(), stat.getTimestamp(),
                (stat.getId().equals("") ? "computed by myself" : "received from " + stat.getId())
        );
        if (stat.getId().equals("")) {
            //La media mi è stata inviata dal buffer, quindi ci metto il mio id
            stat.setId(me.getId());
        }
        if (stat.getTimestamp() > lastGlobalTimestamp) {
            meanBuffer.add(stat);
            sendStatsToCoordinator();
        } else {
            Log.w("Stats is too old, dropping");
        }
    }

    synchronized void newMeasurement(Measurement m) {
        measurementBuffer.add(m);
    }

    private void close() {
        Log.i("Stopping servers");
        serverForNodes.shutdown();
        serverForSensors.shutdownNow();
        Log.i("Closing all connections");
        for (Pair<ManagedChannel, NodeServiceGrpc.NodeServiceStub> pair : connections.values()) {
            pair.getKey().shutdown();
        }
        Log.g("Connections closed");

        //dico al server di rimuovermi dalla lista
        r.path("node")
                .entity(me, MediaType.APPLICATION_JSON_TYPE)
                .delete();
        goon = false;
    }

    enum Status {ELECTION, NORMAL}
}
