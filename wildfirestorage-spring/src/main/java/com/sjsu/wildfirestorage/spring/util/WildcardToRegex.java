package com.sjsu.wildfirestorage.spring.util;

public class WildcardToRegex {

    public static String wildcardToRegex(String wildcard) {
        StringBuilder regex = new StringBuilder("^");

        for (char ch : wildcard.toCharArray()) {
            switch (ch) {
                case '*':
                    regex.append("[^\\/]*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '.':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '^':
                case '$':
                case '+':
                case '|':
                    regex.append("\\").append(ch);
                    break;
                default:
                    regex.append(ch);
            }
        }

        regex.append("$");
        return regex.toString();
    }
}
