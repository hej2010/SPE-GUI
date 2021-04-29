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

    @Override
    public boolean equals(Object o) {
        if (o instanceof Stream) {
            Stream s2 = (Stream) o;
            return from.equals(s2.from) && to.equals(s2.to);
        }
        return false;
    }
}
