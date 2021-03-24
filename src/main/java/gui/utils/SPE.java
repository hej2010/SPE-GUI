package gui.utils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public enum SPE {
    LIEBRE("Liebre", "liebre.json"),
    FLINK("Apache Flink", "apache-flink.json");

    public final String name, fileName;

    SPE(String name, String fileName) {
        this.name = name;
        this.fileName = fileName;
    }

    @Nonnull
    public static List<SPE> asList() {
        return new LinkedList<>(Arrays.asList(values()));
    }

    @Override
    public String toString() {
        return name;
    }
}
