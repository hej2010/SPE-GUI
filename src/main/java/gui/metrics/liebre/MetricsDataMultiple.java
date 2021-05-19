package gui.metrics.liebre;

import java.util.Map;
import java.util.Set;

public class MetricsDataMultiple extends MetricsData {
    private final Map<String, Double> integerMap;

    public MetricsDataMultiple(long timestamp, Map<String, Double> integerMap) {
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
