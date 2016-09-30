package com.web.spring.datatable.util;

import java.util.Collection;

/**
 * Collection of utilities to ease validating arguments.
 */
public final class Validate {

    /**
     * Prevents instantiation.
     */
    private Validate() {
        super();
    }

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notBlank(String object, String message) {
        if (StringUtils.isBlank(object)) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notBlank(StringBuilder object, String message) {
        Validate.notNull(object, message);
        if (object.length() == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notEmpty(Collection<?> object, String message) {
        if (object == null || object.size() == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notEmpty(Object[] object, String message) {
        if (object == null || object.length == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void containsNoNulls(Iterable<?> collection, String message) {
        for (Object object : collection) {
            notNull(object, message);
        }
    }

    public static void containsNoEmpties(Iterable<String> collection, String message) {
        for (String object : collection) {
            notBlank(object, message);
        }
    }

    public static void containsNoNulls(Object[] array, String message) {
        for (Object object : array) {
            notNull(object, message);
        }
    }

    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
