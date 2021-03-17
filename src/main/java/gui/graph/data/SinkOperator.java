package gui.graph.data;

public class SinkOperator extends GraphOperator {

    public SinkOperator(String name) {
        super(name, false);
    }

    public SinkOperator() {
        super("sink", true);
    }

}
