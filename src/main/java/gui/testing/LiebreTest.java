package gui.testing;

import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.PickledGraphite;
import common.metrics.Metrics;
import common.util.Util;
import component.operator.Operator;
import component.operator.in1.BaseOperator1In;
import component.operator.in1.filter.FilterFunction;
import component.operator.in1.map.FlatMapFunction;
import component.operator.in1.map.MapFunction;
import component.operator.in2.BaseOperator2In;
import component.operator.router.RouterOperator;
import component.source.Source;
import query.LiebreContext;
import query.Query;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LiebreTest {
    public static void main(String[] args) {

        LiebreContext.setOperatorMetrics(Metrics.dropWizard());
        LiebreContext.setUserMetrics(Metrics.dropWizard());
        LiebreContext.setStreamMetrics(Metrics.dropWizard());
        final PickledGraphite graphite = new PickledGraphite(new InetSocketAddress("localhost", 2004));
        final GraphiteReporter graphiteReporter =
                GraphiteReporter.forRegistry(Metrics.metricRegistry())
                        .prefixedWith(String.format("liebre.%s.", "test"/*System.currentTimeMillis()*/))
                        .build(graphite);
        graphiteReporter.start(1, TimeUnit.SECONDS);

        Query query = new Query();
        Operator<Integer, Double> mOp = query.addMapOperator("map1", integer -> integer * Math.PI);
        Operator<Double, Double> fOp = query.addFilterOperator("filter1", integer -> integer > 2.0);
        query.connect(mOp, fOp);

        Operator<MyTuple, MyTuple> multiply = query.addOperator(new BaseOperator1In<>("M") {
            @Override
            public List<MyTuple> processTupleIn1(MyTuple tuple) {
                List<MyTuple> result = new LinkedList<MyTuple>();
                result.add(new MyTuple(tuple.timestamp, tuple.key, tuple.value * 2));
                return result;
            }
        });

        query.addMapOperator("df", (MapFunction<MyTuple, MyTuple>) myTuple -> {
            myTuple.key = 3434;
            return myTuple;
        });
        query.addOperator(new BaseOperator2In<String, Float, Integer>("") {

            @Override
            public List<Integer> processTupleIn1(String tuple) {
                return null;
            }

            @Override
            public List<Integer> processTupleIn2(Float tuple) {
                return null;
            }
        });
        query.addFlatMapOperator("df", (FlatMapFunction<String, Integer>) tuple -> {
            //
            return null;
        });
        Operator<String, String> ID = query.addFilterOperator("fd", (FilterFunction<String>) s -> {
            //
            return false;
        });
        RouterOperator<Integer> r = query.addRouterOperator("");


        Source<MyTuple> sink = query.addBaseSource("O1", () -> {
            //
            return null;
        });
        Source<String> sink2 = query.addTextFileSource("fd",
                "fdfd"
        );

        //q.connect(source, multiply).connect(multiply, sink);

        query.activate();
        Util.sleep(10000);
        query.deActivate();
    }

    private static class MyTuple {

        public long timestamp;
        public int key;
        public int value;

        public MyTuple(long timestamp, int key, int value) {
            this.timestamp = timestamp;
            this.key = key;
            this.value = value;
        }
    }
}
