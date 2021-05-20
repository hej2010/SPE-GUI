package gui.testing;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class StatsParserLatGraph {
    public static void main(String[] args) {
        long latTotalTot = 0, latMaxTot = -1, latMinTot = 1000000000000L;
        long latTotalNet = 0, latMaxNet = -1, latMinNet = 1000000000000L;
        int max = 10;
        List<Long> all = new LinkedList<>(), allNet = new LinkedList<>();
        int count = 0;
        for (String line : s.split("\n")) {
            if (count >= max) {
                break;
            }
            if (line.startsWith(",")) {
                continue;
            }
            String[] data = line.split(",");
            long network = Long.parseLong(data[1].trim());
            long total = Long.parseLong(data[2].trim());
            latTotalTot += total;
            latTotalNet += network;
            all.add(total);
            allNet.add(network);
            if (total > latMaxTot) {
                latMaxTot = total;
            }
            if (total < latMinTot) {
                latMinTot = total;
            }
            if (network > latMaxNet) {
                latMaxNet = network;
            }
            if (network < latMinNet) {
                latMinNet = network;
            }
            count++;
        }
        Collections.sort(all);
        Collections.sort(allNet);
        long avgLat = latTotalTot / count;
        System.out.println("min " + latMinTot / 1000000.0);
        System.out.println("max " + latMaxTot / 1000000.0);
        System.out.println("med " + all.get(count / 2) / 1000000.0);
        System.out.println("avg " + avgLat / 1000000.0);
        System.out.println();
        avgLat = latTotalNet / count;
        System.out.println("min " + latMinNet / 1000000.0);
        System.out.println("max " + latMaxNet / 1000000.0);
        System.out.println("med " + allNet.get(count / 2) / 1000000.0);
        System.out.println("avg " + avgLat / 1000000.0);
        System.out.println("readings: " + count);
    }

    static String s = "t,606003699,634465200\n" +
            ",29667\n" +
            "t,600303201,630689501\n" +
            ",29667\n" +
            "t,697035999,726398500\n" +
            ",29699\n" +
            "t,512025400,538967800\n" +
            ",29681\n" +
            "t,545894800,577587900\n" +
            ",29671\n" +
            "t,596412400,623023200\n" +
            ",29667\n" +
            "t,546506700,573643600\n" +
            ",29667\n" +
            "t,531334399,560347100\n" +
            ",29655\n" +
            "t,602697901,632718300\n" +
            ",29667\n" +
            "t,604765800,632565900\n" +
            ",29700\n";
}
