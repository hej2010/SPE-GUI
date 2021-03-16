package gui.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class Files {
    @Nonnull
    public static String readFile(@Nonnull String path) throws IOException {
        byte[] encoded = java.nio.file.Files.readAllBytes(Path.of(path));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    @Nullable
    public static String writeFile(@Nonnull File file, @Nonnull String content) {
        try {
            java.io.FileWriter writer = new java.io.FileWriter(file);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }
        return null;
    }
}
