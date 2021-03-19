package gui.utils;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Set;

public class StringStuff {

    @Nonnull
    public static String toCommaSeparatedString(@Nonnull Set<String> set) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

}
