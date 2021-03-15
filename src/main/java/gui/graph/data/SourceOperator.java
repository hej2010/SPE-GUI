package gui.graph.data;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

public class SourceOperator extends GraphOperator {
    private final List<GraphStream> out;

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
