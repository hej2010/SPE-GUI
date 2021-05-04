package gui.metrics;

import java.util.Map;
import java.util.Set;

public class MetricsDataLiebre extends MetricsData {
    private final Map<String, Double> integerMap;

    public MetricsDataLiebre(long timestamp, Map<String, Double> integerMap) {
        super(timestamp);
        this.integerMap = integerMap;
    }

    public Number getValueFor(String field) {
        return integerMap.get(field);
    }

    public Set<String> getFields() {
        return integerMap.keySet();
    }

    @Override
    public String toString() {
        return "MetricsDataLiebre{" +
                "timestamp=" + timestamp +
                ", integerMap=" + integerMap +
                '}';
    }
}
