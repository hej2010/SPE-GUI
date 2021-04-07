package gui.testing;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatJoinFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import scala.Tuple2;

import javax.annotation.Nullable;

public class StreamingJob {

    public static void main(String[] args) throws Exception {
        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        DataStreamSource<String> source = env.readTextFile("./h1.txt");

        AllWindowedStream<StreamData, TimeWindow> A1 = source
                .map((MapFunction<String, StreamData>) StreamData::fromString)
                .assignTimestampsAndWatermarks(new MyTimestampsAndWatermarks())
                .filter((FilterFunction<StreamData>) streamData -> streamData.getSpeed() >= 20)
                .windowAll(TumblingEventTimeWindows.of(Time.minutes(1)));

        AllWindowedStream<StreamData, TimeWindow> A2 = source
                .map((MapFunction<String, StreamData>) StreamData::fromString)
                .assignTimestampsAndWatermarks(new MyTimestampsAndWatermarks())
                .filter((FilterFunction<StreamData>) streamData -> streamData.getSpeed() < 20)
                .windowAll(TumblingEventTimeWindows.of(Time.minutes(1)));

        final AllWindowFunction<StreamData, Tuple2<Double, TimeWindow>, TimeWindow> function = (timeWindow, iterable, collector) -> {
            double sum = 0, count = 0;
            for (StreamData sd : iterable) {
                sum += sd.getSpeed();
                count++;
            }
            collector.collect(new Tuple2<>(sum / count, timeWindow));
        };

        SingleOutputStreamOperator<Tuple2<Double, TimeWindow>> out1 = A1.apply(function);
        SingleOutputStreamOperator<Tuple2<Double, TimeWindow>> out2 = A2.apply(function);

        DataStream<String> joinedStreams = out1.join(out2)
                .where((event) -> event._2.getStart()).equalTo((event) -> event._2.getStart())
                .window(TumblingEventTimeWindows.of(Time.minutes(1)))
                .apply((FlatJoinFunction<Tuple2<Double, TimeWindow>, Tuple2<Double, TimeWindow>, String>) (first, second, collector) -> {
                    if (first._2.getStart() == second._2.getStart()) {
                        double diff = Math.abs(first._1 - second._1);
                        if (diff > 20) {
                            collector.collect("Alert! at time " + (first._2.getStart() / 1000) +
                                    ", speed difference is ~" + Math.round(diff) + " MPH (Average above 20 MPH: " + Math.round(first._1)
                                    + " MPH, average below 20 MPH: " + Math.round(second._1) + ")");
                }}});

        // output the result
        joinedStreams.writeAsText("./result.txt", FileSystem.WriteMode.OVERWRITE).setParallelism(1);

        // execute program
        env.execute("Flink Streaming Java API Skeleton");
        System.out.println(env.getExecutionPlan());
    }

    private static class MyTimestampsAndWatermarks implements AssignerWithPeriodicWatermarks<StreamData> {

        @Override
        public long extractTimestamp(StreamData streamData, long l) {
            return streamData.getTime() * 1000; // return the time in milliseconds
        }

        @Nullable
        @Override
        public Watermark getCurrentWatermark() {
            return null;
        }
    }

    private static class StreamData {
        private final int speed;
        private final long time;

        private StreamData(int speed, long time) {
            this.speed = speed;
            this.time = time;
        }

        public int getSpeed() {
            return speed;
        }

        public long getTime() {
            return time;
        }

        public static StreamData fromString(String s) {
            String[] data = s.split(",");
            int speed = Integer.parseInt(data[3]);
            long time = Long.parseLong(data[1]);
            return new StreamData(speed, time);
        }
    }
}
