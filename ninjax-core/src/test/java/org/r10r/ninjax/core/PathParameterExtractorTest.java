package org.r10r.ninjax.core;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PathParameterExtractorTest {

    private static Router.Route route(String path) {
        // ControllerMethod is irrelevant for extraction; a no-op lambda is enough.
        return new Router().new Route("GET", path, request -> null, List.of());
    }

    private static Map<String, String> extract(String path, String uri) {
        Router.Route route = route(path);
        return PathParameterExtractor.extractPathParameters(route.pathRegex(), route.parameters, uri);
    }

    @Test
    void simpleParameter_extractsValue() {
        // given
        // when
        Map<String, String> params = extract("/user/{id}", "/user/42");

        // then
        assertThat(params).containsExactly("id", "42");
    }

    @Test
    void regexConstrainedParameter_matchesDigits() {
        // given
        // when
        Map<String, String> params = extract("/api/{id: [0-9]+}", "/api/123");

        // then
        assertThat(params).containsExactly("id", "123");
    }

    @Test
    void regexConstrainedParameter_rejectsNonMatchingUri() {
        // given a route whose regex only accepts digits
        // when the uri does not match the constraint
        Map<String, String> params = extract("/api/{id: [0-9]+}", "/api/abc");

        // then nothing is extracted (the regex did not match)
        assertThat(params).isEmpty();
    }

    @Test
    void multipleParameters_mapEachNameToItsValue() {
        // given
        // when
        Map<String, String> params =
                extract("/user/{id: [0-9]+}/email/{addr}", "/user/7/email/joe@example.com");

        // then
        assertThat(params).containsExactly("id", "7", "addr", "joe@example.com");
    }

    @Test
    void userRegexWithMultipleCapturingGroups_doesNotBreakExtraction() {
        // given a user regex containing multiple capturing groups (regression for finding #6)
        // when matching a uri that satisfies it
        Map<String, String> params = extract("/api/{id: (a|b)(c|d)}", "/api/ac");

        // then the whole match is bound to the single parameter, with no crash or mismatch
        assertThat(params).containsExactly("id", "ac");
    }

    @Test
    void userRegexWithNestedCapturingGroups_doesNotBreakExtraction() {
        // given a user regex with nested capturing groups
        // when matching a uri that satisfies it
        Map<String, String> params = extract("/api/{id: ((a)(b))}", "/api/ab");

        // then the full match maps to the parameter name
        assertThat(params).containsExactly("id", "ab");
    }

    @Test
    void capturingGroupsAcrossMultipleParameters_keepNamesAligned() {
        // given two parameters where the first carries its own capturing groups
        // when matching
        Map<String, String> params =
                extract("/x/{a: (a|b)(c|d)}/y/{b}", "/x/bd/y/tail");

        // then each name maps to its own segment, unaffected by inner groups
        assertThat(params).containsExactly("a", "bd", "b", "tail");
    }
}
