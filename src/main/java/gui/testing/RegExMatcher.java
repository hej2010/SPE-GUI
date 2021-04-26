package gui.testing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExMatcher {

    public static void main(String[] args) {
        String line = "query.connect(source1, map1).connect(map1, filter1)";
        String pattern = ".connect\\((\\w+),\\s*(\\w+)\\)";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(line);
        m.find();

        System.out.println("Found value: " + m.group(0) );
        System.out.println("Found value: " + m.group(1) );
        System.out.println("Found value: " + m.group(2) );

        m.find();

        System.out.println("Found value: " + m.group(0) );
        System.out.println("Found value: " + m.group(1) );
        System.out.println("Found value: " + m.group(2) );
    }
}
