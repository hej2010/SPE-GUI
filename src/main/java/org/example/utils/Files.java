package org.example.utils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Files {
    @Nonnull
    public static String readFile(@Nonnull String path) throws IOException {
        byte[] encoded = java.nio.file.Files.readAllBytes(Path.of(path));
        return new String(encoded, StandardCharsets.UTF_8);
    }
}
