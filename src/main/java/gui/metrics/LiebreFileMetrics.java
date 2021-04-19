package gui.metrics;

import gui.graph.data.GraphObject;
import gui.graph.data.GraphOperator;
import gui.graph.data.GraphStream;
import gui.utils.Files;
import javafx.util.Pair;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class LiebreFileMetrics {
    private final ExecutorService executorService = Executors.newFixedThreadPool(8);
    private final File dir;
    private final List<GraphObject> graphObjects;

    public LiebreFileMetrics(@Nonnull File dir, @Nonnull List<GraphStream> streams, @Nonnull List<GraphOperator> operators) {
        this.dir = dir;
        this.graphObjects = new LinkedList<>();
        this.graphObjects.addAll(streams);
        this.graphObjects.addAll(operators);
    }

    public List<FileData> getMetrics() {
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

    private List<Pair<Long, String>> extractData(String data) {
        List<Pair<Long, String>> values = new LinkedList<>();
        for (String line : data.split("\n")) {
            String[] d = line.split(",");
            if (d.length == 2) {
                values.add(new Pair<>(Long.valueOf(d[0].trim()), d[1]));
            }
        }
        return values;
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
                    "values=" + values.size() +
                    ", fileName='" + fileName + '\'' +
                    '}';
        }
    }

}
