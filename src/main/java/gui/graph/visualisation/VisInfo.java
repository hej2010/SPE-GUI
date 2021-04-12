package gui.graph.visualisation;

import gui.graph.data.GraphOperator;
import gui.graph.data.Operator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VisInfo {
    @Nonnull
    private final String fileName, className, methodName;
    @Nonnull
    public final VariableInfo variableInfo;

    VisInfo(@Nonnull String fileName, @Nonnull String className, @Nonnull String methodName, @Nonnull VariableInfo variableInfo) {
        this.fileName = fileName;
        this.className = className;
        this.methodName = methodName;
        this.variableInfo = new VariableInfo(variableInfo.variableName, variableInfo.calledWithVariableName, variableInfo.variableClass, variableInfo.variableData, variableInfo.operatorType, variableInfo.operator);
    }

    public static class VariableInfo {
        @Nullable
        private final String calledWithVariableName;
        @Nullable
        private String variableName, variableClass, variableData, operator;
        public final boolean savedInExistingVariable, savedInNewVariable;
        private Class<? extends GraphOperator> operatorType;

        VariableInfo(@Nullable String variableName, @Nullable String calledWithVariableName, @Nullable String variableClass, @Nullable String variableData,
                     @Nonnull Class<? extends GraphOperator> operatorType, @Nullable String operator) {
            this.variableName = variableName == null ? null : variableName.trim();
            this.calledWithVariableName = calledWithVariableName == null ? null : calledWithVariableName.trim();
            this.variableClass = variableClass == null ? null : variableClass.trim();
            boolean saved = variableName != null;
            this.savedInExistingVariable = saved && variableClass == null;
            this.savedInNewVariable = saved && variableClass != null;
            this.operatorType = operatorType;
            this.variableData = variableData;
            this.operator = operator;
        }

        VariableInfo(@Nullable String variableName, @Nullable String calledWithVariableName, @Nullable String variableClass, @Nullable String variableData, @Nullable String operator) {
            this(variableName, calledWithVariableName, variableClass, variableData, Operator.class, operator);
        }

        @Nullable
        public String getVariableData() {
            return variableData;
        }

        public void setVariableData(@Nullable String variableData) {
            this.variableData = variableData;
        }

        @Nullable
        public String getOperator() {
            return operator;
        }

        public void setOperator(@Nullable String operator) {
            this.operator = operator;
        }

        public Class<? extends GraphOperator> getOperatorType() {
            return operatorType;
        }

        public void setOperatorType(@Nonnull Class<? extends GraphOperator> operatorType) {
            this.operatorType = operatorType;
        }

        @Nullable
        public String getCalledWithVariableName() {
            return calledWithVariableName;
        }

        @Nullable
        public String getVariableName() {
            return variableName;
        }

        public void setVariableName(@Nullable String variableName) {
            this.variableName = variableName == null ? null : variableName.trim();
        }

        @Nullable
        public String getVariableClass() {
            return variableClass;
        }

        public void setVariableClass(@Nullable String variableClass) {
            this.variableClass = variableClass == null ? null : variableClass.trim();
        }

        @Override
        public String toString() {
            return "VariableInfo{" +
                    "calledWithVariableName='" + calledWithVariableName + '\'' +
                    ", variableName='" + variableName + '\'' +
                    ", variableClass='" + variableClass + '\'' +
                    ", savedInExistingVariable=" + savedInExistingVariable +
                    ", savedInNewVariable=" + savedInNewVariable +
                    '}';
        }
    }

    @Nonnull
    public String getFileName() {
        return fileName;
    }

    @Nonnull
    public String getClassName() {
        return className;
    }

    @Nonnull
    public String getMethodName() {
        return methodName;
    }

    @Override
    public String toString() {
        return "VisInfo{" +
                "fileName='" + fileName + '\'' +
                ", className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", variableInfo=" + variableInfo +
                '}';
    }
}
