package gui.graph.data;

import gui.spe.ParsedOperator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class GraphOperator extends GraphObject {
    protected String name;
    private int selectionIndex;
    private OnSelectionChangedListener listener;
    private ParsedOperator operatorType;

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
        return (isSelected() ? "[" + (selectionIndex == 0 ? "FROM" : "TO") + "] " : "") + name + " (" + (operatorType == null ? "" : (operatorType.getName() + ", ")) + id + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphOperator that = (GraphOperator) o;
        return id == that.id;
    }

    @Nullable
    public ParsedOperator getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(@Nullable ParsedOperator operatorType) {
        this.operatorType = operatorType;
    }

}
