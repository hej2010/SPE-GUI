package gui.graph.data;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

public class Operator extends GraphOperator {
    private final List<GraphStream> in;
    private final List<GraphStream> out;

    public Operator(String name) {
        super(name);
        in = new LinkedList<>();
        out = new LinkedList<>();
    }

    Operator addInputStream(@Nonnull GraphStream stream) {
        in.add(stream);
        return this;
    }

    Operator addOutputStream(@Nonnull GraphStream stream) {
        out.add(stream);
        return this;
    }

    public boolean hasInputStream() {
        return !in.isEmpty();
    }

    public boolean hasOutputStream() {
        return !out.isEmpty();
    }

    public List<GraphStream> getInputStreams() {
        return in;
    }

    public List<GraphStream> getOutputStreams() {
        return out;
    }
}
