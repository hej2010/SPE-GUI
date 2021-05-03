package gui.metrics;

public abstract class MetricsData {
    public final long timestamp;

    public MetricsData(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "MetricsData{" +
                "timestamp=" + timestamp +
                '}';
    }
}
