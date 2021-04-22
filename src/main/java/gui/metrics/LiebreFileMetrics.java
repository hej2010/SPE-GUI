package gui.metrics;

import gui.graph.data.GraphObject;
import gui.graph.data.GraphOperator;
import gui.graph.data.GraphStream;
import gui.utils.Files;
import javafx.util.Pair;
import org.apache.commons.io.input.Tailer;

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

    /**
     * Reads metrics starting after the given date
     * @param afterDate the timestamp in seconds
     * @return a list of file data
     */
    public List<FileData> getMetrics(long afterDate) {
        List<FileData> result = new LinkedList<>();
        List<Future<List<FileData>>> tasks = new LinkedList<>();
        for (GraphObject op : graphObjects) {
            tasks.add(executorService.submit(readMetrics(op, afterDate)));
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

    public List<FileData> getMetrics() {
        return getMetrics(-1);
    }

    /*
    For each stream with id X, Liebre will produce two files:

    X.IN.csv (for the rate with which tuples are added to the stream).
    X.OUT.csv (for the rate with which tuples are taken from the stream).

    For each source, operator or sink with id X, Liebre will produce one file:

    X.EXEC.csv (for the processing time of the source, operator or sink)
    X.RATE.csv (for the rate of the source, operator or sink)

     */

    private Callable<List<FileData>> readMetrics(GraphObject op, long afterDate) {
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
                result.add(new FileData(extractData(Files.readFile(dir.getPath() + File.separator + s), afterDate), s));
            }

            return result;
        };
    }

    private List<Pair<Long, String>> extractData(String data, long afterDate) {
        List<Pair<Long, String>> values = new LinkedList<>();
        for (String line : data.split("\n")) {
            String[] d = line.split(",");
            if (d.length == 2) {
                long date = Long.parseLong(d[0].trim());
                if (date > afterDate) {
                    values.add(new Pair<>(date, d[1]));
                }
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
