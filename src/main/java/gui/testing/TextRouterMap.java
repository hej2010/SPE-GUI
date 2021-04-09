package gui.testing;

import common.util.Util;
import component.operator.Operator;
import component.sink.Sink;
import component.source.Source;
import query.Query;

import java.io.File;

public class TextRouterMap {

    public static void main(String[] args) {

        final String reportFolder = args[0];
        final String inputFile = args[1];
        final String outputFile1 = reportFolder + File.separator + "TextRouterMap_Out1.out.csv";
        final String outputFile2 = reportFolder + File.separator + "TextRouterMap_Out2.out.csv";

        Query q = new Query();

        Source<String> i1 = q.addTextFileSource("I1", inputFile);

        Operator<String, MyTuple> inputReader =
                q.addMapOperator(
                        "map",
                        line -> {
                            Util.sleep(100);
                            String[] tokens = line.split(",");
                            return new MyTuple(
                                    Long.valueOf(tokens[0]), Integer.valueOf(tokens[1]), Integer.valueOf(tokens[2]));
                        });

        Operator<MyTuple, MyTuple> router = q.addRouterOperator("router");

        Operator<MyTuple, MyTuple> filterHigh =
                q.addFilterOperator("fHigh", t -> {
                    Util.sleep(100);
                    return Integer.valueOf(t.getKey()) < 5;
                });

        Operator<MyTuple, MyTuple> filterLow =
                q.addFilterOperator("fLow", t -> Integer.valueOf(t.getKey()) > 4);

        Sink<MyTuple> o1 = q.addTextFileSink("o1", outputFile1, true);
        Sink<MyTuple> o2 = q.addTextFileSink("o2", outputFile2, true);

        q.connect(i1, inputReader)
                .connect(inputReader, router)
                .connect(router, filterHigh)
                .connect(filterHigh, o1)
                .connect(router, filterLow)
                .connect(filterLow, o2);

        q.activate();
    }
}