package ai.content.auto.util;

import java.util.Locale;

/**
 * Utility class for string operations with case-insensitive comparisons
 */
public final class StringUtil {

    private StringUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Compare two strings ignoring case (null-safe)
     * 
     * @param str1 First string
     * @param str2 Second string
     * @return true if strings are equal ignoring case, false otherwise
     */
    public static boolean equalsIgnoreCase(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        return str1.toLowerCase(Locale.ROOT).equals(str2.toLowerCase(Locale.ROOT));
    }

    /**
     * Check if string contains another string ignoring case (null-safe)
     * 
     * @param source Source string
     * @param target Target string to search for
     * @return true if source contains target ignoring case, false otherwise
     */
    public static boolean containsIgnoreCase(String source, String target) {
        if (source == null || target == null) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(target.toLowerCase(Locale.ROOT));
    }

    /**
     * Check if string starts with another string ignoring case (null-safe)
     * 
     * @param source Source string
     * @param prefix Prefix to check
     * @return true if source starts with prefix ignoring case, false otherwise
     */
    public static boolean startsWithIgnoreCase(String source, String prefix) {
        if (source == null || prefix == null) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }

    /**
     * Check if string ends with another string ignoring case (null-safe)
     * 
     * @param source Source string
     * @param suffix Suffix to check
     * @return true if source ends with suffix ignoring case, false otherwise
     */
    public static boolean endsWithIgnoreCase(String source, String suffix) {
        if (source == null || suffix == null) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT));
    }

    /**
     * Convert string to lowercase using ROOT locale for consistency
     * 
     * @param str String to convert
     * @return lowercase string or null if input is null
     */
    public static String toLowerCase(String str) {
        return str != null ? str.toLowerCase(Locale.ROOT) : null;
    }

    /**
     * Convert string to uppercase using ROOT locale for consistency
     * 
     * @param str String to convert
     * @return uppercase string or null if input is null
     */
    public static String toUpperCase(String str) {
        return str != null ? str.toUpperCase(Locale.ROOT) : null;
    }

    /**
     * Check if string is null or empty after trimming
     * 
     * @param str String to check
     * @return true if string is null or empty/whitespace only
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Check if string is not null and not empty after trimming
     * 
     * @param str String to check
     * @return true if string is not null and not empty/whitespace only
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * Get default value if string is blank
     * 
     * @param str          String to check
     * @param defaultValue Default value to return if str is blank
     * @return str if not blank, defaultValue otherwise
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return isNotBlank(str) ? str : defaultValue;
    }
}