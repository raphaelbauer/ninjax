package org.r10r.ninjax.core.jwt;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class JwtJsonTest {

    @Test
    void serializesMinifiedAndEscaped() {
        // given
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("text", "a\"b\\c\nd\u0001");
        object.put("number", 42L);
        object.put("flag", false);

        // when
        String json = JwtJson.serialize(object);

        // then
        assertThat(json).isEqualTo("{\"text\":\"a\\\"b\\\\c\\nd\\u0001\",\"number\":42,\"flag\":false}");
    }

    @Test
    void parsesWhatItSerializes() {
        // given
        Map<String, Object> object = new LinkedHashMap<>();
        object.put("text", "a\"b\\c\nd\u0001ü");
        object.put("small", 7);
        object.put("big", 4102444800L);
        object.put("flag", true);

        // when
        Map<String, Object> parsed = JwtJson.parseObject(JwtJson.serialize(object));

        // then
        assertThat(parsed).isEqualTo(object);
    }

    @Test
    void rejectsArrays() {
        // when
        JwtException exception = assertThrows(JwtException.class, () -> JwtJson.parseObject("{\"a\":[1,2,3]}"));

        // then
        assertThat(exception).hasMessageThat().contains("arrays");
    }

    @Test
    void rejectsTopLevelValueThatIsNotAnObject() {
        // when
        JwtException exception = assertThrows(JwtException.class, () -> JwtJson.parseObject("\"just a string\""));

        // then
        assertThat(exception).hasMessageThat().contains("Expected JSON object");
    }

    @Test
    void rejectsDuplicateKeys() {
        // when
        JwtException exception = assertThrows(JwtException.class, () -> JwtJson.parseObject("{\"a\":1,\"a\":2}"));

        // then
        assertThat(exception).hasMessageThat().contains("Duplicate JSON key");
    }

    @Test
    void rejectsTrailingContent() {
        // when
        JwtException exception = assertThrows(JwtException.class, () -> JwtJson.parseObject("{\"a\":1}{\"a\":2}"));

        // then
        assertThat(exception).hasMessageThat().contains("Trailing JSON content");
    }

    @Test
    void rejectsDeepNesting() {
        // given
        String json = "{}";
        for (int depth = 0; depth < 12; depth++) {
            json = "{\"a\":" + json + "}";
        }
        String tooDeep = json;

        // when
        JwtException exception = assertThrows(JwtException.class, () -> JwtJson.parseObject(tooDeep));

        // then
        assertThat(exception).hasMessageThat().contains("nesting too deep");
    }

    @Test
    void rejectsTooManyKeys() {
        // given
        StringBuilder json = new StringBuilder("{");
        for (int key = 0; key < 257; key++) {
            json.append(key == 0 ? "" : ",").append("\"k").append(key).append("\":1");
        }
        json.append("}");

        // when
        JwtException exception = assertThrows(JwtException.class, () -> JwtJson.parseObject(json.toString()));

        // then
        assertThat(exception).hasMessageThat().contains("Too many JSON object keys");
    }

    @Test
    void rejectsVeryLongString() {
        // given
        String json = "{\"a\":\"" + "z".repeat(3000) + "\"}";

        // when
        JwtException exception = assertThrows(JwtException.class, () -> JwtJson.parseObject(json));

        // then
        assertThat(exception).hasMessageThat().contains("string too long");
    }

    @Test
    void rejectsBadUnicodeEscape() {
        // when
        JwtException exception = assertThrows(JwtException.class, () -> JwtJson.parseObject("{\"a\":\"\\uZZZZ\"}"));

        // then
        assertThat(exception).hasMessageThat().contains("Bad unicode escape");
    }
}
