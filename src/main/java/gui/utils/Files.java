package gui.utils;

import gui.spe.SPEParser;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class Files {
    @Nonnull
    public static String readFile(@Nonnull String path) throws IOException {
        byte[] encoded = java.nio.file.Files.readAllBytes(Path.of(path));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    public static String readResource(@Nonnull String resourceName) {
        InputStream inputStream = SPEParser.class.getClassLoader().getResourceAsStream(resourceName);
        StringBuilder sb = new StringBuilder();
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream is null!");
        }
        try {
            for (int ch; (ch = inputStream.read()) != -1; ) {
                sb.append((char) ch);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public static String readFirstLineOfFile(@Nonnull File file) {
        BufferedReader brTest = null;
        try {
            brTest = new BufferedReader(new FileReader(file));
            String text = brTest.readLine();
            brTest.close();
            return text;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (brTest != null) {
                try {
                    brTest.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
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
