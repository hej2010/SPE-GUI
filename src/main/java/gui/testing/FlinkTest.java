package gui.testing;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

public class FlinkTest {
    public static void main(String[] args) {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStream<String> text = env.readTextFile("file:///path/to/file");
        DataStream<String> text2 = env.addSource(new SourceFunction<String>() {
            @Override
            public void run(SourceContext<String> ctx) throws Exception {
                //
                //
            }

            @Override
            public void cancel() {
                //
                //
            }
        });
        DataStreamSink<String> sink = text.addSink(new SinkFunction<String>() {
            @Override
            public void invoke(String value, Context context) throws Exception {

            }
        });

        DataStream<String> map1 = text.map((MapFunction<String, String>) value -> {
            //
            return value;
        });

        DataStream<String> flatmap1 = text.flatMap((FlatMapFunction<String, String>) (value, out) -> {
            //
            //
        });

        DataStream<String> filter1 = text.filter((FilterFunction<String>) value -> {
            //
            return false;
        });

        KeyedStream<String, String> keyby1 = text.keyBy((KeySelector<String, String>) value -> {
            //

            return value;
        });
        DataStream<String> reduce1 = keyby1.reduce((ReduceFunction<String>) (value1, value2) -> {
            //
            return null;
        });


    }
}
