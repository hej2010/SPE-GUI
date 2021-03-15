package gui.metrics.graphite;

import gui.network.NetworkRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GraphiteRenderQuery {
    private final List<RenderDatapoint> dataPoints;
    private final String target;

    private GraphiteRenderQuery(List<RenderDatapoint> dataPoints, String target) {
        this.dataPoints = dataPoints;
        this.target = target;
    }

    public static GraphiteRenderQuery run(Map<String, String> map) {
        return run("localhost", 80, map);
    }

    public static GraphiteRenderQuery run(@Nonnull String host, int port, Map<String, String> map) {
        JSONArray arr;
        try {
            String response = new NetworkRequest("http://" + host + ":" + port + "/render", map).run();
            arr = new JSONArray(response);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
        return fromJson(arr);
    }

    static GraphiteRenderQuery fromJson(JSONArray arr) {
        JSONObject o;
        if (arr != null) {
            if (arr.isEmpty()) {
                return new GraphiteRenderQuery(new LinkedList<>(), null);
            }
            o = arr.getJSONObject(0);
        } else {
            System.out.println("GraphiteRenderQuery - fromJson: empty");
            return null;
        }
        JSONArray dataPoints = o.getJSONArray("datapoints");
        String target = o.getString("target");
        return new GraphiteRenderQuery(RenderDatapoint.fromJson(dataPoints), target);
    }

    public List<RenderDatapoint> getDataPoints() {
        return dataPoints;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "Response{" +
                "dataPoints=" + dataPoints.size() +
                ", target='" + target + '\'' +
                '}';
    }
}
