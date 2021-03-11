package se.chalmers.datx05.graph.data;

public abstract class GraphStream extends GraphObject {

    public GraphStream() {
        super();
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
