package me.muxteam.basic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Stacktrace {
    private static final Logger LOGGER = LogManager.getLogger("MuxSystem-Error");

    public static void print(final String str, final Throwable throwable) { LOGGER.error(str + (str.endsWith(" ") ? "" : " ") + getStacktrace(throwable));}

    public static void print(final Throwable throwable) {
        LOGGER.error("An error occurs! : " + getStacktrace(throwable));
    }

    public static String getStacktrace(final Throwable throwable) {
        final StringWriter stack = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stack));
        return stack.toString();
    }
}