package org.ninjax.test;

import static com.google.common.truth.Truth.assertThat;

import java.util.Collection;
import java.util.Map;

/**
 * General utility assertions for testing.
 *
 * Example usage:
 * <pre>
 * TestAssertions.assertEmpty(list);
 * TestAssertions.assertSize(list, 5);
 * TestAssertions.assertContains(list, "item");
 * </pre>
 */
public class TestAssertions {

    private TestAssertions() {
        // Utility class
    }

    /**
     * Assert that a collection is empty.
     *
     * @param collection The collection to check
     */
    public static void assertEmpty(Collection<?> collection) {
        assertThat(collection).isEmpty();
    }

    /**
     * Assert that a collection has the expected size.
     *
     * @param collection The collection to check
     * @param expectedSize The expected size
     */
    public static void assertSize(Collection<?> collection, int expectedSize) {
        assertThat(collection).hasSize(expectedSize);
    }

    /**
     * Assert that a collection contains the given element.
     *
     * @param collection The collection to check
     * @param element The element to find
     */
    public static void assertContains(Collection<?> collection, Object element) {
        assertThat(collection).contains(element);
    }

    /**
     * Assert that a map is empty.
     *
     * @param map The map to check
     */
    public static void assertEmpty(Map<?, ?> map) {
        assertThat(map).isEmpty();
    }

    /**
     * Assert that a map has the expected size.
     *
     * @param map The map to check
     * @param expectedSize The expected size
     */
    public static void assertSize(Map<?, ?> map, int expectedSize) {
        assertThat(map).hasSize(expectedSize);
    }

    /**
     * Assert that a map contains the given key.
     *
     * @param map The map to check
     * @param key The key to find
     */
    public static void assertContainsKey(Map<?, ?> map, Object key) {
        assertThat(map).containsKey(key);
    }

    /**
     * Assert that a string contains the given substring.
     *
     * @param actual The actual string
     * @param substring The substring to find
     */
    public static void assertContains(String actual, String substring) {
        assertThat(actual).contains(substring);
    }

    /**
     * Assert that a string starts with the given prefix.
     *
     * @param actual The actual string
     * @param prefix The expected prefix
     */
    public static void assertStartsWith(String actual, String prefix) {
        assertThat(actual).startsWith(prefix);
    }

    /**
     * Assert that a string ends with the given suffix.
     *
     * @param actual The actual string
     * @param suffix The expected suffix
     */
    public static void assertEndsWith(String actual, String suffix) {
        assertThat(actual).endsWith(suffix);
    }

    /**
     * Assert that a value is null.
     *
     * @param actual The actual value
     */
    public static void assertNull(Object actual) {
        assertThat(actual).isNull();
    }

    /**
     * Assert that a value is not null.
     *
     * @param actual The actual value
     */
    public static void assertNotNull(Object actual) {
        assertThat(actual).isNotNull();
    }
}
