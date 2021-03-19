package gui.utils;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassFinder {
    private static final String REG_EXPR = "(\\s+)?(public\\s+((static|final)(\\s+))*(class|interface|enum)(\\s+)(\\w+)(<.*>)?(.*))";

    public static void main(String[] args) {
        /*Path p = Paths.get(Paths.get(".").toAbsolutePath().normalize().toString(), "/src/main/java/gui/utils/TestClass.java");
        System.out.println(p);
        findClassesIn(new ConcurrentLinkedQueue<>(), p);*/
        Path p = Paths.get(Paths.get(".").toAbsolutePath().normalize().toString(), "/src/main/java/gui");
        //Path p = new File("").toPath();
        Set<String> set = findJavaClassesIn(p.toFile());
        //gui.utils.Files.writeFile(Paths.get("." + System.currentTimeMillis() + ".txt").toFile(), StringStuff.toCommaSeparatedString(set));
        System.out.println(StringStuff.toCommaSeparatedString(set));
    }

    /**
     * Given a directory, traverses it and finds all public Java class names inside .class files.
     *
     * @param directory the root directory to traverse
     * @return a set of all class names
     */
    @Nonnull
    public static Set<String> findJavaClassesIn(@Nonnull File directory) {
        final long start = System.currentTimeMillis();
        Queue<String> javaClasses = new ConcurrentLinkedQueue<>();
        Queue<Thread> threads = new ConcurrentLinkedQueue<>();

        try {
            DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory.toPath());
            Thread startThread = new Thread(() -> findClassFilesIn(javaClasses, dirStream, threads));
            threads.add(startThread);
            startThread.start();
        } catch (IOException e) {
            e.printStackTrace();
            return new HashSet<>();
        }
        Thread t;
        while ((t = threads.peek()) != null) { // wait for all threads to finish
            try {
                t.join();
                threads.remove();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Set<String> set = new HashSet<>(javaClasses);
        System.err.println("findJavaClassesIn() took " + (System.currentTimeMillis() - start) + " ms, found " + set.size() + " classes");
        return set;
    }

    private static void findClassFilesIn(Queue<String> javaClasses, DirectoryStream<Path> dirStream, Queue<Thread> threads) {
        if (dirStream == null) {
            return;
        }

        for (Path p : dirStream) {
            if (Files.isDirectory(p)) {
                // traverse
                try {
                    findClassFilesIn(javaClasses, Files.newDirectoryStream(p), threads);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                String fileName = p.getFileName().toString();
                if (fileName.endsWith(".java")) {
                    Thread readerThread = new Thread(() -> findClassesInFile(javaClasses, p.toFile()));
                    threads.add(readerThread);
                    readerThread.start();
                }
            }
        }
    }

    private static void findClassesInFile(Queue<String> javaClasses, File file) {

        String currentLine;
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(file.getPath()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        Pattern classDeclarationPattern = Pattern.compile(REG_EXPR);
        try {
            while ((currentLine = bufferedReader.readLine()) != null) {
                Matcher classDeclarationMatcher = classDeclarationPattern.matcher(currentLine);
                if (classDeclarationMatcher.matches() && classDeclarationMatcher.groupCount() > 8) {
                    String className = classDeclarationMatcher.group(8);
                    //System.out.println("Found class in file: " + className);
                    javaClasses.add(className);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
