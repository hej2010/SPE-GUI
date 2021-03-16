package gui.graph.data;

import gui.graph.export.JsonExported;
import gui.spe.ParsedOperator;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class GraphOperator extends GraphObject implements JsonExported {
    protected String name;
    private int selectionIndex;
    private OnSelectionChangedListener listener;
    private final Map<String, ParsedOperator> operatorsMap;
    private ParsedOperator currentOperator;

    public GraphOperator(String name) {
        super();
        this.name = name;
        this.selectionIndex = -1;
        this.operatorsMap = new HashMap<>();
    }

    public boolean isSelected() {
        return selectionIndex >= 0;
    }

    public void setSelectedIndex(int index) {
        boolean old = isSelected();
        this.selectionIndex = index;
        onChanged(old, isSelected());
    }

    private void onChanged(boolean old, boolean selected) {
        if (listener != null) {
            listener.onChanged(old, selected);
        }
    }

    public void setOnSelectedChangeListener(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    public String getName() {
        return name;
    }

    @Override
    @Nonnull
    public String toString() {
        return (isSelected() ? "[" + (selectionIndex == 0 ? "FROM" : "TO") + "] " : "") + name + " (" + (currentOperator == null ? "" : (currentOperator.getOperatorName() + ", ")) + id + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphOperator that = (GraphOperator) o;
        return id == that.id;
    }

    @Nullable
    public ParsedOperator getCurrentOperator() {
        return currentOperator;
    }

    public void selectOperator(@Nullable String operatorName, List<ParsedOperator> operators) {
        this.currentOperator = getOrInitOperator(operatorName, operators);
    }

    private ParsedOperator getOrInitOperator(String operatorName, List<ParsedOperator> operators) {
        if (!operatorsMap.containsKey(operatorName)) {
            for (ParsedOperator op : operators) {
                if (op.getOperatorName().equals(operatorName)) {
                    operatorsMap.put(operatorName, op.clone());
                    break;
                }
            }
        }
        return operatorsMap.get(operatorName);
    }

    @Override
    public JSONObject toJsonObject() {
        JSONObject o = new JSONObject();
        o.put("name", name);
        o.put("ops", getOperators());
        return o;
    }

    private JSONObject getOperators() {
        Set<Map.Entry<String, ParsedOperator>> set = operatorsMap.entrySet();
        JSONObject o = new JSONObject();
        for (Map.Entry<String, ParsedOperator> e : set) {
            o.put(e.getKey(), e.getValue().toJsonObject());
        }
        return o;
    }

    @Override
    public Object fromJsonObject(JSONObject from) {
        return null;
    }
}
