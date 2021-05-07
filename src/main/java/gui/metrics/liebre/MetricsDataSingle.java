package gui.metrics.liebre;

public class MetricsDataSingle extends MetricsData {
    public final double value;

    public MetricsDataSingle(long timestamp, double value) {
        super(timestamp);
        this.value = value;
    }

    @Override
    public String toString() {
        return "MetricsDataSingle{" +
                "timestamp=" + timestamp +
                ", value=" + value +
                '}';
    }
}
