package org.example.testing;

import org.example.metrics.graphite.GraphiteRenderQuery;
import org.example.network.NetworkRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GraphiteTest {
    public static void main(String[] args) {
        Map<String, String> map = new HashMap<>();
        map.put("format", "json");
        map.put("target", "liebre.name.I1.EXEC.count");
        map.put("from", "-148h");
        map.put("until", "");
        GraphiteRenderQuery q = GraphiteRenderQuery.run(map);
        System.out.println(q);
    }

}
