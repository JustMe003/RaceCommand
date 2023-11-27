package io.github.hielkemaps.racecommand.util;

public class TimeConverter {
    public static String convertSecondsToTimeString(int seconds) {
        if (seconds < 0) {
            return "Invalid input";
        }

        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;

        if (minutes == 0) {
            return "" + seconds;
        } else {
            return String.format("%d min %d sec", minutes, remainingSeconds);
        }
    }
}
