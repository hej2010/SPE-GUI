package se.chalmers.datx05.graph.data;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

public class SinkOperator extends GraphOperator {
    private final List<GraphStream> in;

    public SinkOperator(String name) {
        super(name);
        in = new LinkedList<>();
    }

    public boolean hasInputStream() {
        return !in.isEmpty();
    }

    public List<GraphStream> getInputStreams() {
        return in;
    }

    boolean addInputStream(@Nonnull GraphStream stream) {
        return in.add(stream);
    }

}
