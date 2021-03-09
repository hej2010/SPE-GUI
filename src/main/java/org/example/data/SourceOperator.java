package org.example.data;

import javax.annotation.Nonnull;
import java.util.*;

public class SourceOperator extends GraphOperator {
    private List<GraphStream> out;

    public SourceOperator(String name) {
        super(name);
        out = new LinkedList<>();
    }

    public boolean hasOutputStream() {
        return !out.isEmpty();
    }

    public List<GraphStream> getOutputStreams() {
        return out;
    }

    boolean addOutputStream(@Nonnull GraphStream stream) {
        return out.add(stream);
    }

}
