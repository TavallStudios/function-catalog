package org.tavall.gemini.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AIResponseParser utility methods.
 * Tests cover all parsing methods with various valid, invalid, and edge case inputs.
 */
class AIResponseParserTest {

    private void logTest(String testName, String input, Object expected, Object actual) {
        System.out.printf("%-50s | Input: %-30s | Expected: %-20s | Actual: %-20s | %s%n",
                testName, input, expected, actual, expected.equals(actual) ? "✓ PASS" : "✗ FAIL");
    }

    private void logTestHeader(String category) {
        System.out.println("\n" + "=".repeat(140));
        System.out.println(category);
        System.out.println("=".repeat(140));
    }

    // ======== String Parsing Tests ========

    @Test
    void testParseString_ValidInput() {
        logTestHeader("String Parsing - Valid Input");

        String result1 = AIResponseParser.parseString("  Hello World  ", "fallback");
        logTest("parseString(trimmed)", "  Hello World  ", "Hello World", result1);
        assertEquals("Hello World", result1);

        String result2 = AIResponseParser.parseString("Test", "fallback");
        logTest("parseString(plain)", "Test", "Test", result2);
        assertEquals("Test", result2);
    }

    @Test
    void testParseString_NullOrEmpty() {
        logTestHeader("String Parsing - Null/Empty");

        String result1 = AIResponseParser.parseString(null, "fallback");
        logTest("parseString(null)", "null", "fallback", result1);
        assertEquals("fallback", result1);

        String result2 = AIResponseParser.parseString("", "fallback");
        logTest("parseString(empty)", "\"\"", "fallback", result2);
        assertEquals("fallback", result2);

        String result3 = AIResponseParser.parseString("   ", "fallback");
        logTest("parseString(whitespace)", "\"   \"", "fallback", result3);
        assertEquals("fallback", result3);
    }

    @Test
    void testParseBoundedString_WithinBounds() {
        logTestHeader("Bounded String Parsing - Within Bounds");

        String result1 = AIResponseParser.parseBoundedString("Short", 10, "fallback");
        logTest("parseBoundedString(5/10)", "Short", "Short", result1);
        assertEquals("Short", result1);

        String result2 = AIResponseParser.parseBoundedString("  Test  ", 10, "fallback");
        logTest("parseBoundedString(trimmed 4/10)", "  Test  ", "Test", result2);
        assertEquals("Test", result2);
    }

    @Test
    void testParseBoundedString_ExceedsBounds() {
        logTestHeader("Bounded String Parsing - Exceeds Bounds");

        String result1 = AIResponseParser.parseBoundedString("This is a very long string", 5, "fallback");
        logTest("parseBoundedString(26/5)", "This is a very long string", "fallback", result1);
        assertEquals("fallback", result1);

        String result2 = AIResponseParser.parseBoundedString("ExactlyTen", 9, "fallback");
        logTest("parseBoundedString(10/9)", "ExactlyTen", "fallback", result2);
        assertEquals("fallback", result2);
    }

    @Test
    void testParseBoundedString_NullOrEmpty() {
        logTestHeader("Bounded String Parsing - Null/Empty");

        String result1 = AIResponseParser.parseBoundedString(null, 10, "fallback");
        logTest("parseBoundedString(null)", "null", "fallback", result1);
        assertEquals("fallback", result1);

        String result2 = AIResponseParser.parseBoundedString("", 10, "fallback");
        logTest("parseBoundedString(empty)", "\"\"", "fallback", result2);
        assertEquals("fallback", result2);
    }

    // ======== Integer Parsing Tests ========

    @Test
    void testParseInt_ValidInput() {
        logTestHeader("Integer Parsing - Valid Input");

        int result1 = AIResponseParser.parseInt("42", 0);
        logTest("parseInt(42)", "42", 42, result1);
        assertEquals(42, result1);

        int result2 = AIResponseParser.parseInt("-100", 0);
        logTest("parseInt(-100)", "-100", -100, result2);
        assertEquals(-100, result2);

        int result3 = AIResponseParser.parseInt("  999  ", 0);
        logTest("parseInt(trimmed)", "  999  ", 999, result3);
        assertEquals(999, result3);
    }

