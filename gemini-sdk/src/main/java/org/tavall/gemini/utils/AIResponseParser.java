package org.tavall.gemini.utils;


import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

public class AIResponseParser {
    public AIResponseParser(){

    }
    public static void praseGeminiResponse(String response) {

    }
    //====== Helper ======\\

    /**
     * Executes a supplier and returns its result. If an exception is thrown during execution,
     * the provided fallback value is returned instead.
     *
     * @param <T> the type of the result
     * @param supplier a {@code Supplier} that provides the result
     * @param fallback the fallback value to return in case of an exception
     * @return the result of the supplier if successful, or the fallback value if an exception occurs
     */
    private static <T> T tryAndCatch(Supplier<T> supplier, T fallback) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return fallback;
        }
    }

    // ======\\ Primitives \\======

    /**
     * Parses a raw string and returns a trimmed, non-empty string. If the input is null, empty,
     * or consists of only whitespace, the fallback value is returned.
     *
     * @param raw the raw string to parse. May be null or contain leading/trailing whitespace.
     * @param fallback the fallback string to return if the raw string is null, empty, or only whitespace.
     * @return a trimmed, non-empty string if the raw string is valid; otherwise, the fallback value.
     */
    public static String parseString(String raw, String fallback) {
        return raw != null && !raw.trim().isEmpty() ? raw.trim() : fallback;
    }

    /**
     * Parses a raw string and ensures the result does not exceed a specified maximum length.
     * If the raw string is null or empty, the fallback value is returned. If the parsed
     * string's length exceeds the specified maximum length, the fallback value is returned.
     *
     * @param raw      the raw input string to be parsed
     * @param maxLength the maximum allowed length of the parsed string
     * @param fallback the fallback value to return if the raw string is invalid
     *                 or exceeds the maximum length
     * @return the parsed string if valid and within the maximum length, otherwise the fallback value
     */
    public static String parseBoundedString(String raw, int maxLength, String fallback) {
        String parsed = parseString(raw, fallback);
        return parsed != null && parsed.length() <= maxLength ? parsed : fallback;
    }

    /**
     * Parses the given raw string into an integer. If the parsing fails (e.g., due to the input not being
     * a valid number), the specified fallback value is returned instead.
     *
     * @param raw the input string to be parsed as an integer. It may include leading or trailing whitespace.
     * @param fallback the value to return in case the parsing fails or the input is invalid.
     * @return the parsed integer if successful, or the fallback value if parsing fails.
     */
    public static int parseInt(String raw, int fallback) {
        return tryAndCatch(() -> Integer.parseInt(raw.trim()), fallback);
    }

    /**
     * Parses a string into an integer, ensuring the parsed value falls within a specified range.
     * If the parsed value is outside the range or the input is invalid, returns a fallback value.
     *
     * @param raw the input string to parse
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @param fallback the fallback value to use if parsing fails or the value is out of range
     * @return the parsed integer if valid and within the range; otherwise, the fallback value
     */
    public static int parseInt(String raw, int min, int max, int fallback) {
        int value = parseInt(raw, fallback);
        return value >= min && value <= max ? value : fallback;
    }

    /**
     * Parses the given string into a long value. If the string cannot be parsed,
     * the specified fallback value is returned.
     *
     * @param raw the string to be parsed. It is expected to contain a valid long value
     *            or be trim-able to one.
     * @param fallback the value to return if parsing fails.
     * @return the parsed long value if successful; otherwise, the fallback value.
     */
    public static long parseLong(String raw, long fallback) {
        return tryAndCatch(() -> Long.parseLong(raw.trim()), fallback);
    }

    /**
     * Parses a string and attempts to convert it to a double value. If the conversion fails
     * due to an exception (e.g., NumberFormatException), the provided fallback value is returned.
     *
     * @param raw the input string to parse, which may contain a numeric value in string form
     * @param fallback the value to return if parsing fails or the input string is invalid
     * @return the parsed double value if the conversion is successful; otherwise, the fallback value
     */
    public static double parseDouble(String raw, double fallback) {
        return tryAndCatch(() -> Double.parseDouble(raw.trim()), fallback);
    }

    /**
     * Parses a string to a double value while ensuring it falls within a specified range.
     * If the parsed value is outside the range or the string cannot be parsed, a fallback value is returned.
     *
     * @param raw      the string to parse into a double
     * @param min      the minimum allowable value (inclusive)
     * @param max      the maximum allowable value (inclusive)
     * @param fallback the value to return if parsing fails or the parsed value is out of range
     * @return the parsed double value if it is within the specified range, otherwise the fallback value
     */
    public static double parseDouble(String raw, double min, double max, double fallback) {
        double value = parseDouble(raw, fallback);
        return value >= min && value <= max ? value : fallback;
    }

    /**
     * Parses the given string into a boolean value. The method recognizes certain
     * strings such as "true", "1", "yes" (case-insensitive) as true values, and
     * "false", "0", "no" (case-insensitive) as false values. If the string does not match
     * any of the recognized values or an error occurs during parsing, the fallback value
     * is returned.
     *
     * @param raw the input string to be parsed
     * @param fallback the fallback boolean value to return if parsing fails or the input
     *        is not recognized
     * @return the parsed boolean value if successful, or the fallback value otherwise
     */
    public static boolean parseBoolean(String raw, boolean fallback) {
        return tryAndCatch(() -> {
            String trimmed = raw.trim().toLowerCase();
            if (trimmed.equals("true") || trimmed.equals("1") || trimmed.equals("yes") || trimmed.equals("y")) return true;
            if (trimmed.equals("false") || trimmed.equals("0") || trimmed.equals("no") || trimmed.equals("n")) return false;
            return fallback;
        }, fallback);
    }

    // ======\\ Enums \\======

    /**
     * Parses the given raw string into an enum constant of the specified enum type.
     * If the parsing fails or if the raw string is invalid, the provided fallback value is returned.
     *
     * @param <T> the type of the enum
     * @param raw the raw string to be parsed
     * @param enumType the class of the enum to which the string should be parsed
     * @param fallback the fallback value to return in case of parsing failure
     * @return the parsed enum constant if successful; the fallback value otherwise
     */
    public static <T extends Enum<T>> T parseEnum(String raw, Class<T> enumType, T fallback) {
        return tryAndCatch(() -> Enum.valueOf(enumType, raw.trim().toUpperCase().replace(" ", "_")), fallback);
    }

    /**
     * Parses a delimited string into a list of Enum constants of the specified type.
     * The method splits the input string using delimiters such as commas, semicolons, or whitespace
     * and attempts to convert each part into an Enum constant of the specified type. Values
     * that cannot be converted or are null are ignored.
     *
     * @param <T>      the type of Enum being parsed
     * @param raw      the raw, delimited string to parse
     * @param enumType the Enum class type to which the parsed values belong
     * @return a list of Enum constants parsed from the input string, or an empty list if parsing fails
     */
    public static <T extends Enum<T>> List<T> parseEnumList(String raw, Class<T> enumType) {
        return tryAndCatch(() -> {
            List<T> result = new ArrayList<>();
            String[] parts = raw.split("[,;\\s]+");
            for (String part : parts) {
                T value = tryAndCatch(() -> Enum.valueOf(enumType, part.trim().toUpperCase().replace(" ", "_")), null);
                if (value != null) result.add(value);
            }
            return result;
        }, Collections.emptyList());
    }

    //====== Numeric Lists ======\\

    /**
     * Parses a string containing integer values separated by delimiters
     * (commas, semicolons, or whitespace) into a list of integers.
     * Non-numeric or invalid parts are ignored.
     *
     * @param raw the input string containing integer values delimited by commas,
     *            semicolons, or whitespace
     * @return a list of parsed integers, or an empty list if the parsing fails
     */
    public static List<Integer> parseIntList(String raw) {
        return tryAndCatch(() -> {
            List<Integer> result = new ArrayList<>();
            String[] parts = raw.split("[,;\\s]+");
            for (String part : parts) {
                Integer value = tryAndCatch(() -> Integer.parseInt(part.trim()), null);
                if (value != null) result.add(value);
            }
            return result;
        }, Collections.emptyList());
    }

    /**
     * Parses a delimited string into a list of Long values.
     * The input string is split based on delimiters such as commas, semicolons, or whitespace.
     * Each resulting part is parsed into a Long. Invalid parts are ignored.
     *
     * @param raw the delimited input string to parse, may contain numbers separated by commas, semicolons, or whitespace
     * @return a list of parsed Long values, or an empty list if no valid numbers are found
     */
    public static List<Long> parseLongList(String raw) {
        return tryAndCatch(() -> {
            List<Long> result = new ArrayList<>();
            String[] parts = raw.split("[,;\\s]+");
            for (String part : parts) {
                Long value = tryAndCatch(() -> Long.parseLong(part.trim()), null);
                if (value != null) result.add(value);
            }
            return result;
        }, Collections.emptyList());
    }

    /**
     * Parses a string containing a list of numbers into a list of Double values.
     * The input string is split using delimiters such as commas, semicolons, or whitespace.
     * Each extracted substring is then converted into a Double. If conversion fails for any substring,
     * it is ignored.
     *
     * @param raw the raw string to parse, containing numeric values separated by delimiters
     * @return a list of Double values parsed from the input string, or an empty list if the input is null, empty, or contains no valid numbers
     */
    public static List<Double> parseDoubleList(String raw) {
        return tryAndCatch(() -> {
            List<Double> result = new ArrayList<>();
            String[] parts = raw.split("[,;\\s]+");
            for (String part : parts) {
                Double value = tryAndCatch(() -> Double.parseDouble(part.trim()), null);
                if (value != null) result.add(value);
            }
            return result;
        }, Collections.emptyList());
    }

    //====== String Lists ======\\

    /**
     * Parses a raw string into a list of trimmed strings, splitting the input by common delimiters
     * such as commas, semicolons, or newline characters. Empty or whitespace-only entries are
     * excluded from the result.
     *
     * @param raw the raw input string to parse, which may contain multiple tokens separated by
     *            delimiters such as commas, semicolons, or newlines
     * @return a list of non-empty, trimmed strings parsed from the input
     */
    public static List<String> parseStringList(String raw) {
        return tryAndCatch(() -> {
            List<String> result = new ArrayList<>();
            String[] parts = raw.split("[,;\\n]+");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) result.add(trimmed);
            }
            return result;
        }, Collections.emptyList());
    }

    //====== Maps ======\\

    /**
     * Parses a raw string into a map of key-value pairs. The input string is
     * split into lines using commas or newline characters. Each line is further
     * split into key and value using either a colon (:) or an equals sign (=).
     * Keys are trimmed, converted to lowercase, and stored with their
     * corresponding trimmed values in the resulting map.
     *
     * @param raw the raw input string containing key-value pairs, separated by
     *            commas, newline characters, colons, or equals signs
     * @return a map containing the parsed and normalized key-value pairs, or an
     *         empty map if parsing fails
     */
    public static Map<String, String> parseKeyValueMap(String raw) {
        return tryAndCatch(() -> {
            Map<String, String> result = new HashMap<>();
            String[] lines = raw.split("[,\\n]+");
            for (String line : lines) {
                String[] parts = line.contains(":") ? line.split(":", 2) : line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim().toLowerCase();
                    String value = parts[1].trim();
                    if (!key.isEmpty()) result.put(key, value);
                }
            }
            return result;
        }, Collections.emptyMap());
    }

    //====== Time ======\\

    /**
     * Parses a string into an {@link Instant}. If the parsing fails, the provided fallback value is returned.
     *
     * @param raw the input string to parse into an {@link Instant}; may be null or contain whitespace.
     * @param fallback the fallback {@link Instant} value to return if parsing fails.
     * @return the parsed {@link Instant} if successful, or the fallback {@link Instant} if an error occurs.
     */
    public static Instant parseInstant(String raw, Instant fallback) {
        return tryAndCatch(() -> Instant.parse(raw.trim()), fallback);
    }

    //====== AI-Specific ======\\

    /**
     * Parses a confidence value from a string representation. The input string
     * may represent a percentage (e.g., "75%") or a decimal value (e.g., "0.75").
     * If the parsed value is greater than 1.0, it is treated as a percentage
     * (i.e., divided by 100). Values below 0.0 are clamped to 0.0, and values
     * above 1.0 are clamped to 1.0. If parsing fails, the provided fallback value
     * is returned.
     *
     * @param raw the raw string to parse as a confidence value
     * @param fallback the fallback value to return if parsing fails
     * @return the parsed confidence value as a double, adjusted to be within
     *         the valid range [0.0, 1.0], or the fallback value if parsing fails
     */
    public static double parseConfidence(String raw, double fallback) {
        return tryAndCatch(() -> {
            String trimmed = raw.trim().replace("%", "");
            double value = Double.parseDouble(trimmed);
            if (value > 1.0) value = value / 100.0;
            if (value < 0.0) return 0.0;
            if (value > 1.0) return 1.0;
            return value;
        }, fallback);
    }
}