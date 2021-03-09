package org.example.testing;

import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.PickledGraphite;
import common.metrics.Metrics;
import common.util.Util;
import component.operator.Operator;
import component.operator.in1.BaseOperator1In;
import component.sink.Sink;
import component.source.Source;
import component.source.SourceFunction;
import query.LiebreContext;
import query.Query;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SimpleQuery {
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

        Query q = new Query();
        Source<MyTuple> source = q.addBaseSource("I1", new SourceFunction<>() {
            private final Random r = new Random();

            @Override
            public MyTuple get() {
                Util.sleep(r.nextInt(150));
                return new MyTuple(System.currentTimeMillis(), r.nextInt(5), r.nextInt(100));
            }
        });

        Operator<MyTuple, MyTuple> multiply = q.addOperator(new BaseOperator1In<>("M") {
            @Override
            public List<MyTuple> processTupleIn1(MyTuple tuple) {
                List<MyTuple> result = new LinkedList<MyTuple>();
                result.add(new MyTuple(tuple.timestamp, tuple.key, tuple.value * 2));
                return result;
            }
        });

        Sink<MyTuple> sink = q.addBaseSink("O1", tuple -> {
            //System.out.println(tuple.timestamp + "," + tuple.key + "," + tuple.value);
        });

        q.connect(source, multiply).connect(multiply, sink);

        q.activate();
        Util.sleep(10000);
        q.deActivate();
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