    @Test
    void testParseInt_InvalidInput() {
        logTestHeader("Integer Parsing - Invalid Input");

        int result1 = AIResponseParser.parseInt("not a number", 0);
        logTest("parseInt(invalid)", "not a number", 0, result1);
        assertEquals(0, result1);

        int result2 = AIResponseParser.parseInt("12.34", -1);
        logTest("parseInt(decimal)", "12.34", -1, result2);
        assertEquals(-1, result2);

        int result3 = AIResponseParser.parseInt(null, 100);
        logTest("parseInt(null)", "null", 100, result3);
        assertEquals(100, result3);
    }

    @Test
    void testParseIntBounded_WithinRange() {
        logTestHeader("Integer Parsing - Within Range [0-100]");

        int result1 = AIResponseParser.parseInt("50", 0, 100, -1);
        logTest("parseInt(50, 0-100)", "50", 50, result1);
        assertEquals(50, result1);

        int result2 = AIResponseParser.parseInt("0", 0, 100, -1);
        logTest("parseInt(0, 0-100)", "0", 0, result2);
        assertEquals(0, result2);

        int result3 = AIResponseParser.parseInt("100", 0, 100, -1);
        logTest("parseInt(100, 0-100)", "100", 100, result3);
        assertEquals(100, result3);
    }

    @Test
    void testParseIntBounded_OutOfRange() {
        logTestHeader("Integer Parsing - Out Of Range [0-100]");

        int result1 = AIResponseParser.parseInt("150", 0, 100, -1);
        logTest("parseInt(150, 0-100)", "150", -1, result1);
        assertEquals(-1, result1);

        int result2 = AIResponseParser.parseInt("-50", 0, 100, -1);
        logTest("parseInt(-50, 0-100)", "-50", -1, result2);
        assertEquals(-1, result2);

        int result3 = AIResponseParser.parseInt("101", 0, 100, 99);
        logTest("parseInt(101, 0-100)", "101", 99, result3);
        assertEquals(99, result3);
    }

    @Test
    void testParseIntBounded_InvalidInput() {
        logTestHeader("Integer Parsing - Invalid Input (Bounded)");

        int result1 = AIResponseParser.parseInt("invalid", 0, 100, -1);
        logTest("parseInt(invalid, 0-100)", "invalid", -1, result1);
        assertEquals(-1, result1);

        int result2 = AIResponseParser.parseInt(null, 0, 100, 50);
        logTest("parseInt(null, 0-100)", "null", 50, result2);
        assertEquals(50, result2);
    }

    // ======== Long Parsing Tests ========

    @Test
    void testParseLong_ValidInput() {
        logTestHeader("Long Parsing - Valid Input");

        long result1 = AIResponseParser.parseLong("1234567890", 0L);
        logTest("parseLong(1234567890)", "1234567890", 1234567890L, result1);
        assertEquals(1234567890L, result1);

        long result2 = AIResponseParser.parseLong("-9876543210", 0L);
        logTest("parseLong(-9876543210)", "-9876543210", -9876543210L, result2);
        assertEquals(-9876543210L, result2);

        long result3 = AIResponseParser.parseLong("  999  ", 0L);
        logTest("parseLong(trimmed)", "  999  ", 999L, result3);
        assertEquals(999L, result3);
    }

    @Test
    void testParseLong_InvalidInput() {
        logTestHeader("Long Parsing - Invalid Input");

        long result1 = AIResponseParser.parseLong("not a number", 0L);
        logTest("parseLong(invalid)", "not a number", 0L, result1);
        assertEquals(0L, result1);

        long result2 = AIResponseParser.parseLong("12.34", -1L);
        logTest("parseLong(decimal)", "12.34", -1L, result2);
        assertEquals(-1L, result2);

        long result3 = AIResponseParser.parseLong(null, 100L);
        logTest("parseLong(null)", "null", 100L, result3);
        assertEquals(100L, result3);
    }

    // ======== Double Parsing Tests ========

