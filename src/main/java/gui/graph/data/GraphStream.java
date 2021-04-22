package gui.graph.data;

public abstract class GraphStream extends GraphObject {
    GraphOperator from, to;

    public GraphStream() {
        super();
    }

    public GraphOperator getFrom() {
        return from;
    }

    public GraphOperator getTo() {
        return to;
    }

    @Override
    public abstract String toString();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphStream that = (GraphStream) o;
        return id == that.id;
    }
}
