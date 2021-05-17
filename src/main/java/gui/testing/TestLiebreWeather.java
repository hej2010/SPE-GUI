package gui.testing;

import com.codahale.metrics.CsvReporter;
import common.metrics.Metrics;
import common.tuple.BaseRichTuple;
import component.operator.Operator;
import component.operator.in1.aggregate.BaseTimeBasedSingleWindow;
import component.operator.in1.aggregate.TimeBasedSingleWindow;
import component.operator.router.RouterOperator;
import component.sink.Sink;
import component.source.Source;
import org.apache.flink.api.java.tuple.Tuple2;
import query.LiebreContext;
import query.Query;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class TestLiebreWeather {
    private static final String[] CITIES = {"Gothenburg", "Stockholm", "Malmö", "Lund", "Luleå"};

    public static void main(String[] args) {
        LiebreContext.setOperatorMetrics(Metrics.dropWizard());
        LiebreContext.setUserMetrics(Metrics.dropWizard());
        LiebreContext.setStreamMetrics(Metrics.dropWizard());
        CsvReporter csvReporter = CsvReporter.forRegistry(Metrics.metricRegistry())
                .build(Paths.get("./metrics").toFile());
        csvReporter.start(1, TimeUnit.SECONDS);

        final Query query = new Query();
        Source<String> source = query.addBaseSource("src1", () -> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return System.currentTimeMillis() + "," + CITIES[(int) (Math.random() * CITIES.length)] + "," +
                    Math.random() * 100 + "," + (Math.random() * 50 - 20);
        });

        Operator<String, WeatherData> map1 = query.addMapOperator("map1", value -> {
            String[] s = value.split(",");
            return new WeatherData(Long.parseLong(s[0]), s[1], Double.parseDouble(s[2]), Double.parseDouble(s[3]));
        });

        RouterOperator<WeatherData> router = query.addRouterOperator("router");
        Operator<WeatherData, WeatherData> agg1 = query.addAggregateOperator("agg1", new MaxWindow(), 86400, 86400);
        Operator<WeatherData, WeatherData> filter1 = query.addFilterOperator("filter1", weatherData -> weatherData.getCity().equalsIgnoreCase(CITIES[0]) && weatherData.getTemp() > 21);
        Operator<WeatherData, Tuple2<Long, Double>> map2 = query.addMapOperator("map2", weatherData -> new Tuple2<>(weatherData.getTimestamp(), weatherData.getTemp()));
        Sink<WeatherData> sink1 = query.addBaseSink("sink1", weatherData -> {/*System.out.println("Max temp for " + weatherData.city + " at " + weatherData.timestamp + " is " + weatherData.temp)*/});
        Sink<Tuple2<Long, Double>> sink2 = query.addBaseSink("sink2", value -> {/*System.out.println("Temp for " + CITIES[0] + " at " + value.f0 + " is " + value.f1)*/});

        query.connect(source, map1).connect(map1, router);
        query.connect(router, agg1).connect(agg1, sink1);
        query.connect(router, filter1).connect(filter1, map2).connect(map2, sink2);

        query.activate();
    }

    public static class WeatherData extends BaseRichTuple {
        private long timestamp;
        private double temp, humidity;
        private String city;

        public WeatherData(long timestamp, String city, double humidity, double temp) {
            super(timestamp, city);
            this.timestamp = timestamp;
            this.city = city;
            this.humidity = humidity;
            this.temp = temp;
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

    private static class MaxWindow extends BaseTimeBasedSingleWindow<WeatherData, WeatherData> {

        private WeatherData max = null;

        @Override
        public void add(WeatherData t) {
            if (max != null) {
                if (max.getTemp() < t.getTemp()) {
                    max = t;
                }
            } else {
                max = t;
            }
        }

        @Override
        public void remove(WeatherData t) {
            if (max == t) {
                max = null;
            }
        }

        @Override
        public WeatherData getAggregatedResult() {
            return max;
        }

        @Override
        public TimeBasedSingleWindow<WeatherData, WeatherData> factory() {
            return new MaxWindow();
        }
    }
}
