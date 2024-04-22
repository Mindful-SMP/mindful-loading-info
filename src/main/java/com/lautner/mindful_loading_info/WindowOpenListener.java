// Source code is decompiled from a .class file using FernFlower decompiler.
package com.lautner.mindful_loading_info;

import java.util.LinkedList;
import java.util.List;

public final class WindowOpenListener {
    private static final List<Runnable> listeners = new LinkedList<>();

    public WindowOpenListener() {
    }

    public static List<Runnable> getListeners() {
        return listeners;
    }

    public static void trigger() {

        for (Runnable l : listeners) {
            l.run();
        }

    }
}
