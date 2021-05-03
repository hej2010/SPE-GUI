package gui.metrics;

public class MetricsDataSingle extends MetricsData {
    public final int value;

    public MetricsDataSingle(long timestamp, int value) {
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
