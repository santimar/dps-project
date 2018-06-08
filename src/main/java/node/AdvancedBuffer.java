package node;

import sensor.Measurement;
import util.Buffer;
import util.Log;

public class AdvancedBuffer extends Buffer<Measurement> {
    private NewStat listener;

    AdvancedBuffer(NewStat listener) {
        this.listener = listener;
    }

    @Override
    public synchronized void add(Measurement element) {
        //Log.i("Adding new measurement {id: %s, val: %s, ts: %s}", element.getId(), element.getValue(), element.getTimestamp());
        super.add(element);
        if (list.size() == 40) {
            Log.d("I have 40 measurements");
            listener.newStat(new NodeStat("", calculateMean(), System.currentTimeMillis()));
            list.removeAll(list.subList(0, 20));
        }
    }

    private double calculateMean() {
        double n = 0;
        for (Measurement measurement : list) {
            n += measurement.getValue();
        }
        return n / list.size();
    }


}
