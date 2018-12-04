package com.ont.media.odvp.utils;

/**
 * OntLog
 */

public class OntLog {

    public static final byte LOGLEVEL_DISABLED = 0x00;        // Disabled
    public static final byte LOGLEVEL_ERROR = 0x01;        // Error
    public static final byte LOGLEVEL_WARN = 0x02;        // Warning
    public static final byte LOGLEVEL_INFO = 0x03;        // Info
    public static final byte LOGLEVEL_DEBUG = 0x04;        // Debug
    public static final byte LOGLEVEL_VERBOSE = 0x05;        // Verbose
    public static final byte LOGLEVEL_ALL = 0x7f;        // All
    public static byte LOGLEVEL = LOGLEVEL_DISABLED;

    public static void setLogLevel(final byte level) {

        if (level < LOGLEVEL_DISABLED) {
            LOGLEVEL = LOGLEVEL_DISABLED;
        } else if (level > LOGLEVEL_VERBOSE) {
            LOGLEVEL = LOGLEVEL_ALL;
        } else {
            LOGLEVEL = level;
        }
    }

    public static int getLogLevel() {
        return LOGLEVEL;
    }

    public static void v(final String tag, final String msg) {

        if (LOGLEVEL >= LOGLEVEL_VERBOSE) {
            android.util.Log.v(tag, msg);
        }
    }

    public static void v(final String tag, final String msg, final Throwable tr) {

        if (LOGLEVEL >= LOGLEVEL_VERBOSE) {
            android.util.Log.v(tag, msg, tr);
        }
    }

    public static void d(final String tag, final String msg) {

        if (LOGLEVEL >= LOGLEVEL_DEBUG) {
            android.util.Log.d(tag, msg);
        }
    }

    public static void d(final String tag, final String msg, final Throwable tr) {

        if (LOGLEVEL >= LOGLEVEL_DEBUG) {
            android.util.Log.d(tag, msg, tr);
        }
    }

    public static void i(final String tag, final String msg) {

        if (LOGLEVEL >= LOGLEVEL_INFO) {
            android.util.Log.i(tag, msg);
        }
    }

    public static void i(final String tag, final String msg, final Throwable tr) {

        if (LOGLEVEL >= LOGLEVEL_INFO) {
            android.util.Log.i(tag, msg, tr);
        }
    }

    public static void w(final String tag, final String msg) {

        if (LOGLEVEL >= LOGLEVEL_WARN) {
            android.util.Log.w(tag, msg);
        }
    }

    public static void w(final String tag, final String msg, final Throwable tr) {

        if (LOGLEVEL >= LOGLEVEL_WARN) {
            android.util.Log.w(tag, msg, tr);
        }
    }

    public static void e(final String tag, final String msg) {

        if (LOGLEVEL >= LOGLEVEL_ERROR) {
            android.util.Log.e(tag, msg);
        }
    }

    public static void e(final String tag, final String msg, final Throwable tr) {

        if (LOGLEVEL >= LOGLEVEL_ERROR) {
            android.util.Log.e(tag, msg, tr);
        }
    }

    public static boolean isEnabled() {
        return LOGLEVEL > LOGLEVEL_DISABLED;
    }
}

