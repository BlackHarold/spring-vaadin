package ru.suek.util;

public class Token {
    private static String value;

    public static String getValue() {
        return value;
    }

    public static void setValue(String value) {
        Token.value = value;
    }

    public static boolean isUserLoggedIn() {
        return value != null && value.equals("logon");
    }
}
