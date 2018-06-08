package util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss:SSS");

    public synchronized static void d(String message) {
        log("", message);
    }

    public synchronized static void d(String message, Object... args) {
        log("", String.format(message, args));
    }

    public synchronized static void i(String message) {
        log(ANSI_BLUE, message);
    }

    public synchronized static void i(String message, Object... args) {
        log(ANSI_BLUE, String.format(message, args));
    }

    public synchronized static void e(String message) {
        log(ANSI_RED, message);
    }

    public synchronized static void e(String message, Object... args) {
        log(ANSI_RED, String.format(message, args));
    }

    public synchronized static void g(String message) {
        log(ANSI_GREEN, message);
    }

    public synchronized static void g(String message, Object... args) {
        log(ANSI_GREEN, String.format(message, args));
    }

    public synchronized static void w(String message) {
        log(ANSI_YELLOW, message);
    }

    public synchronized static void w(String message, Object... args) {
        log(ANSI_YELLOW, String.format(message, args));
    }

    public synchronized static void print(String message) {
        System.out.print(message);
    }

    public synchronized static void print(final String color, String message) {
        System.out.print(color + message + ANSI_RESET);
    }

    public synchronized static void println(String message) {
        System.out.println(message);
    }

    public synchronized static void println(final String color, String message) {
        System.out.println(color + message + ANSI_RESET);
    }

    private static void log(final String color, String message) {
        System.out.println(color + String.format("[%s] %s", DATE_FORMAT.format(new Date()), message) + ANSI_RESET);
    }

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
}
