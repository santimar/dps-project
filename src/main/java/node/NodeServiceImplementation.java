package node;

import com.messages.proto.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import util.Log;

public class NodeServiceImplementation extends NodeServiceGrpc.NodeServiceImplBase {
    private final Node node;
    private final Object queue = new Object(), acceptElections = new Object();
    private boolean coordinatorArrived = false, canBecameCoordinator = false;

    NodeServiceImplementation(Node me) {
        this.node = me;
    }

    @Override
    public void areYouCoordinator(final com.messages.proto.Node request, StreamObserver<Coordinator> responseObserver) {
        Log.d("New node connected '%s'", request.getId());
        try {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    node.newNode(new SimpleNode(request.getId(), request.getIp(), request.getPort()));
                }
            });
            t.start();
            //Faccio una join, così sono sicuro che se il nodo che si è aggiunto era il vecchio coordinatore,
            //Dopo la join mi troverò in stato di elezione o avrò già il nuovo coordinatore
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Rispondo se sono il coordinatore o no
        responseObserver.onNext(Coordinator.newBuilder()
                .setIsCoordinator(node.isCoordinator())
                .build()
        );

        responseObserver.onCompleted();
    }

    @Override
    public void sendLocalStats(Stats request, StreamObserver<Mean> responseObserver) {
        //Sono il master, ricevo delle statistiche da qualche nodo locale
        Log.d("Received {from: %s, mean: %s, ts: %s}", request.getId(), request.getMean(), request.getTimestamp());
        node.newStat(new NodeStat(request.getId(), request.getMean(), request.getTimestamp()));
        responseObserver.onNext(Mean.newBuilder().setMean(node.getLastGlobalMean()).build());
        responseObserver.onCompleted();
    }

    @Override
    public void startElection(Ack request, StreamObserver<Ack> responseObserver) {
        Log.i("Received request to start a new election by %s", request.getId());
        synchronized (acceptElections){
            if(!canBecameCoordinator){
                Log.w("Not everyone knows that i'm in the network, i can't became the coordinator");
                responseObserver.onError(Status.PERMISSION_DENIED.withDescription("Can't became coordinator yet").asException());
                return;
            }
        }
        responseObserver.onNext(
                Ack.newBuilder()
                        .setId(node.getId())
                        .build()
        );
        synchronized (queue) {
            if (node.getCurrentStatus() == Node.Status.NORMAL) {
                coordinatorArrived = false;
            }
        }
        //Faccio partire l'elezione a mia volta
        new Thread(new Runnable() {
            @Override
            public void run() {
                node.startElection();
            }
        }).start();
        //Non chiudo la connessione subito, ma solo quando il coordinatore mi contatterà
        synchronized (queue) {
            try {
                while (!coordinatorArrived) {
                    queue.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d("Sending onComplete of startElection to " + request.getId());
        responseObserver.onCompleted();
    }

    @Override
    public void newCoordinator(com.messages.proto.Node request, StreamObserver<Ack> responseObserver) {
        SimpleNode newCoordinator = new SimpleNode(request.getId(), request.getIp(), request.getPort());
        Log.i("Node with id: %s is the new coordinator", newCoordinator.getId());
        node.newCoordinatorFound(newCoordinator);
        responseObserver.onNext(Ack.newBuilder().build());
        Log.d("Sending onComplete of newCoordinator to " + request.getId());
        responseObserver.onCompleted();
        unlockQueue();
    }

    /*Se il coordinatore sono io, nessun coordinatore mi contatterà mai e non
     * chiuderò mai le connessioni in attesa della startElection
     * Perciò lo devo fare a mano
     */
    void unlockQueue() {
        Log.d("Unlocking queue");
        synchronized (queue) {
            coordinatorArrived = true;
            queue.notifyAll();
        }
    }

    void startAcceptElections(){
        synchronized (acceptElections){
            Log.i("Now everyone knows that i am in the network");
            canBecameCoordinator = true;
        }
    }
}
