package se.chalmers.datx05.metrics.graphite;

import org.json.JSONArray;
import org.json.JSONObject;

public class GraphiteMetricsQuery {
    private final JSONArray dataPoints;
    private final String target;

    private GraphiteMetricsQuery(JSONArray dataPoints, String target) {
        this.dataPoints = dataPoints;
        this.target = target;
    }

    static GraphiteMetricsQuery fromJson(JSONArray arr) {
        JSONObject o = arr.getJSONObject(0);
        JSONArray dataPoints = o.getJSONArray("datapoints");
        String target = o.getString("target");
        return new GraphiteMetricsQuery(dataPoints, target);
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
