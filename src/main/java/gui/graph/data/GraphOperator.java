package gui.graph.data;

import gui.graph.export.ExportManager;
import gui.graph.export.JsonExported;
import gui.graph.visualisation.VisInfo;
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
    protected StringData identifier, identifier2, prevIdentifier;
    private int selectionIndex;
    private final Map<String, ParsedOperator> operatorsMap;
    private ParsedOperator currentOperator;
    private VisInfo visInfo;

    protected GraphOperator(String identifier, boolean addIdToIdentifier) {
        super();
        this.identifier = new StringData(identifier + (addIdToIdentifier ? getId() : ""));
        this.identifier2 = new StringData(this.identifier.get());
        this.prevIdentifier = null;
        this.selectionIndex = -1;
        this.operatorsMap = new HashMap<>();
        this.visInfo = null;
    }

    public void setVisInfo(VisInfo visInfo) {
        this.visInfo = visInfo;
    }

    public VisInfo getVisInfo() {
        return visInfo;
    }

    public boolean isSelected() {
        return selectionIndex >= 0;
    }

    public void setSelectedIndex(int index) {
        this.selectionIndex = index;
    }

    @Nonnull
    public StringData getIdentifier() {
        return identifier;
    }

    @Nullable
    public StringData getPrevIdentifier() {
        return prevIdentifier;
    }

    @Nonnull
    private String getIdentifierString() {
        return identifier == null ? "" : identifier.get();
    }

    @Nonnull
    private String getPrevIdentifierString() {
        return prevIdentifier == null ? "" : prevIdentifier.get();
    }

    public void setIdentifier(@Nonnull String identifier) {
        this.identifier.set(identifier.trim().replace(" ", ""));
    }

    public void setPrevIdentifier(@Nullable StringData stringData) {
        this.prevIdentifier = stringData;
    }

    public StringData getIdentifier2() {
        return identifier2;
    }

    public void setIdentifier2(String identifier2) {
        this.identifier2 = new StringData(identifier2);
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
        o.put(ExportManager.EXPORT_NAME, getIdentifierString());
        o.put(ExportManager.EXPORT_PREV_NAME, getPrevIdentifierString());
        o.put(ExportManager.EXPORT_OPS, getOperatorsAsJson());
        int type;
        if (this instanceof SourceOperator) {
            type = ParsedOperator.TYPE_SOURCE_OPERATOR;
        } else if (this instanceof Operator) {
            type = ParsedOperator.TYPE_REGULAR_OPERATOR;
        } else {
            type = ParsedOperator.TYPE_SINK_OPERATOR;
        }
        o.put(ExportManager.EXPORT_TYPE, type);
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
        identifier = new StringData(from.getString(ExportManager.EXPORT_NAME));
        prevIdentifier = new StringData(from.getString(ExportManager.EXPORT_PREV_NAME));
        operatorsMap.clear();
        JSONObject ops = from.getJSONObject(ExportManager.EXPORT_OPS);
        for (String operatorName : ops.keySet()) {
            for (ParsedOperator op : parsedSPE.getOperators()) {
                if (operatorName.equals(op.getOperatorName())) {
                    operatorsMap.put(operatorName, ParsedOperator.fromJsonObject(ops.getJSONObject(operatorName), (ParsedOperator.Definition) op.getDefinition().clone()));
                    selectOperator(operatorName, parsedSPE.getOperators());
                }
            }
        }
        return this;
    }
}
