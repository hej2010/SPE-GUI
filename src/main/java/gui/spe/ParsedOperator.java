package gui.spe;

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

    public static class Definition {
        private final String codeBefore, codeAfter;
        private String codeMiddle;
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

        public String getCodeBefore(boolean replace) {
            String before = codeBefore;
            if (replace) {
                for (String s : inputPlaceholders) {
                    for (int i = 1; i <= getInputCount(); i++) {
                        String toReplace = "@INPUT" + i;
                        if (toReplace.equals(s)) {
                            s = "";
                        }
                        before = before.replace(toReplace, s);
                    }
                }
                for (String s : outputPlaceholders) {
                    for (int i = 1; i <= getOutputCount(); i++) {
                        String toReplace = "@OUTPUT" + i;
                        if (toReplace.equals(s)) {
                            s = "";
                        }
                        before = before.replace(toReplace, s);
                    }
                }
            }
            System.out.println("Get code before: " + before);
            return before;
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

        public String getIdentifierPlaceholder() {
            return identifierPlaceholder;
        }

        public void setInputPlaceholders(int pos, String placeholder) {
            inputPlaceholders.remove(pos);
            inputPlaceholders.add(pos, placeholder);
        }

        public void setOutputPlaceholders(int pos, String placeholder) {
            outputPlaceholders.remove(pos);
            outputPlaceholders.add(pos, placeholder);
        }

        public void setCodeMiddle(String codeMiddle) {
            this.codeMiddle = codeMiddle;
        }

        public String getCode() {
            return getCodeBefore(true) + "\n" + codeMiddle + "\n" + getCodeAfter();
        }
    }
}
