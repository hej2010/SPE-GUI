package se.chalmers.datx05.utils;

public class Time {
    public static final int SECOND_SECONDS = 1;
    public static final int MINUTE_SECONDS = SECOND_SECONDS * 60;
    public static final int HOUR_SECONDS = MINUTE_SECONDS * 60;
    public static final int DAY_SECONDS = HOUR_SECONDS * 24;
    public static final int WEEK_SECONDS = DAY_SECONDS * 7;
    public static final int MONTH_SECONDS = DAY_SECONDS * 30;
    public static final int YEAR_SECONDS = DAY_SECONDS * 365;
    public static final String DATE_SECOND = "s";
    public static final String DATE_MINUTE = "min";
    public static final String DATE_HOUR = "h";
    public static final String DATE_DAY = "d";
    public static final String DATE_WEEK = "w";
    public static final String DATE_MONTH = "mon";
    public static final String DATE_YEAR = "y";

    public static int fromString(String s) {
        switch (s) {
            case DATE_SECOND:
                return SECOND_SECONDS;
            case DATE_MINUTE:
                return MINUTE_SECONDS;
            case DATE_HOUR:
                return HOUR_SECONDS;
            case DATE_DAY:
                return DAY_SECONDS;
            case DATE_WEEK:
                return WEEK_SECONDS;
            case DATE_MONTH:
                return MONTH_SECONDS;
            case DATE_YEAR:
                return YEAR_SECONDS;
            default:
                return -1;
        }
    }
}