    @Test
    void testParseDouble_ValidInput() {
        logTestHeader("Double Parsing - Valid Input");

        double result1 = AIResponseParser.parseDouble("3.14", 0.0);
        logTest("parseDouble(3.14)", "3.14", 3.14, result1);
        assertEquals(3.14, result1, 0.001);

        double result2 = AIResponseParser.parseDouble("-99.99", 0.0);
        logTest("parseDouble(-99.99)", "-99.99", -99.99, result2);
        assertEquals(-99.99, result2, 0.001);

        double result3 = AIResponseParser.parseDouble("  42  ", 0.0);
        logTest("parseDouble(trimmed)", "  42  ", 42.0, result3);
        assertEquals(42.0, result3, 0.001);
    }

    @Test
    void testParseDouble_InvalidInput() {
        logTestHeader("Double Parsing - Invalid Input");

        double result1 = AIResponseParser.parseDouble("not a number", 0.0);
        logTest("parseDouble(invalid)", "not a number", 0.0, result1);
        assertEquals(0.0, result1, 0.001);

        double result2 = AIResponseParser.parseDouble(null, -1.0);
        logTest("parseDouble(null)", "null", -1.0, result2);
        assertEquals(-1.0, result2, 0.001);
    }

    @Test
    void testParseDoubleBounded_WithinRange() {
        logTestHeader("Double Parsing - Within Range [0.0-10.0]");

        double result1 = AIResponseParser.parseDouble("5.5", 0.0, 10.0, -1.0);
        logTest("parseDouble(5.5, 0-10)", "5.5", 5.5, result1);
        assertEquals(5.5, result1, 0.001);

        double result2 = AIResponseParser.parseDouble("0", 0.0, 10.0, -1.0);
        logTest("parseDouble(0, 0-10)", "0", 0.0, result2);
        assertEquals(0.0, result2, 0.001);

        double result3 = AIResponseParser.parseDouble("10.0", 0.0, 10.0, -1.0);
        logTest("parseDouble(10.0, 0-10)", "10.0", 10.0, result3);
        assertEquals(10.0, result3, 0.001);
    }

    @Test
    void testParseDoubleBounded_OutOfRange() {
        logTestHeader("Double Parsing - Out Of Range [0.0-10.0]");

        double result1 = AIResponseParser.parseDouble("15.5", 0.0, 10.0, -1.0);
        logTest("parseDouble(15.5, 0-10)", "15.5", -1.0, result1);
        assertEquals(-1.0, result1, 0.001);

        double result2 = AIResponseParser.parseDouble("-5.5", 0.0, 10.0, -1.0);
        logTest("parseDouble(-5.5, 0-10)", "-5.5", -1.0, result2);
        assertEquals(-1.0, result2, 0.001);

        double result3 = AIResponseParser.parseDouble("100", 0.0, 10.0, 99.9);
        logTest("parseDouble(100, 0-10)", "100", 99.9, result3);
        assertEquals(99.9, result3, 0.001);
    }

    @Test
    void testParseDoubleBounded_InvalidInput() {
        logTestHeader("Double Parsing - Invalid Input (Bounded)");

        double result1 = AIResponseParser.parseDouble("invalid", 0.0, 10.0, -1.0);
        logTest("parseDouble(invalid, 0-10)", "invalid", -1.0, result1);
        assertEquals(-1.0, result1, 0.001);

        double result2 = AIResponseParser.parseDouble(null, 0.0, 10.0, 5.0);
        logTest("parseDouble(null, 0-10)", "null", 5.0, result2);
        assertEquals(5.0, result2, 0.001);
    }

    // ======== Boolean Parsing Tests ========

    @Test
    void testParseBoolean_TrueValues() {
        logTestHeader("Boolean Parsing - True Values");

        boolean result1 = AIResponseParser.parseBoolean("true", false);
        logTest("parseBoolean(true)", "true", true, result1);
        assertTrue(result1);

        boolean result2 = AIResponseParser.parseBoolean("TRUE", false);
        logTest("parseBoolean(TRUE)", "TRUE", true, result2);
        assertTrue(result2);

        boolean result3 = AIResponseParser.parseBoolean("1", false);
        logTest("parseBoolean(1)", "1", true, result3);
        assertTrue(result3);

        boolean result4 = AIResponseParser.parseBoolean("yes", false);
        logTest("parseBoolean(yes)", "yes", true, result4);
        assertTrue(result4);

        boolean result5 = AIResponseParser.parseBoolean("YES", false);
        logTest("parseBoolean(YES)", "YES", true, result5);
        assertTrue(result5);
    }

