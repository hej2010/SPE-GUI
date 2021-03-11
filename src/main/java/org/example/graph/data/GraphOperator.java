package org.example.graph.data;

import javax.annotation.Nonnull;

public abstract class GraphOperator extends GraphObject {
    protected String name;
    private int selectionIndex;
    private OnSelectionChangedListener listener;

    public GraphOperator(String name) {
        super();
        this.name = name;
        this.selectionIndex = -1;
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

    public String getName() {
        return name;
    }

    @Override
    @Nonnull
    public String toString() {
        return (isSelected() ? "[" + (selectionIndex == 0 ? "FROM" : "TO") + "] " : "") + name + " (" + id + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphOperator that = (GraphOperator) o;
        return id == that.id;
    }

    public enum Type {
        Map("Map"),
        FlatMap("FlatMap"),
        Filter("Filter"),
        KeyBy("KeyBy"),
        Reduce("Reduce");

        private final String name;
        Type(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
