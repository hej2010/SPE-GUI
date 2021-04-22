package gui.graph.data;

public class Stream extends GraphStream {

    public Stream(GraphOperator from, GraphOperator to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() {
        return "stream";
    }
}
