package gui.graph.data;

public class SourceOperator extends GraphOperator {

    public SourceOperator(String name) {
        super(name, false);
    }

    public SourceOperator() {
        super("source", true);
    }

}
