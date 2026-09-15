package com.pfe.adminagent.ai;

/**
 * Defensive enum parsing for values coming from the LLM.
 */
public final class Enums {

    private Enums() {
    }

    public static <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
