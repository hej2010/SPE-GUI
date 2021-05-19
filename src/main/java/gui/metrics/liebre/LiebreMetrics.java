package gui.metrics.liebre;

import gui.graph.data.GraphObject;
import gui.graph.data.GraphOperator;
import gui.graph.data.GraphStream;
import gui.utils.Files;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

public class LiebreMetrics {
    public static final String[] CSV_NAMES = {"count", "max", "mean", "min", "stddev", "p50", "p75", "p95", "p98", "p99", "p999"};
    private final ExecutorService executorService;
    private final File dir;
    private final List<GraphObject> graphObjects;
    private final IOnNewMetricDataListener listener;
    private final List<Tailer> tailers;

    public LiebreMetrics(@Nonnull File dir, @Nonnull List<GraphObject> graphObjects, @Nonnull IOnNewMetricDataListener listener) {
        this.dir = dir;
        this.graphObjects = new LinkedList<>();
        this.graphObjects.addAll(graphObjects);
        if (this.graphObjects.isEmpty()) {
            throw new IllegalStateException("List is null!");
        }
        this.executorService = Executors.newFixedThreadPool(Math.min(16, graphObjects.size() * 2));
        this.listener = listener;
        this.tailers = new LinkedList<>();
    }

    public LiebreMetrics(@Nonnull File dir, @Nonnull List<GraphOperator> graphObjects) {
        this.dir = dir;
        this.graphObjects = new LinkedList<>();
        this.graphObjects.addAll(graphObjects);
        this.executorService = Executors.newFixedThreadPool(Math.min(16, graphObjects.size() * 2));
        this.listener = null;
        this.tailers = new LinkedList<>();
    }

