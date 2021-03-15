package gui.spe;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

public class ParsedOperator implements Cloneable {
    private static final String PLACEHOLDER_IN = "@IN";
    private static final String PLACEHOLDER_OUT = "@OUT";
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

    @Override
    public ParsedOperator clone() {
        return new ParsedOperator(name, (Definition) this.definition.clone());
    }

    public static class Definition implements Cloneable {
        private final String codeBefore, codeAfter;
        private String codeMiddle;
        private final List<String> inputPlaceholders, outputPlaceholders;
        private final String identifierPlaceholder;
        private String identifier;
        private final boolean modifiable;

        Definition(String codeBefore, String codeMiddle, String codeAfter, @Nonnull List<String> inputPlaceholders, @Nonnull List<String> outputPlaceholders, String identifierPlaceholder) {
            this.codeBefore = codeBefore;
            this.codeMiddle = codeMiddle;
            this.codeAfter = codeAfter;
            this.inputPlaceholders = inputPlaceholders;
            this.outputPlaceholders = outputPlaceholders;
            this.identifierPlaceholder = identifierPlaceholder;
            this.identifier = identifierPlaceholder;
            this.modifiable = !codeMiddle.isEmpty();
        }

        @Override
        protected Object clone() {
            return new Definition(codeBefore, codeMiddle, codeAfter, new LinkedList<>(inputPlaceholders),
                    new LinkedList<>(outputPlaceholders), identifierPlaceholder);
        }

        public boolean isModifiable() {
            return modifiable;
        }

        public String getCodeBefore(boolean replace) {
            return getReplaced(replace, codeBefore);
        }

        public String getCodeMiddle(boolean replace) {
            return getReplaced(replace, codeMiddle);
        }

        private String getReplaced(boolean replace, String codeMiddle) {
            String middle = codeMiddle;
            if (replace) {
                for (int i = 1; i <= getInputCount(); i++) {
                    middle = middle.replace(PLACEHOLDER_IN + i, inputPlaceholders.get(i - 1));
                }
                for (int i = 1; i <= getOutputCount(); i++) {
                    middle = middle.replace(PLACEHOLDER_OUT + i, outputPlaceholders.get(i - 1));
                }
                middle = middle.replace(identifierPlaceholder, identifier);
            }
            return middle;
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
            inputPlaceholders.remove(pos);
            inputPlaceholders.add(pos, placeholder);
        }

        public void setOutputPlaceholders(int pos, String placeholder) {
            outputPlaceholders.remove(pos);
            outputPlaceholders.add(pos, placeholder);
        }

        public void setCodeMiddle(@Nonnull String codeMiddle) {
            if (modifiable) {
                this.codeMiddle = codeMiddle;
            }
        }

        @Nonnull
        public String getCode() {
            return getCodeBefore(true) + "\n" + getCodeMiddle(true) + "\n" + getCodeAfter();
        }

        @Nonnull
        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(@Nonnull String identifier) {
            this.identifier = identifier.trim().replace(" ", "");
        }
    }
}
