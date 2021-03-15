package gui.graph.dag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

public class Node<T> {
    private final List<Node<T>> successors;
    private final T item;

    public Node(@Nonnull T item, @Nullable List<Node<T>> successors) {
        this.successors = successors;
        this.item = item;
    }

    @Nonnull
    public List<Node<T>> getSuccessors() {
        return successors == null ? new LinkedList<>() : successors;
    }

    @Nonnull
    public T getItem() {
        return item;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Node{");
        sb.append("item=").append(item.toString()).append(", successors=");
        for (Node<T> n : getSuccessors()) {
            sb.append(n.toString());
        }
        return sb.append('}').toString();
    }
}
