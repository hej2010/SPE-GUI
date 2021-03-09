package org.example.testing;

import common.metrics.Metrics;
import common.tuple.BaseRichTuple;
import common.util.Util;
import component.operator.Operator;
import component.sink.Sink;
import component.source.Source;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import query.LiebreContext;
import query.Query;

public class TextMap1 {

    public static void main(String[] args) {

        final String reportFolder = ".";
        final String inputFile = "filename.txt";
        final String outputFile = reportFolder + File.separator + "TextMap1-" + System.currentTimeMillis() + ".out.csv";

        LiebreContext.setOperatorMetrics(Metrics.file(reportFolder));
        LiebreContext.setStreamMetrics(Metrics.file(reportFolder, false));
        LiebreContext.setUserMetrics(Metrics.file(reportFolder));

        Query q = new Query();

        Source<String> i1 = q.addTextFileSource("I1", inputFile);

        Operator<String, MyTuple> inputReader =
                q.addMapOperator(
                        "map",
                        line -> {
                            //Util.sleep(100);
                            String[] tokens = line.split(",");
                            return new MyTuple(Long.valueOf(tokens[0]), Integer.valueOf(tokens[1]), Integer.valueOf(tokens[2]));
                        });

        Operator<MyTuple, MyTuple> multiply =
                q.addMapOperator(
                        "multiply", tuple -> new MyTuple(tuple.timestamp, tuple.key, tuple.value * 2));

        Sink<MyTuple> o1 = q.addTextFileSink("o1", outputFile, true);

        q.connect(i1, inputReader).connect(inputReader, multiply).connect(multiply, o1);

        q.activate();

        //q.deActivate();
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

        @Override
        public String toString() {
            return "MyTuple{" +
                    "timestamp=" + timestamp +
                    ", key=" + key +
                    ", value=" + value +
                    '}';
        }
    }

}