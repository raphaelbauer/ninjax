package com.ninjaxframework.test;

import java.util.*;

/**
 * Builder for form data and query parameters.
 *
 * Example usage:
 * <pre>
 * Map&lt;String, String[]&gt; params = ParameterBuilder.create()
 *     .add("title", "Task 1")
 *     .add("completed", "true")
 *     .buildArray();
 *
 * Map&lt;String, String&gt; simpleParams = ParameterBuilder.create()
 *     .add("title", "Task 1")
 *     .buildSimple();
 * </pre>
 */
public class ParameterBuilder {

    private final Map<String, List<String>> parameters = new LinkedHashMap<>();

    private ParameterBuilder() {
    }

    /**
     * Create a new ParameterBuilder.
     *
     * @return A new builder instance
     */
    public static ParameterBuilder create() {
        return new ParameterBuilder();
    }

    /**
     * Add a parameter.
     *
     * @param name Parameter name
     * @param value Parameter value
     * @return This builder
     */
    public ParameterBuilder add(String name, String value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        parameters.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return this;
    }

    /**
     * Add multiple values for a parameter.
     *
     * @param name Parameter name
     * @param values Parameter values
     * @return This builder
     */
    public ParameterBuilder add(String name, String... values) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(values, "values");
        parameters.computeIfAbsent(name, k -> new ArrayList<>()).addAll(Arrays.asList(values));
        return this;
    }

    /**
     * Build as a Map&lt;String, String[]&gt; (standard servlet format).
     *
     * @return Parameter map with string arrays
     */
    public Map<String, String[]> buildArray() {
        Map<String, String[]> result = new LinkedHashMap<>();
        for (var entry : parameters.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toArray(new String[0]));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Build as a Map&lt;String, String&gt; (simple single-value format).
     * If a parameter has multiple values, only the first one is used.
     *
     * @return Simple parameter map
     */
    public Map<String, String> buildSimple() {
        Map<String, String> result = new LinkedHashMap<>();
        for (var entry : parameters.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                result.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Build as a Map&lt;String, List&lt;String&gt;&gt; (multi-value format).
     *
     * @return Parameter map with lists
     */
    public Map<String, List<String>> buildList() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (var entry : parameters.entrySet()) {
            result.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }
}
