package gui.testing;

public class StatsParser {
    public static void main(String[] args) {
        double cpuTotal = 0, cpuMax = -1;
        long ramTotal = 0, ramMax = -1;
        int count = 0;
        int max = 60;
        for (String line : s.split("\n")) {
            if (count >= max) {
                break;
            }
            String[] data = line.split(",");
            count++;
            double cpu = Double.parseDouble(data[0].trim());
            long ram = Long.parseLong(data[1].trim());
            cpuTotal += cpu;
            ramTotal += ram;
            if (cpu > cpuMax) {
                cpuMax = cpu;
            }
            if (ram > ramMax) {
                ramMax = ram;
            }
        }
        double avgCpu = cpuTotal / count;
        double avgRam = ramTotal / (count + 0.0);
        System.out.println(avgCpu + ", " + avgRam);
        System.out.println(cpuMax + ", " + ramMax);
        System.out.println("readings: " + count);
    }

    static String s = "66.71791707360745, 107\n" +
            "23.13900212611119, 113\n" +
            "13.860138233694345, 117\n" +
            "13.283755316252646, 121\n" +
            "13.472771262166827, 125\n" +
            "13.976249767435203, 135\n" +
            "47.98922554335477, 125\n" +
            "17.900697136079817, 127\n" +
            "14.456883789785344, 131\n" +
            "13.264673137979466, 133\n" +
            "29.148082781823625, 98";
}
