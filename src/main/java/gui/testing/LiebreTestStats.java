package gui.testing;

import common.metrics.Metrics;
import component.operator.Operator;
import component.sink.Sink;
import component.source.Source;
import gui.graph.data.GraphOperator;
import gui.graph.data.GraphStream;
import gui.metrics.LiebreFileMetrics;
import query.LiebreContext;
import query.Query;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.*;

public class LiebreTestStats {
    public static void main(String[] args) {

        LiebreContext.setOperatorMetrics(Metrics.file("."));
        LiebreContext.setStreamMetrics(Metrics.file("."));

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

        Timer timer = new Timer();
        List<GraphOperator> ops = new LinkedList<>();
        gui.graph.data.Operator op1 = new gui.graph.data.Operator();
        gui.graph.data.Operator op2 = new gui.graph.data.Operator();
        gui.graph.data.Operator op3 = new gui.graph.data.Operator();
        op1.setIdentifier("mySource");
        op2.setIdentifier("myMap");
        op3.setIdentifier("sadasdas3");
        ops.add(op1);
        ops.add(op2);
        ops.add(op3);
        List<GraphStream> streams = new LinkedList<>();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                List<LiebreFileMetrics.FileData> metrics = new LiebreFileMetrics(Path.of("").toAbsolutePath().toFile(), streams, ops).getMetrics();
                System.out.println("Got " + metrics.size() + ": " + metrics);
            }
        }, 5000, 5000);

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
