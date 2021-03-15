package gui.graph.data;

import java.util.Objects;

public abstract class GraphObject {
    private static int ID_COUNT = 0;
    protected final int id;

    public GraphObject() {
        this.id = setId();
    }

    private synchronized int setId() {
        return ++ID_COUNT;
    }

    public int getId() {
        return id;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }
}