    /**
     * Reads metrics once
     */
    public List<FileData> runOnceSync() {
        List<FileData> result = new LinkedList<>();
        List<Future<List<FileData>>> tasks = new LinkedList<>();
        for (GraphObject op : graphObjects) {
            tasks.add(executorService.submit(readMetrics(op)));
        }
        tasks.forEach(t -> {
            try {
                result.addAll(t.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        return result;
    }

    public void runAndListenAsync(boolean fromEndOfFile) {
        if (listener == null) {
            throw new IllegalStateException("No listener set!");
        }
        if (!tailers.isEmpty()) {
            stop();
        }
        final List<File> filesToRead = getFilesToRead();
        for (File f : filesToRead) {
            TailerListener tailerListener = new MyTailListener(listener, f.getName());
            Tailer tailer = new Tailer(f, tailerListener, 500, fromEndOfFile);
            tailers.add(tailer);
            //System.out.println("for file " + f);
            new Thread(tailer).start();
        }
    }

    public void stop() {
        //System.out.println("Stopped");
        for (Tailer t : tailers) {
            t.stop();
        }
        tailers.clear();
    }

    @Nonnull
    public List<File> getFilesToRead() {
        List<File> filesToRead = new LinkedList<>();
        for (GraphObject op : graphObjects) {
            if (op instanceof GraphStream) {
                GraphStream stream = (GraphStream) op;
                String file = stream.getFrom().getIdentifier2().get() + "_" + stream.getTo().getIdentifier2().get();
                filesToRead.add(new File(dir, file + ".IN.csv"));
                filesToRead.add(new File(dir, file + ".OUT.csv"));
            } else {
                GraphOperator operator = (GraphOperator) op;
                String file = operator.getIdentifier2().get();
                filesToRead.add(new File(dir, file + ".EXEC.csv"));
                filesToRead.add(new File(dir, file + ".RATE.csv"));
            }
        }
        return filesToRead;
    }


    /*
    For each stream with id X, Liebre will produce two files:

    X.IN.csv (for the rate with which tuples are added to the stream).
    X.OUT.csv (for the rate with which tuples are taken from the stream).

    For each source, operator or sink with id X, Liebre will produce one file:

    X.EXEC.csv (for the processing time of the source, operator or sink)
    X.RATE.csv (for the rate of the source, operator or sink)

     */

    private Callable<List<FileData>> readMetrics(GraphObject op) {
        return () -> {
            List<FileData> result = new LinkedList<>();
            List<String> filesToRead = new LinkedList<>();
            if (op instanceof GraphStream) {
                GraphStream stream = (GraphStream) op;
                //filesToRead.add(stream.);
                String file = stream.getFrom().getIdentifier2().get() + "_" + stream.getTo().getIdentifier().get();
                filesToRead.add(file + ".IN.csv");
                filesToRead.add(file + ".OUT.csv");
            } else {
                GraphOperator operator = (GraphOperator) op;
                filesToRead.add(operator.getIdentifier2().get() + ".EXEC.csv");
                filesToRead.add(operator.getIdentifier2().get() + ".RATE.csv");
            }
            for (String s : filesToRead) {
                result.add(new FileData(extractData(Files.readFile(dir.getPath() + File.separator + s)), s));
            }

            return result;
        };
    }

    private static List<MetricsData> extractData(String data) {
        List<MetricsData> values = new LinkedList<>();
        for (String line : data.split("\n")) {
            if (!line.startsWith("t")) {
                String[] d = line.split(",");
                if (d.length == 2) {
                    values.add(new MetricsDataSingle(Long.parseLong(d[0]), Double.parseDouble(d[1])));
                } else if (d.length == 12) {
                    values.add(new MetricsDataMultiple(Long.parseLong(d[0]), getValues(d)));
                } else if (d.length == 20) {
                    values.add(new MetricsDataMultiple(Long.parseLong(d[0]), getValues(fixValues(d))));
                }
            }
        }
        return values;
    }

    private static Map<String, Double> getValues(String[] d) {
        if (d.length != 12) {
            throw new IllegalArgumentException("Not correct length of input! Expected 12 but got " + d.length);
        }
        Map<String, Double> map = new HashMap<>();
        int j = 0;
        for (int i = 1; i < 12; i++) { // Skip first (timestamp)
            map.put(CSV_NAMES[i - 1], Double.valueOf(d[i]));
        }
        return map;
    }

    private static String[] fixValues(String[] d) {
        if (d.length != 20) {
            throw new IllegalArgumentException("Not correct length of input! Expected 20 but got " + d.length);
        }
        String[] res = new String[12];

        // t,count,max,mean,min,stddev,p50,p75,p95,p98,p99,p999
        // 1620058296--8--62821100--53675022,415518--50736500--3590468,664281--52888700,000000--53054400,000000--62821100,000000--62821100,000000--62821100,000000--62821100,000000

        res[0] = d[0];
        res[1] = d[1];
        res[2] = d[2];
        res[3] = d[3] + "." + d[4];
        res[4] = d[5];
        res[5] = d[6] + "." + d[7];
        res[6] = d[8] + "." + d[9];
        res[7] = d[10] + "." + d[11];
        res[8] = d[12] + "." + d[13];
        res[9] = d[14] + "." + d[15];
        res[10] = d[16] + "." + d[17];
        res[11] = d[18] + "." + d[19];

        return res;
    }

    private static class MyTailListener extends TailerListenerAdapter {
        int count;
        private final IOnNewMetricDataListener listener;
        private final String name;

        private MyTailListener(IOnNewMetricDataListener listener, String name) {
            this.listener = listener;
            this.name = name;
            this.count = 0;
        }

        @Override
        public void handle(String line) {
            System.out.println("received " + ++count);
            //System.out.println("received " + line);
            listener.onNewData(new FileData(extractData(line), name));
        }

        @Override
        public void handle(Exception ex) {
            //System.out.println("got error " + ex);
            if (ex != null) {
                ex.printStackTrace();
            }
        }
    }

    public static class FileData {
        private final List<MetricsData> values;
        private final String fileName;

        private FileData(List<MetricsData> values, String fileName) {
            this.values = values;
            this.fileName = fileName;
        }

        public List<MetricsData> getValues() {
            return values;
        }

        public String getFileName() {
            return fileName;
        }

        @Override
        public String toString() {
            return "FileData{" +
                    "values=" + values.size() + ": " + values +
                    ", fileName='" + fileName + '\'' +
                    '}';
        }
    }

}
