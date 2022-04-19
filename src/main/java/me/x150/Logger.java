package me.x150;

import org.fusesource.jansi.Ansi;

public class Logger {
    public static void error(String text) {
        logColored(text, Ansi.Color.RED);
    }

    public static void logWithoutNewline(Object t) {
        System.out.print(t);
    }

    public static void warning(String text) {
        logColored(text, Ansi.Color.YELLOW);
    }

    public static void info(String text) {
        logColored(text, Ansi.Color.WHITE);
    }

    public static void logColored(String text, Ansi.Color color) {
        log(Ansi.ansi().fg(color).a(text));
    }

    public static void log(Object text) {
        System.out.println(text);
    }
}
