package node;

import com.messages.proto.Ack;
import com.messages.proto.Measure;
import com.messages.proto.SensorServiceGrpc;
import io.grpc.stub.StreamObserver;
import sensor.Measurement;
import util.Log;

public class SensorServiceImplementation extends SensorServiceGrpc.SensorServiceImplBase {
    private final Node node;

    SensorServiceImplementation(Node me) {
        this.node = me;
    }

    @Override
    public StreamObserver<Measure> sendMeasure(final StreamObserver<Ack> responseObserver) {
        return new StreamObserver<Measure>() {
            @Override
            public void onNext(Measure measure) {
                //Ho ricevuto una measurement da un sensore
                Log.i("Received measurement from sensor %s", measure.getId());
                node.newMeasurement(new Measurement(measure.getId(), measure.getType(),
                        measure.getValue(), measure.getTimestamp()));
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e("SensorService sendMeasure OnError [%s]", throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                //Il nodo mi informa che non mi invierà più dati
                //rispondo con un ok
                Log.w("Sensor closed connection, it has a nearer node");
                responseObserver.onNext(Ack.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
    }
}
