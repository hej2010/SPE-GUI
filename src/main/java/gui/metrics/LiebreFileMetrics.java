package gui.metrics;

import gui.graph.data.GraphObject;
import gui.graph.data.GraphOperator;
import gui.graph.data.GraphStream;
import gui.utils.Files;
import javafx.util.Pair;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class LiebreFileMetrics {
    private final ExecutorService executorService;
    private final File dir;
    private final List<GraphObject> graphObjects;
    private final IOnNewMetricDataListener listener;
    private final List<Tailer> tailers;

    public LiebreFileMetrics(@Nonnull File dir, @Nonnull List<GraphStream> streams, @Nonnull List<GraphOperator> operators, @Nonnull IOnNewMetricDataListener listener) {
        this.dir = dir;
        this.graphObjects = new LinkedList<>();
        this.graphObjects.addAll(streams);
        this.graphObjects.addAll(operators);
        this.executorService = Executors.newFixedThreadPool(Math.min(16, graphObjects.size() * 2));
        this.listener = listener;
        this.tailers = new LinkedList<>();
    }

    public LiebreFileMetrics(@Nonnull File dir, @Nonnull List<GraphStream> streams, @Nonnull List<GraphOperator> operators) {
        this.dir = dir;
        this.graphObjects = new LinkedList<>();
        this.graphObjects.addAll(streams);
        this.graphObjects.addAll(operators);
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
            System.out.println("for file " + f);
            new Thread(tailer).start();
        }
    }

    public void stop() {
        System.out.println("Stopped");
        for (Tailer t : tailers) {
            t.stop();
        }
        tailers.clear();
    }

    @Nonnull
    private List<File> getFilesToRead() {
        List<File> filesToRead = new LinkedList<>();
        for (GraphObject op : graphObjects) {
            if (op instanceof GraphStream) {
                GraphStream stream = (GraphStream) op;
                String file = stream.getFrom().getIdentifier().get() + "_" + stream.getTo().getIdentifier().get();
                filesToRead.add(new File(dir, file + ".IN.csv"));
                filesToRead.add(new File(dir, file + ".OUT.csv"));
            } else {
                GraphOperator operator = (GraphOperator) op;
                String file = ((GraphOperator) op).getIdentifier().get();
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
                String file = stream.getFrom().getIdentifier().get() + "_" + stream.getTo().getIdentifier().get();
                filesToRead.add(file + ".IN.csv");
                filesToRead.add(file + ".OUT.csv");
            } else {
                GraphOperator operator = (GraphOperator) op;
                filesToRead.add(operator.getIdentifier().get() + ".EXEC.csv");
                filesToRead.add(operator.getIdentifier().get() + ".RATE.csv");
            }
            for (String s : filesToRead) {
                result.add(new FileData(extractData(Files.readFile(dir.getPath() + File.separator + s)), s));
            }

            return result;
        };
    }

    private static List<Pair<Long, String>> extractData(String data) {
        List<Pair<Long, String>> values = new LinkedList<>();
        for (String line : data.split("\n")) {
            String[] d = line.split(",");
            if (d.length == 2) {
                values.add(new Pair<>(Long.parseLong(d[0].trim()), d[1]));
            }
        }
        return values;
    }

    private static class MyTailListener extends TailerListenerAdapter {
        private final IOnNewMetricDataListener listener;
        private final String name;

        private MyTailListener(IOnNewMetricDataListener listener, String name) {
            this.listener = listener;
            this.name = name;
        }

        @Override
        public void handle(String line) {
            //System.out.println("received " + line);
            listener.onNewData(new FileData(extractData(line), name));
        }

        @Override
        public void handle(Exception ex) {
            if (ex != null) {
                ex.printStackTrace();
            }
        }
    }

    public static class FileData {
        private final List<Pair<Long, String>> values;
        private final String fileName;

        private FileData(List<Pair<Long, String>> values, String fileName) {
            this.values = values;
            this.fileName = fileName;
        }

        public List<Pair<Long, String>> getValues() {
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
