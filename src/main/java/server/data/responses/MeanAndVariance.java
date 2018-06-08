package server.data.responses;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MeanAndVariance {
    private double mean, variance;

    public MeanAndVariance() {
    }

    public MeanAndVariance(double mean, double variance) {
        this.mean = mean;
        this.variance = variance;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getVariance() {
        return variance;
    }

    public void setVariance(double variance) {
        this.variance = variance;
    }
}
