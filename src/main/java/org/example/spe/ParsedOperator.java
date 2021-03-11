package org.example.spe;

import javax.annotation.Nonnull;
import java.util.List;

public class ParsedOperator {
    private final String name;
    private final Definition definition;

    ParsedOperator(@Nonnull String name, @Nonnull Definition definition) {
        this.name = name;
        this.definition = definition;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public Definition getDefinition() {
        return definition;
    }

    static class Definition {
        private String codeBefore, codeMiddle, codeAfter;
        private final List<String> inputPlaceholders, outputPlaceholders;
        private final String identifierPlaceholder;

        Definition(String codeBefore, String codeMiddle, String codeAfter, @Nonnull List<String> inputPlaceholders, @Nonnull List<String> outputPlaceholders, String identifierPlaceholder) {
            this.codeBefore = codeBefore;
            this.codeMiddle = codeMiddle;
            this.codeAfter = codeAfter;
            this.inputPlaceholders = inputPlaceholders;
            this.outputPlaceholders = outputPlaceholders;
            this.identifierPlaceholder = identifierPlaceholder;
        }

        public String getCodeBefore() {
            return codeBefore;
        }

        public String getCodeMiddle() {
            return codeMiddle;
        }

        public String getCodeAfter() {
            return codeAfter;
        }

        @Nonnull
        public List<String> getInputPlaceholders() {
            return inputPlaceholders;
        }

        @Nonnull
        public List<String> getOutputPlaceholders() {
            return outputPlaceholders;
        }

        public String getIdentifierPlaceholder() {
            return identifierPlaceholder;
        }

        public void setCodeBefore(String codeBefore) {
            this.codeBefore = codeBefore;
        }

        public void setCodeMiddle(String codeMiddle) {
            this.codeMiddle = codeMiddle;
        }

        public void setCodeAfter(String codeAfter) {
            this.codeAfter = codeAfter;
        }
    }
}
