package com.cloudmen.cloudguard.utility;

/**
 * A general-purpose utility class containing common string manipulation
 * and formatting functions used throughout the application.
 */
public final class UtilityFunctions {

    // Prevents instantiation of this utility class
    private UtilityFunctions() {}

    /**
     * Converts a given string to Title Case (capitalizes the first letter
     * of every word and lowercases the rest). <p>
     * * Example: "jOhN dOE" becomes "John Doe".
     *
     * @param str the input string to format
     * @return the formatted Title Case string, or the original string if null/empty
     */
    public static String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Escapes standard HTML characters in a string to prevent formatting
     * issues or basic Cross-Site Scripting (XSS) vulnerabilities when
     * rendering the text in a browser or email.
     *
     * @param text the raw string potentially containing HTML characters
     * @return the escaped, safe HTML string
     */
    public static String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
