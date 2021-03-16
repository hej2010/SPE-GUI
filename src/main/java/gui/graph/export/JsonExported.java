package gui.graph.export;

import org.json.JSONObject;

public interface JsonExported {
    JSONObject toJsonObject();
    Object fromJsonObject(JSONObject from);
}
