package gui.metrics.graphite;

import org.json.JSONArray;
import org.json.JSONObject;

public class GraphiteMetricsQueryOld {
    private final JSONArray dataPoints;
    private final String target;

    private GraphiteMetricsQueryOld(JSONArray dataPoints, String target) {
        this.dataPoints = dataPoints;
        this.target = target;
    }

    static GraphiteMetricsQueryOld fromJson(JSONArray arr) {
        JSONObject o = arr.getJSONObject(0);
        JSONArray dataPoints = o.getJSONArray("datapoints");
        String target = o.getString("target");
        return new GraphiteMetricsQueryOld(dataPoints, target);
    }

    public JSONArray getDataPoints() {
        return dataPoints;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "Response{" +
                "dataPoints=" + dataPoints.toString(2) +
                ", target='" + target + '\'' +
                '}';
    }
}
