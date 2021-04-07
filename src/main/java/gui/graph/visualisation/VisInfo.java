package gui.graph.visualisation;

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
        this.variableInfo = new VariableInfo(variableInfo.variableName, variableInfo.calledWithVariableName, variableInfo.variableClass);
    }

    public static class VariableInfo {
        @Nullable
        private final String calledWithVariableName;
        @Nullable
        private String variableName, variableClass;
        public final boolean savedInExistingVariable, savedInNewVariable;

        VariableInfo(@Nullable String variableName, @Nullable String calledWithVariableName, @Nullable String variableClass) {
            this.variableName = variableName == null ? null : variableName.trim();
            this.calledWithVariableName = calledWithVariableName == null ? null : calledWithVariableName.trim();
            this.variableClass = variableClass == null ? null : variableClass.trim();
            this.savedInExistingVariable = variableClass == null;
            this.savedInNewVariable = !savedInExistingVariable;
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
}
