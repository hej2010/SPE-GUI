package gui.testing;

import com.codahale.metrics.CsvReporter;
import common.metrics.Metric;
import common.metrics.Metrics;
import component.operator.Operator;
import component.sink.Sink;
import component.source.Source;
import query.LiebreContext;
import query.Query;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class LiebreTestStats {
    public static void main(String[] args) {

        /*LiebreContext.setOperatorMetrics(Metrics.file("."));
        LiebreContext.setStreamMetrics(Metrics.file("."));
        LiebreContext.setUserMetrics(Metrics.file("."));*/
        LiebreContext.setOperatorMetrics(Metrics.dropWizard());
        LiebreContext.setUserMetrics(Metrics.dropWizard());
        LiebreContext.setStreamMetrics(Metrics.dropWizard());
        CsvReporter csvReporter = CsvReporter.forRegistry(Metrics.metricRegistry())
                .build(Paths.get(".").toFile());
        csvReporter.start(1, TimeUnit.SECONDS);

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
        Operator<Double, Double> mOp = query.addMapOperator("myMap", integer -> integer * Math.PI);

        Sink<Double> sink = query.addBaseSink("sadasdas3", myTuple -> {
            //System.out.println("tuple is " + myTuple);
        });

        query.connect(src, mOp).connect(mOp, sink);

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
}