    @Test
    void testParseBoolean_FalseValues() {
        logTestHeader("Boolean Parsing - False Values");

        boolean result1 = AIResponseParser.parseBoolean("false", true);
        logTest("parseBoolean(false)", "false", false, result1);
        assertFalse(result1);

        boolean result2 = AIResponseParser.parseBoolean("FALSE", true);
        logTest("parseBoolean(FALSE)", "FALSE", false, result2);
        assertFalse(result2);

        boolean result3 = AIResponseParser.parseBoolean("0", true);
        logTest("parseBoolean(0)", "0", false, result3);
        assertFalse(result3);

        boolean result4 = AIResponseParser.parseBoolean("no", true);
        logTest("parseBoolean(no)", "no", false, result4);
        assertFalse(result4);

        boolean result5 = AIResponseParser.parseBoolean("NO", true);
        logTest("parseBoolean(NO)", "NO", false, result5);
        assertFalse(result5);
    }

    @Test
    void testParseBoolean_InvalidInput() {
        logTestHeader("Boolean Parsing - Invalid Input (Falls back)");

        boolean result1 = AIResponseParser.parseBoolean("invalid", true);
        logTest("parseBoolean(invalid, fallback=true)", "invalid", true, result1);
        assertTrue(result1);

        boolean result2 = AIResponseParser.parseBoolean("maybe", false);
        logTest("parseBoolean(maybe, fallback=false)", "maybe", false, result2);
        assertFalse(result2);

        boolean result3 = AIResponseParser.parseBoolean(null, true);
        logTest("parseBoolean(null, fallback=true)", "null", true, result3);
        assertTrue(result3);
    }

    // ======== Enum Parsing Tests ========

    @Test
    void testParseEnum_ValidInput() {
        logTestHeader("Enum Parsing - Valid Input");

        TestEnum result1 = AIResponseParser.parseEnum("OPTION_A", TestEnum.class, TestEnum.OPTION_C);
        logTest("parseEnum(OPTION_A)", "OPTION_A", TestEnum.OPTION_A, result1);
        assertEquals(TestEnum.OPTION_A, result1);

        TestEnum result2 = AIResponseParser.parseEnum("OPTION_B", TestEnum.class, TestEnum.OPTION_C);
        logTest("parseEnum(OPTION_B)", "OPTION_B", TestEnum.OPTION_B, result2);
        assertEquals(TestEnum.OPTION_B, result2);
    }

    @Test
    void testParseEnum_InvalidInput() {
        logTestHeader("Enum Parsing - Invalid Input");

        TestEnum result1 = AIResponseParser.parseEnum("INVALID", TestEnum.class, TestEnum.OPTION_C);
        logTest("parseEnum(INVALID)", "INVALID", TestEnum.OPTION_C, result1);
        assertEquals(TestEnum.OPTION_C, result1);

        TestEnum result2 = AIResponseParser.parseEnum(null, TestEnum.class, TestEnum.OPTION_A);
        logTest("parseEnum(null)", "null", TestEnum.OPTION_A, result2);
        assertEquals(TestEnum.OPTION_A, result2);
    }

    @Test
    void testParseEnumList_ValidInput() {
        logTestHeader("Enum List Parsing - Valid Input");

        List<TestEnum> result = AIResponseParser.parseEnumList("OPTION_A,OPTION_B,OPTION_C", TestEnum.class);
        System.out.printf("Input: %-40s | Result: %s%n", "OPTION_A,OPTION_B,OPTION_C", result);
        assertEquals(3, result.size());
        assertTrue(result.contains(TestEnum.OPTION_A));
        assertTrue(result.contains(TestEnum.OPTION_B));
        assertTrue(result.contains(TestEnum.OPTION_C));
    }

