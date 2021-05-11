package gui.testing;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.HashMap;
import java.util.Map;

public class FlinkTest {
    public static void main(String[] args) throws Exception {
        Map<String, String> conf = new HashMap<>();
        conf.put("metrics.reporter.grph.factory.class", "org.apache.flink.metrics.graphite.GraphiteReporterFactory");
        conf.put("metrics.reporter.grph.host", "localhost");
        conf.put("metrics.reporter.grph.port", "2003");
        conf.put("metrics.reporter.grph.protocol", "TCP");
        conf.put("metrics.reporter.grph.interval", "5 SECONDS");
        final StreamExecutionEnvironment query = StreamExecutionEnvironment.createLocalEnvironment(Configuration.fromMap(conf));

        //DataStream<String> text = query.readTextFile("file:///path/to/file");
        DataStream<String> sourceStream = query.addSource(new SourceFunction<String>() {
            @Override
            public void run(SourceContext<String> ctx) throws Exception {
                int count = 0;
                while (true) {
                    if (count > 100) {
                        return;
                    }
                    ctx.collect(Math.random() * 100 + "");
                    count++;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                    }
                }
            }

            @Override
            public void cancel() {
            }
        }, "source1");

        DataStream<Integer> intStream = query.addSource(new SourceFunction<Integer>() {
            @Override
            public void run(SourceContext<Integer> ctx) throws Exception {
                ctx.collect((int) (Math.random() * 10));
            }

            @Override
            public void cancel() {

            }
        }, "source2");

        SingleOutputStreamOperator<Double> stream = intStream
                .map((MapFunction<Integer, Double>) value -> value * Math.PI)
                .map((MapFunction<Double, Double>) value -> value * Math.PI)
                .filter((FilterFunction<Double>) value -> value > 2);

        stream.addSink(new SinkFunction<Double>() {
            @Override
            public void invoke(Double value, Context context) throws Exception {
                System.out.println(value);
            }
        });

        sourceStream.addSink(new SinkFunction<>() {
            @Override
            public void invoke(String value, Context context) throws Exception {
                System.out.println(value);
            }
        });

        query.execute();

        //System.out.println(query.getExecutionPlan());

    }
}
