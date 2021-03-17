package gui.graph.data;

import gui.graph.export.JsonExported;
import gui.spe.ParsedOperator;
import gui.spe.ParsedSPE;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class GraphOperator extends GraphObject implements JsonExported {
    protected String identifier;
    private int selectionIndex;
    private OnSelectionChangedListener listener;
    private final Map<String, ParsedOperator> operatorsMap;
    private ParsedOperator currentOperator;

    protected GraphOperator(String identifier, boolean addIdToIdentifier) {
        super();
        this.identifier = identifier + (addIdToIdentifier ? getId() : "");
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

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(@Nonnull String identifier) {
        this.identifier = identifier.trim().replace(" ", "");
    }

    @Override
    @Nonnull
    public String toString() {
        return (isSelected() ? "[" + (selectionIndex == 0 ? "FROM" : "TO") + "] " : "") + identifier + " (" + (currentOperator == null ? "" : (currentOperator.getOperatorName() + ", ")) + id + ")";
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
        o.put("name", identifier);
        o.put("ops", getOperatorsAsJson());
        int type;
        if (this instanceof SourceOperator) {
            type = ParsedOperator.TYPE_SOURCE_OPERATOR;
        } else if (this instanceof Operator) {
            type = ParsedOperator.TYPE_REGULAR_OPERATOR;
        } else {
            type = ParsedOperator.TYPE_SINK_OPERATOR;
        }
        o.put("type", type);
        return o;
    }

    private JSONObject getOperatorsAsJson() {
        Set<Map.Entry<String, ParsedOperator>> set = operatorsMap.entrySet();
        JSONObject o = new JSONObject();
        for (Map.Entry<String, ParsedOperator> e : set) {
            o.put(e.getKey(), e.getValue().toJsonObject());
        }
        return o;
    }

    public GraphOperator fromJsonObject(JSONObject from, ParsedSPE parsedSPE) {
        identifier = from.getString("name");
        operatorsMap.clear();
        JSONObject ops = from.getJSONObject("ops");
        for (String operatorName : ops.keySet()) {
            for (ParsedOperator op : parsedSPE.getOperators()) {
                if (operatorName.equals(op.getOperatorName())) {
                    operatorsMap.put(operatorName, ParsedOperator.fromJsonObject(ops.getJSONObject(operatorName), op.getDefinition()));
                    selectOperator(operatorName, parsedSPE.getOperators());
                }
            }

        }
        return this;
    }
}
