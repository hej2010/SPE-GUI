package gui.graph.data;

import javax.annotation.Nonnull;
import java.util.Objects;

public class StringData {
    private String s;

    public StringData(@Nonnull String s) {
        this.s = s;
    }

    @Nonnull
    public String get() {
        return s;
    }

    public void set(@Nonnull String s) {
        this.s = s;
    }

    @Override
    public String toString() {
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StringData)) {
            return false;
        }
        return s.equals(o.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(s);
    }
}
