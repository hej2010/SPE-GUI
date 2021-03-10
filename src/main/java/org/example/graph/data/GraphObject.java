package org.example.graph.data;

import java.util.Objects;

public abstract class GraphObject {
    private static int ID_COUNT = 0;
    protected final int id;

    public GraphObject() {
        this.id = getId();
    }

    private synchronized int getId() {
        return ++ID_COUNT;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }
}
