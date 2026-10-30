package com.urlshortener.urlshortener.util;

public class Base62 {

    private static final String CHARS =
        "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;

    public static String encode(long number) {
        if (number == 0) {
            return String.valueOf(CHARS.charAt(0));
        }
        StringBuilder result = new StringBuilder();
        while (number > 0) {
            int remainder = (int) (number % BASE);
            result.append(CHARS.charAt(remainder));
            number = number / BASE;
        }
        return result.reverse().toString();
    }

    public static long decode(String code) {
        long number = 0;
        for (char c : code.toCharArray()) {
            number = number * BASE + CHARS.indexOf(c);
        }
        return number;
    }
}