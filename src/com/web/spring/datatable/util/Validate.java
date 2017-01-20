package com.web.spring.datatable.util;

import com.library.common.StringHelper;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        System.out.println(isDate("2016-10-25 13:39:56"));
    }

    /**
     * Not null.
     *
     * @param object  the object
     * @param message the message
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Not blank.
     *
     * @param object  the object
     * @param message the message
     */
    public static void notBlank(String object, String message) {
        if (StringHelper.isEmpty(object)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Not blank.
     *
     * @param object  the object
     * @param message the message
     */
    public static void notBlank(StringBuilder object, String message) {
        Validate.notNull(object, message);
        if (object.length() == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Not empty.
     *
     * @param object  the object
     * @param message the message
     */
    public static void notEmpty(Collection<?> object, String message) {
        if (object == null || object.size() == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Not empty.
     *
     * @param object  the object
     * @param message the message
     */
    public static void notEmpty(Object[] object, String message) {
        if (object == null || object.length == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Contains no nulls.
     *
     * @param collection the collection
     * @param message    the message
     */
    public static void containsNoNulls(Iterable<?> collection, String message) {
        for (Object object : collection) {
            notNull(object, message);
        }
    }

    /**
     * Contains no empties.
     *
     * @param collection the collection
     * @param message    the message
     */
    public static void containsNoEmpties(Iterable<String> collection, String message) {
        for (String object : collection) {
            notBlank(object, message);
        }
    }

    /**
     * Contains no nulls.
     *
     * @param array   the array
     * @param message the message
     */
    public static void containsNoNulls(Object[] array, String message) {
        for (Object object : array) {
            notNull(object, message);
        }
    }

    /**
     * Is true.
     *
     * @param condition the condition
     * @param message   the message
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Is date boolean.
     *
     * @param str the str
     * @return the boolean
     */
    public static boolean isDate(String str) {
        Pattern p = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}.*");
        Matcher m = p.matcher(str);
        return m.matches();
    }
}
