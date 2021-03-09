package org.example.data;

import java.util.Objects;

public abstract class GraphStream {
    private static int ID_COUNT = 0;
    private final int id;

    public GraphStream() {
        this.id = getId();
    }

    private synchronized int getId() {
        return ++ID_COUNT;
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

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
