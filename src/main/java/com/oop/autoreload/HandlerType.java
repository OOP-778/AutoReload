package com.oop.autoreload;

public enum HandlerType {
    SERVER_RELOAD,
    SERVER_RESTART,
    PLUGIN_RELOAD;

    public static HandlerType match(String in) {
        in = in.replace("-", "").toLowerCase();
        for (HandlerType value : values()) {
            if (value.name().replace("_", "").toLowerCase().contentEquals(in)) {
                return value;
            }
        }
        throw new IllegalStateException("Invalid Handler type by " + in);
    }
}
