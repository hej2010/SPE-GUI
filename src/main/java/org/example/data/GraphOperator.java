package org.example.data;

import javax.annotation.Nonnull;
import java.util.Objects;

public abstract class GraphOperator {
    private static int ID_COUNT = 0;
    private final int id;
    protected String name;
    private int selectionIndex;
    private OnSelectionChangedListener listener;

    public GraphOperator(String name) {
        this.name = name;
        this.selectionIndex = -1;
        this.id = getId();
    }

    private synchronized int getId() {
        return ++ID_COUNT;
    }

    public boolean isSelected() {
        return selectionIndex >= 0;
    }

    public void setSelectedIndex(int index) {
        boolean old = isSelected();
        this.selectionIndex = index;
        onChanged(old, isSelected());
    }

    private void onChanged(boolean old, boolean selected) {
        if (listener != null) {
            listener.onChanged(old, selected);
        }
    }

    public void setOnSelectedChangeListener(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    @Override
    @Nonnull
    public String toString() {
        return (isSelected() ? "[" + selectionIndex + "] " : "") + name + " (" + id + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphOperator that = (GraphOperator) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
