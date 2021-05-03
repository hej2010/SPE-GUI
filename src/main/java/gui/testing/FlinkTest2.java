package gui.testing;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

public class FlinkTest2 {
    private static final String[] CITIES = {"Gothenburg", "Stockholm", "Malmö", "Lund", "Luleå"};

    public static void main(String[] args) {
        final StreamExecutionEnvironment query = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStream<String> sourceStream = query.addSource(new SourceFunction<>() {
            @Override
            public void run(SourceContext<String> ctx) {
                ctx.collect(System.currentTimeMillis() + "," + CITIES[(int) (Math.random() * CITIES.length)] + "," +
                        Math.random() * 100 + "," + (Math.random() * 50 - 20));
            }

            @Override
            public void cancel() {
            }
        });

        DataStream<WeatherData> mapStream = sourceStream.map((MapFunction<String, WeatherData>) value -> {
            String[] s = value.split(",");
            return new WeatherData(Long.parseLong(s[0]), s[1], Double.parseDouble(s[2]), Double.parseDouble(s[3]));
        });

        DataStream<WeatherData> aggregated = mapStream
                .keyBy((KeySelector<WeatherData, String>) WeatherData::getCity)
                .window(TumblingProcessingTimeWindows.of(Time.days(1)))
                .max("temp")
                .setParallelism(1);

        DataStream<Tuple2<Long, Double>> filtered = mapStream.filter((FilterFunction<WeatherData>) value -> value.getCity().equalsIgnoreCase(CITIES[0]) && value.getTemp() >= 21)
                .map(new MapFunction<WeatherData, Tuple2<Long, Double>>() {
                    @Override
                    public Tuple2<Long, Double> map(WeatherData value) throws Exception {
                        return new Tuple2<>(value.timestamp, value.temp);
                    }
                });

        aggregated.addSink(new SinkFunction<>() {
            @Override
            public void invoke(WeatherData value, Context context) {
                System.out.println("Max temp for " + value.city + " at " + value.timestamp + " is " + value.temp);
            }
        });
        filtered.addSink(new SinkFunction<>() {
            @Override
            public void invoke(Tuple2<Long, Double> value, Context context) {
                System.out.println("Max temp for " + CITIES[0] + " at " + value.f0 + " is " + value.f1);
            }
        });

        System.out.println(query.getExecutionPlan());
    }

    public static class WeatherData {
        private long timestamp;
        private double temp, humidity;
        private String city;

        public WeatherData(long timestamp, String city, double humidity, double temp) {
            this.timestamp = timestamp;
            this.city = city;
            this.humidity = humidity;
            this.temp = temp;
        }

        public WeatherData() {
            this(System.currentTimeMillis(), CITIES[(int) (Math.random() * CITIES.length)],
                    Math.random() * 100, (Math.random() * 50 - 20));
        }

        @Override
        public String toString() {
            return "TempData{" +
                    "timestamp=" + timestamp +
                    ", temp=" + temp +
                    ", humidity=" + humidity +
                    ", city='" + city + '\'' +
                    '}';
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public double getTemp() {
            return temp;
        }

        public void setTemp(double temp) {
            this.temp = temp;
        }

        public double getHumidity() {
            return humidity;
        }

        public void setHumidity(double humidity) {
            this.humidity = humidity;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }
}