    @Test
    void testParseEnumList_MixedValidInvalid() {
        logTestHeader("Enum List Parsing - Mixed Valid/Invalid");

        List<TestEnum> result = AIResponseParser.parseEnumList("OPTION_A,INVALID,OPTION_B", TestEnum.class);
        System.out.printf("Input: %-40s | Result: %s (invalid entries filtered)%n", "OPTION_A,INVALID,OPTION_B", result);
        assertEquals(2, result.size());
        assertTrue(result.contains(TestEnum.OPTION_A));
        assertTrue(result.contains(TestEnum.OPTION_B));
    }

    @Test
    void testParseEnumList_VariousDelimiters() {
        logTestHeader("Enum List Parsing - Various Delimiters");

        List<TestEnum> result1 = AIResponseParser.parseEnumList("OPTION_A;OPTION_B;OPTION_C", TestEnum.class);
        System.out.printf("Input (semicolon): %-40s | Result: %s%n", "OPTION_A;OPTION_B;OPTION_C", result1);
        assertEquals(3, result1.size());

        List<TestEnum> result2 = AIResponseParser.parseEnumList("OPTION_A OPTION_B", TestEnum.class);
        System.out.printf("Input (space):     %-40s | Result: %s%n", "OPTION_A OPTION_B", result2);
        assertEquals(2, result2.size());
    }

    @Test
    void testParseEnumList_EmptyOrNull() {
        logTestHeader("Enum List Parsing - Empty/Null");

        List<TestEnum> result1 = AIResponseParser.parseEnumList(null, TestEnum.class);
        System.out.printf("Input: %-40s | Result: %s%n", "null", result1);
        assertTrue(result1.isEmpty());

        List<TestEnum> result2 = AIResponseParser.parseEnumList("", TestEnum.class);
        System.out.printf("Input: %-40s | Result: %s%n", "\"\"", result2);
        assertTrue(result2.isEmpty());
    }

    // ======== Integer List Parsing Tests ========

    @Test
    void testParseIntList_ValidInput() {
        logTestHeader("Integer List Parsing - Valid Input");

        List<Integer> result = AIResponseParser.parseIntList("1,2,3,4,5");
        System.out.printf("Input: %-40s | Result: %s | Size: %d%n", "1,2,3,4,5", result, result.size());
        assertEquals(5, result.size());
        assertEquals(List.of(1, 2, 3, 4, 5), result);
    }

