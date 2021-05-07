package gui.metrics.graphite;

import gui.network.NetworkRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class GraphiteMetricsQuery {

    public static List<String> run() {
        return run("localhost", 80);
    }

    public static List<String> run(@Nonnull String host, int port) {
        JSONObject result;
        try {
            String response = new NetworkRequest("http://" + host + ":" + port + "/metrics/expand?query=*.*.*.*.*", null).run();
            result = new JSONObject(response);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
        return fromJson(result);
    }

    static List<String> fromJson(JSONObject obj) {
        List<String> results = new LinkedList<>();
        if (obj.has("results")) {
            JSONArray arr = obj.getJSONArray("results");
            for (int i = 0; i < arr.length(); i++) {
                results.add(arr.getString(i));
            }
        }
        return results;
    }
}
