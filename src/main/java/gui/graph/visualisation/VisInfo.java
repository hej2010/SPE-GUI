package gui.graph.visualisation;

import gui.graph.data.GraphOperator;
import gui.graph.data.Operator;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class VisInfo {
    @Nonnull
    private final String fileName, className, methodName;
    @Nonnull
    public final VariableInfo variableInfo;

    VisInfo(@Nonnull String fileName, @Nonnull String className, @Nonnull String methodName, @Nonnull VariableInfo variableInfo) {
        this.fileName = fileName;
        this.className = className;
        this.methodName = methodName;
        this.variableInfo = new VariableInfo(variableInfo.variableName, variableInfo.calledWithVariableName, variableInfo.variableClass, variableInfo.variableData, variableInfo.operatorType, variableInfo.operatorName);
    }

    public static class VariableInfo {
        @Nullable
        private final String calledWithVariableName;
        @Nullable
        private String variableName, variableClass, variableData, operatorName;
        public final boolean savedInExistingVariable, savedInNewVariable;
        private Class<? extends GraphOperator> operatorType;

        VariableInfo(@Nullable String variableName, @Nullable String calledWithVariableName, @Nullable String variableClass, @Nullable String variableData,
                     @Nonnull Class<? extends GraphOperator> operatorType, @Nullable String operatorName) {
            this.variableName = variableName == null ? null : variableName.trim();
            this.calledWithVariableName = calledWithVariableName == null ? null : calledWithVariableName.trim();
            this.variableClass = variableClass == null ? null : variableClass.trim();
            boolean saved = variableName != null;
            this.savedInExistingVariable = saved && variableClass == null;
            this.savedInNewVariable = saved && variableClass != null;
            this.operatorType = operatorType;
            this.variableData = variableData;
            this.operatorName = operatorName;
        }

        VariableInfo(@Nullable String variableName, @Nullable String calledWithVariableName, @Nullable String variableClass, @Nullable String variableData, @Nullable String operatorName) {
            this(variableName, calledWithVariableName, variableClass, variableData, Operator.class, operatorName);
        }

        @Nullable
        public String getVariableData() {
            return variableData;
        }

        public void setVariableData(@Nullable String variableData) {
            this.variableData = variableData;
        }

        @Nullable
        public String getOperatorName() {
            return operatorName;
        }

        public void setOperatorName(@Nullable String operatorName) {
            this.operatorName = operatorName;
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
                    ", operatorName='" + operatorName + '\'' +
                    ", savedInExistingVariable=" + savedInExistingVariable +
                    ", savedInNewVariable=" + savedInNewVariable +
                    ", operatorType=" + operatorType +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VariableInfo)) return false;
            VariableInfo that = (VariableInfo) o;
            return savedInExistingVariable == that.savedInExistingVariable && savedInNewVariable == that.savedInNewVariable && Objects.equals(calledWithVariableName, that.calledWithVariableName)
                    && Objects.equals(variableName, that.variableName) && Objects.equals(variableClass, that.variableClass) && Objects.equals(variableData, that.variableData)
                    && Objects.equals(operatorName, that.operatorName) && operatorType.equals(that.operatorType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(calledWithVariableName, variableName, variableClass, variableData, operatorName, savedInExistingVariable, savedInNewVariable, operatorType);
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
    public static class VisInfo2 extends VisInfo {
        private final boolean firstInChain;
        private boolean lastInChain;

        VisInfo2(@NotNull String fileName, @NotNull String className, @NotNull String methodName, @NotNull VariableInfo variableInfo, boolean firstInChain, boolean lastInChain) {
            super(fileName, className, methodName, variableInfo);
            this.firstInChain = firstInChain;
            this.lastInChain = lastInChain;
        }

        public boolean isFirstInChain() {
            return firstInChain;
        }

        public boolean isLastInChain() {
            return lastInChain;
        }

        public void setLastInChain(boolean lastInChain) {
            this.lastInChain = lastInChain;
        }
    }
}
