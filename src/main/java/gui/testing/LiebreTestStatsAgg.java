package gui.testing;

import common.metrics.Metrics;
import common.tuple.BaseRichTuple;
import component.operator.Operator;
import component.operator.in1.aggregate.BaseTimeBasedSingleWindow;
import component.operator.in1.aggregate.TimeBasedSingleWindow;
import component.sink.Sink;
import component.source.Source;
import query.LiebreContext;
import query.Query;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

public class LiebreTestStatsAgg {
    public static void main(String[] args) {

        LiebreContext.setOperatorMetrics(Metrics.file("."));
        LiebreContext.setStreamMetrics(Metrics.file("."));
        //LiebreContext.setUserMetrics(Metrics.file("."));
        //Metric m = LiebreContext.userMetrics().newAverageMetric("sadasdas3", "abc");

        Query query = new Query();
        Random random = new Random();
        Source<Double> src = query.addBaseSource("mySource", () -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return random.nextDouble() * 100;
        });
        Operator<Double, InputTuple> mOp = query.addMapOperator("myMap", aDouble -> new InputTuple(System.currentTimeMillis(), 34, aDouble.intValue()));

        Operator<InputTuple, OutputTuple> aggregate = query.addAggregateOperator("agg", new AverageWindow(), 60, 20);

        Sink<OutputTuple> sink = query.addBaseSink("sadasdas3", myTuple -> {
            //System.out.println("tuple is " + myTuple);
        });

        query.connect(src, mOp).connect(mOp, aggregate).connect(aggregate, sink);

        query.activate();
        //Util.sleep(10000);
        //query.deActivate();
    }

    private static void startListener() {
        new Thread(() -> {
            try {
                ServerSocket ss = new ServerSocket(); // Unbound socket
                ss.bind(new InetSocketAddress("localhost", 2004)); // Bind the socket to a specific interface
                Socket client;
                while ((client = ss.accept()) != null) {
                    System.out.println(client.toString());
                    byte[] b = client.getInputStream().readAllBytes();
                    System.out.println("b: " + Arrays.toString(b));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static class InputTuple extends BaseRichTuple {

        public int value;

        public InputTuple(long timestamp, int key, int value) {
            super(timestamp, key + "");
            this.value = value;
        }
    }

    private static class OutputTuple extends BaseRichTuple {

        public int count;
        public double average;

        public OutputTuple(long timestamp, int key, int count, double average) {
            super(timestamp, key + "");
            this.count = count;
            this.average = average;
        }

        @Override
        public String toString() {
            return timestamp + "," + key + "," + count +
                    "," + average;
        }
    }

    private static class AverageWindow extends BaseTimeBasedSingleWindow<InputTuple, OutputTuple> {

        private double count = 0;
        private double sum = 0;

        @Override
        public void add(InputTuple t) {
            count++;
            sum += t.value;
        }

        @Override
        public void remove(InputTuple t) {
            count--;
            sum -= t.value;
        }

        @Override
        public OutputTuple getAggregatedResult() {
            double average = count > 0 ? sum / count : 0;
            return new OutputTuple(startTimestamp, Integer.valueOf(key), (int) count, average);
        }

        @Override
        public TimeBasedSingleWindow<InputTuple, OutputTuple> factory() {
            return new AverageWindow();
        }
    }
}
