package gui.spe;

import gui.graph.data.GraphOperator;
import gui.graph.export.ExportManager;
import gui.graph.export.JsonExported;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

public class ParsedOperator implements Cloneable, JsonExported {
    public static final int TYPE_SOURCE_OPERATOR = 0;
    public static final int TYPE_REGULAR_OPERATOR = 1;
    public static final int TYPE_SINK_OPERATOR = 2;
    private static final String PLACEHOLDER_IN = "@IN";
    private static final String PLACEHOLDER_OUT = "@OUT";
    private final String operatorName;
    private final Definition definition;
    private final int type;

    ParsedOperator(@Nonnull String operatorName, @Nonnull Definition definition, int type) {
        this.operatorName = operatorName;
        this.definition = definition;
        this.type = type;
    }

    public int getType() {
        return type;
    }

    @Nonnull
    public String getOperatorName() {
        return operatorName;
    }

    @Nonnull
    public Definition getDefinition() {
        return definition;
    }

    @Override
    public ParsedOperator clone() {
        return new ParsedOperator(operatorName, (Definition) this.definition.clone(), type);
    }

    public static class Definition implements Cloneable, JsonExported {
        private final String codeBefore, codeAfter;
        private String codeMiddle;
        private final List<String> inputPlaceholders, outputPlaceholders;
        private final String identifierPlaceholder, prevIdentifierPlaceholder;
        private final boolean modifiable;

        Definition(String codeBefore, String codeMiddle, String codeAfter, @Nonnull List<String> inputPlaceholders, @Nonnull List<String> outputPlaceholders,
                   String identifierPlaceholder, @Nullable String prevIdentifierPlaceholder) {
            this.codeBefore = codeBefore;
            this.codeMiddle = codeMiddle;
            this.codeAfter = codeAfter;
            this.inputPlaceholders = inputPlaceholders;
            this.outputPlaceholders = outputPlaceholders;
            this.identifierPlaceholder = identifierPlaceholder;
            this.prevIdentifierPlaceholder = prevIdentifierPlaceholder;
            this.modifiable = !codeMiddle.isEmpty();
        }

        @Override
        public Object clone() {
            return new Definition(codeBefore, codeMiddle, codeAfter, new LinkedList<>(inputPlaceholders),
                    new LinkedList<>(outputPlaceholders), identifierPlaceholder, prevIdentifierPlaceholder);
        }

        public boolean isModifiable() {
            return modifiable;
        }

        public String getCodeBefore(GraphOperator operator) {
            return getReplaced(operator, codeBefore);
        }

        public String getCodeMiddle(GraphOperator operator) {
            return getReplaced(operator, codeMiddle);
        }

        private String getReplaced(GraphOperator operator, String code) {
            String s = code;

            for (int i = 1; i <= getInputCount(); i++) {
                s = s.replace(PLACEHOLDER_IN + i, inputPlaceholders.get(i - 1));
            }
            for (int i = 1; i <= getOutputCount(); i++) {
                s = s.replace(PLACEHOLDER_OUT + i, outputPlaceholders.get(i - 1));
            }
            s = s.replace(identifierPlaceholder, operator.getIdentifier().get());
            if (operator.getPrevIdentifier() != null && prevIdentifierPlaceholder != null) {
                s = s.replace(prevIdentifierPlaceholder, operator.getPrevIdentifier().get());
            }
            return s;
        }

        public String getCodeAfter() {
            return codeAfter;
        }

        @Nonnull
        public List<String> getInputPlaceholders() {
            return inputPlaceholders;
        }

        public int getInputCount() {
            return inputPlaceholders.size();
        }

        public int getOutputCount() {
            return outputPlaceholders.size();
        }

        @Nonnull
        public List<String> getOutputPlaceholders() {
            return outputPlaceholders;
        }

        @Nonnull
        public String getIdentifierPlaceholder() {
            return identifierPlaceholder;
        }

        public void setInputPlaceholders(int pos, String placeholder) {
            if (inputPlaceholders.size() > pos) {
                inputPlaceholders.remove(pos);
            }
            inputPlaceholders.add(pos, placeholder);
        }

        public void setOutputPlaceholders(int pos, String placeholder) {
            if (outputPlaceholders.size() > pos) {
                outputPlaceholders.remove(pos);
            }
            outputPlaceholders.add(pos, placeholder);
        }

        public void setCodeMiddle(@Nonnull String codeMiddle) {
            if (modifiable) {
                this.codeMiddle = codeMiddle;
            }
        }

        @Nonnull
        public String getCode(GraphOperator operator) {
            return getCodeBefore(operator) + "\n" + getCodeMiddle(operator) + "\n" + getCodeAfter();
        }

        @Override
        public JSONObject toJsonObject() {
            JSONObject o = new JSONObject();
            o.put(ExportManager.EXPORT_MIDDLE, codeMiddle);
            o.put(ExportManager.EXPORT_IN, new JSONArray(inputPlaceholders));
            o.put(ExportManager.EXPORT_OUT, new JSONArray(outputPlaceholders));
            return o;
        }

        public Definition fromJsonObject(JSONObject from) {
            codeMiddle = from.getString(ExportManager.EXPORT_MIDDLE);
            setNewList(inputPlaceholders, from.getJSONArray(ExportManager.EXPORT_IN));
            setNewList(outputPlaceholders, from.getJSONArray(ExportManager.EXPORT_OUT));
            return this;
        }

        private void setNewList(List<String> list, JSONArray arr) {
            list.clear();
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.getString(i));
            }
        }
    }

    @Override
    public JSONObject toJsonObject() {
        JSONObject o = new JSONObject();
        o.put(ExportManager.EXPORT_NAME, operatorName);
        o.put(ExportManager.EXPORT_TYPE, type);
        o.put(ExportManager.EXPORT_DEFINITION, definition.toJsonObject());
        return o;
    }

    public static ParsedOperator fromJsonObject(JSONObject from, Definition definition) {
        return new ParsedOperator(from.getString(ExportManager.EXPORT_NAME), definition.fromJsonObject(from.getJSONObject(ExportManager.EXPORT_DEFINITION)), from.getInt(ExportManager.EXPORT_TYPE));
    }

}
