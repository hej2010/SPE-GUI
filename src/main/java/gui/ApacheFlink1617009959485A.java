package gui;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

public class ApacheFlink1617009959485A {

    public static void main(String[] args) throws Exception {

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStreamSink<String> source1 = env.readTextFile("file:///path/to/file")
                .filter((FilterFunction<String>) value -> {
                    return false;
                })
                .map((MapFunction<String, String>) value -> value + "a")
                .addSink(new SinkFunction<>() {
                    @Override
                    public void invoke(String value, Context context) throws Exception {
                        //
                    }
                });
        env.execute();
        //A a = A.b.e.c.d.test();
        //a.b.c.d.e = A.b.test().e.c.d.test();
    }

    /*private static class A {
        static A b, c, d, e;
        static A test() {
            return new A();
        }
    }*/
}