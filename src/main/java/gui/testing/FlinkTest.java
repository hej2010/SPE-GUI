package gui.testing;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.api.windowing.windows.Window;

public class FlinkTest {
    public static void main(String[] args) {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStream<String> text = env.readTextFile("file:///path/to/file");
        DataStream<String> sourceStream = env.addSource(new SourceFunction<String>() {
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

        sourceStream.filter(new FilterFunction<String>() {
            @Override
            public boolean filter(String value) throws Exception {
                return false;
            }
        }).keyBy(new KeySelector<String, Object>() {
            @Override
            public Object getKey(String value) throws Exception {
                return null;
            }
        }).max("temp");

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
        WindowedStream<String, String, TimeWindow> window = keyby1.window(SlidingProcessingTimeWindows.of(Time.minutes(4), Time.minutes(4)));


    }
}