    @Test
    void testParseIntList_MixedValidInvalid() {
        logTestHeader("Integer List Parsing - Mixed Valid/Invalid");

        List<Integer> result = AIResponseParser.parseIntList("1,invalid,3,notanumber,5");
        System.out.printf("Input: %-40s | Result: %s (invalid filtered)%n", "1,invalid,3,notanumber,5", result);
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(3));
        assertTrue(result.contains(5));
    }

    @Test
    void testParseIntList_VariousDelimiters() {
        logTestHeader("Integer List Parsing - Various Delimiters");

        List<Integer> result1 = AIResponseParser.parseIntList("10;20;30");
        System.out.printf("Input (semicolon): %-40s | Result: %s%n", "10;20;30", result1);
        assertEquals(3, result1.size());

        List<Integer> result2 = AIResponseParser.parseIntList("100 200 300");
        System.out.printf("Input (space):     %-40s | Result: %s%n", "100 200 300", result2);
        assertEquals(3, result2.size());
    }

    @Test
    void testParseIntList_NegativeNumbers() {
        logTestHeader("Integer List Parsing - Negative Numbers");

        List<Integer> result = AIResponseParser.parseIntList("-1,-10,-100,50");
        System.out.printf("Input: %-40s | Result: %s%n", "-1,-10,-100,50", result);
        assertEquals(4, result.size());
        assertTrue(result.contains(-1));
        assertTrue(result.contains(-10));
        assertTrue(result.contains(-100));
        assertTrue(result.contains(50));
    }

    @Test
    void testParseIntList_EmptyOrNull() {
        logTestHeader("Integer List Parsing - Empty/Null");

        List<Integer> result1 = AIResponseParser.parseIntList(null);
        System.out.printf("Input: %-40s | Result: %s%n", "null", result1);
        assertTrue(result1.isEmpty());

        List<Integer> result2 = AIResponseParser.parseIntList("");
        System.out.printf("Input: %-40s | Result: %s%n", "\"\"", result2);
        assertTrue(result2.isEmpty());
    }

    // ======== Long List Parsing Tests ========

    @Test
    void testParseLongList_ValidInput() {
        logTestHeader("Long List Parsing - Valid Input");

        List<Long> result = AIResponseParser.parseLongList("1000000,2000000,3000000");
        System.out.printf("Input: %-40s | Result: %s%n", "1000000,2000000,3000000", result);
        assertEquals(3, result.size());
        assertEquals(List.of(1000000L, 2000000L, 3000000L), result);
    }

    @Test
    void testParseLongList_MixedValidInvalid() {
        logTestHeader("Long List Parsing - Mixed Valid/Invalid");

        List<Long> result = AIResponseParser.parseLongList("123456789,invalid,987654321");
        System.out.printf("Input: %-40s | Result: %s (invalid filtered)%n", "123456789,invalid,987654321", result);
        assertEquals(2, result.size());
        assertTrue(result.contains(123456789L));
        assertTrue(result.contains(987654321L));
    }

    @Test
    void testParseLongList_VariousDelimiters() {
        logTestHeader("Long List Parsing - Various Delimiters");

        List<Long> result1 = AIResponseParser.parseLongList("100;200;300");
        System.out.printf("Input (semicolon): %-40s | Result: %s%n", "100;200;300", result1);
        assertEquals(3, result1.size());

        List<Long> result2 = AIResponseParser.parseLongList("1000 2000 3000");
        System.out.printf("Input (space):     %-40s | Result: %s%n", "1000 2000 3000", result2);
        assertEquals(3, result2.size());
    }

    @Test
    void testParseLongList_EmptyOrNull() {
        logTestHeader("Long List Parsing - Empty/Null");

        List<Long> result1 = AIResponseParser.parseLongList(null);
        System.out.printf("Input: %-40s | Result: %s%n", "null", result1);
        assertTrue(result1.isEmpty());

        List<Long> result2 = AIResponseParser.parseLongList("");
        System.out.printf("Input: %-40s | Result: %s%n", "\"\"", result2);
        assertTrue(result2.isEmpty());
    }

    // ======== Double List Parsing Tests ========

    @Test
    void testParseDoubleList_ValidInput() {
        logTestHeader("Double List Parsing - Valid Input");

        List<Double> result = AIResponseParser.parseDoubleList("1.5,2.7,3.14");
        System.out.printf("Input: %-40s | Result: %s%n", "1.5,2.7,3.14", result);
        assertEquals(3, result.size());
        assertEquals(1.5, result.get(0), 0.001);
        assertEquals(2.7, result.get(1), 0.001);
        assertEquals(3.14, result.get(2), 0.001);
    }

    @Test
    void testParseDoubleList_MixedValidInvalid() {
        logTestHeader("Double List Parsing - Mixed Valid/Invalid");

        List<Double> result = AIResponseParser.parseDoubleList("1.5,invalid,3.14");
        System.out.printf("Input: %-40s | Result: %s (invalid filtered)%n", "1.5,invalid,3.14", result);
        assertEquals(2, result.size());
        assertEquals(1.5, result.get(0), 0.001);
        assertEquals(3.14, result.get(1), 0.001);
    }

    @Test
    void testParseDoubleList_VariousDelimiters() {
        logTestHeader("Double List Parsing - Various Delimiters");

        List<Double> result1 = AIResponseParser.parseDoubleList("1.1;2.2;3.3");
        System.out.printf("Input (semicolon): %-40s | Result: %s%n", "1.1;2.2;3.3", result1);
        assertEquals(3, result1.size());

        List<Double> result2 = AIResponseParser.parseDoubleList("10.5 20.5 30.5");
        System.out.printf("Input (space):     %-40s | Result: %s%n", "10.5 20.5 30.5", result2);
        assertEquals(3, result2.size());
    }

    @Test
    void testParseDoubleList_NegativeNumbers() {
        logTestHeader("Double List Parsing - Negative Numbers");

        List<Double> result = AIResponseParser.parseDoubleList("-1.5,-10.5,50.5");
        System.out.printf("Input: %-40s | Result: %s%n", "-1.5,-10.5,50.5", result);
        assertEquals(3, result.size());
        assertEquals(-1.5, result.get(0), 0.001);
        assertEquals(-10.5, result.get(1), 0.001);
        assertEquals(50.5, result.get(2), 0.001);
    }

    @Test
    void testParseDoubleList_EmptyOrNull() {
        logTestHeader("Double List Parsing - Empty/Null");

        List<Double> result1 = AIResponseParser.parseDoubleList(null);
        System.out.printf("Input: %-40s | Result: %s%n", "null", result1);
        assertTrue(result1.isEmpty());

        List<Double> result2 = AIResponseParser.parseDoubleList("");
        System.out.printf("Input: %-40s | Result: %s%n", "\"\"", result2);
        assertTrue(result2.isEmpty());
    }

    // ======== Edge Cases and Boundary Tests ========

    @Test
    void testParseInt_MaxMinValues() {
        logTestHeader("Edge Cases - Integer Max/Min Values");

        int max = AIResponseParser.parseInt(String.valueOf(Integer.MAX_VALUE), 0);
        System.out.printf("parseInt(MAX_VALUE): %d | Expected: %d | %s%n", max, Integer.MAX_VALUE, max == Integer.MAX_VALUE ? "✓" : "✗");
        assertEquals(Integer.MAX_VALUE, max);

        int min = AIResponseParser.parseInt(String.valueOf(Integer.MIN_VALUE), 0);
        System.out.printf("parseInt(MIN_VALUE): %d | Expected: %d | %s%n", min, Integer.MIN_VALUE, min == Integer.MIN_VALUE ? "✓" : "✗");
        assertEquals(Integer.MIN_VALUE, min);
    }

    @Test
    void testParseLong_MaxMinValues() {
        logTestHeader("Edge Cases - Long Max/Min Values");

        long max = AIResponseParser.parseLong(String.valueOf(Long.MAX_VALUE), 0L);
        System.out.printf("parseLong(MAX_VALUE): %d | Expected: %d | %s%n", max, Long.MAX_VALUE, max == Long.MAX_VALUE ? "✓" : "✗");
        assertEquals(Long.MAX_VALUE, max);

        long min = AIResponseParser.parseLong(String.valueOf(Long.MIN_VALUE), 0L);
        System.out.printf("parseLong(MIN_VALUE): %d | Expected: %d | %s%n", min, Long.MIN_VALUE, min == Long.MIN_VALUE ? "✓" : "✗");
        assertEquals(Long.MIN_VALUE, min);
    }

    @Test
    void testParseDouble_SpecialValues() {
        logTestHeader("Edge Cases - Double Special Values");

        double max = AIResponseParser.parseDouble(String.valueOf(Double.MAX_VALUE), 0.0);
        System.out.printf("parseDouble(MAX_VALUE): %e | Expected: %e%n", max, Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, max, 0.001);

        double zero = AIResponseParser.parseDouble("0.0", 1.0);
        System.out.printf("parseDouble(0.0):       %.1f | Expected: %.1f | %s%n", zero, 0.0, zero == 0.0 ? "✓" : "✗");
        assertEquals(0.0, zero, 0.001);
    }

    @Test
    void testParseString_Whitespace() {
        logTestHeader("Edge Cases - String Whitespace Handling");

        String result1 = AIResponseParser.parseString("  a b c  ", "fallback");
        System.out.printf("parseString(\"  a b c  \"): \"%s\"%n", result1);
        assertEquals("a b c", result1);

        String result2 = AIResponseParser.parseString("tab\ttab", "fallback");
        System.out.printf("parseString(\"tab\\ttab\"): \"%s\"%n", result2);
        assertEquals("tab\ttab", result2);
    }

    @Test
    void testParseBoundedString_ExactBoundary() {
        logTestHeader("Edge Cases - Bounded String Exact Boundary");

        String result1 = AIResponseParser.parseBoundedString("12345", 5, "fallback");
        System.out.printf("parseBoundedString(\"12345\", max=5): \"%s\" (length=%d)%n", result1, result1.length());
        assertEquals("12345", result1);

        String result2 = AIResponseParser.parseBoundedString("123456", 5, "fallback");
        System.out.printf("parseBoundedString(\"123456\", max=5): \"%s\" (exceeded, fallback used)%n", result2);
        assertEquals("fallback", result2);
    }
}
