package org.r10r.ninjax.core;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testhelper.RequestTestHelper;

class AssetsControllerTest {

    private AssetsController controller;
    private Router router;

    @BeforeEach
    void setUp() {
        controller = new AssetsController();
        router = new Router();
    }

    @Test
    void serveStatic_existingFileViaPathParam_returns200_andStreamsContent_andSetsMimeType() throws Exception {
        // Route has a path parameter {fileName} which Request will derive.
        Request req = RequestTestHelper.createTestRequest(
                router,
                "/assets/{fileName}",
                "GET",
                "/assets/hello.txt"
        );

        Result r = controller.serveStatic(req);

        assertThat(r.status()).isEqualTo(Result.SC_200_OK);
        assertThat(r.contentType()).isEqualTo("text/plain");
        assertThat(r.outputStreamRenderer()).isPresent();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        r.outputStreamRenderer().get().streamTo(baos);

        assertThat(baos.toString(StandardCharsets.UTF_8)).isEqualTo("hello world");
    }

    @Test
    void serveStatic_existingFileInSubdirectory_returns200_andStreamsContent() throws Exception {
        Request req = RequestTestHelper.createTestRequest(
                router,
                "/assets/{fileName}",
                "GET",
                "/assets/sub/inner.txt"
        );

        Result r = controller.serveStatic(req);

        assertThat(r.status()).isEqualTo(Result.SC_200_OK);
        assertThat(r.outputStreamRenderer()).isPresent();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        r.outputStreamRenderer().get().streamTo(baos);

        assertThat(baos.toString(StandardCharsets.UTF_8)).isEqualTo("inner");
    }

    @Test
    void serveStatic_missingFile_returns404_andNoRenderer() {
        Request req = RequestTestHelper.createTestRequest(
                router,
                "/assets/{fileName}",
                "GET",
                "/assets/does-not-exist.txt"
        );

        Result r = controller.serveStatic(req);

        assertThat(r.status()).isEqualTo(Result.SC_404_NOT_FOUND);
        assertThat(r.outputStreamRenderer()).isEmpty();
    }

    @Test
    void serveStatic_directoryTraversalAttempt_returns404() {
        // controller should reject `../` in the resolved path
        Request req = RequestTestHelper.createTestRequest(
                router,
                "/assets/{fileName}",
                "GET",
                "/assets/../secret.txt"
        );

        Result r = controller.serveStatic(req);

        assertThat(r.status()).isEqualTo(Result.SC_404_NOT_FOUND);
        assertThat(r.outputStreamRenderer()).isEmpty();
    }

    @Test
    void serveStatic_fallsBackToRequestPath_whenNoPathParam() throws Exception {
        // Use a route without path params so getPathParameter("fileName") is empty
        Request req = RequestTestHelper.createTestRequest(
                router,
                "/favicon.ico",
                "GET",
                "/favicon.ico" // AssetsController will use requestPath as fallback
        );

        // For this to pass, put favicon.ico under /assets/ in test resources
        Result r = controller.serveStatic(req);

        assertThat(r.status()).isEqualTo(Result.SC_200_OK);
        assertThat(r.outputStreamRenderer()).isPresent();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        r.outputStreamRenderer().get().streamTo(baos);

        // adapt assertion to whatever content you place in assets/favicon.ico
        String body = baos.toString(StandardCharsets.UTF_8);
        assertThat(body).isNotEmpty();
    }
}
