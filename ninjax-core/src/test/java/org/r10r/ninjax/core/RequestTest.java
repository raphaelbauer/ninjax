package org.r10r.ninjax.core;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class RequestTest {

    @Test
    void getFile_returnsFirstFileOfField() {
        // given
        FileItem first = fileItem("first.txt");
        FileItem second = fileItem("second.txt");
        Request request = minimalBuilder()
                .fileItemsGetter(fieldName -> fieldName.equals("upload") ? List.of(first, second) : List.of())
                .build();

        // when
        Optional<FileItem> file = request.getFile("upload");

        // then
        assertThat(file).hasValue(first);
        assertThat(request.getFiles("upload")).containsExactly(first, second).inOrder();
    }

    @Test
    void getFile_returnsEmptyWhenFieldHasNoFiles() {
        // given
        Request request = minimalBuilder().build();

        // when
        Optional<FileItem> file = request.getFile("upload");

        // then
        assertThat(file).isEmpty();
    }

    @Test
    void toBuilder_keepsLocale() {
        // given
        Request request = minimalBuilder().locale(Locale.GERMAN).build();

        // when
        Request copy = request.toBuilder().requestPath("/other").build();

        // then
        assertThat(copy.getLocale()).isEqualTo(Locale.GERMAN);
        assertThat(copy.getRequestPath()).isEqualTo("/other");
    }

    @Test
    void build_withoutRequiredValue_throws() {
        // given
        Request.Builder builder = minimalBuilder().locale(null);

        // when
        NullPointerException exception = assertThrows(NullPointerException.class, builder::build);

        // then
        assertThat(exception).hasMessageThat().isEqualTo("locale must not be null");
    }

    private static Request.Builder minimalBuilder() {
        return Request.builder()
                .requestPath("/")
                .inputStreamGetter(() -> new ByteArrayInputStream(new byte[0]))
                .fileItemsGetter(fieldName -> List.of())
                .headers(new Request.Headers())
                .parameters(new Request.Parameters());
    }

    private static FileItem fileItem(String fileName) {
        return new FileItem(fileName, "text/plain", 0, new ByteArrayInputStream(new byte[0]));
    }
}
