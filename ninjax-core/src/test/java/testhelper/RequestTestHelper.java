package testhelper;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import com.ninjaxframework.core.PathParameterExtractor;
import com.ninjaxframework.core.Request;
import com.ninjaxframework.core.Result;
import com.ninjaxframework.core.Router;

public class RequestTestHelper {

    public static Request createTestRequest(
            Router router,
            String routePath,
            String httpMethod,
            String requestPath
    ) {
        // Build a temporary route to extract path parameters
        Router.Route route = router.new Route(
                httpMethod,
                routePath,
                req -> Result.ok(), // dummy; not used by AssetsController
                List.of()
        );

        // Extract path parameters using utility
        var pathParams = PathParameterExtractor.extractPathParameters(
                route.pathRegex(),
                route.parameters,
                requestPath
        );

        // Minimal request body; not used by AssetsController, but builder requires it.
        Request.InputStreamGetter inputStreamGetter =
                () -> new ByteArrayInputStream(new byte[0]);

        Request.FileItemGetter fileItemGetter = fieldName -> Optional.empty();
        Request.FileItemsGetter fileItemsGetter = fieldName -> List.of();

        var payload = new com.ninjaxframework.core.Request.Payload(Map.of());

        return Request.builder()
                .requestPath(requestPath)
                .pathParameters(pathParams)
                .inputStreamGetter(inputStreamGetter)
                .fileItemGetter(fileItemGetter)
                .fileItemsGetter(fileItemsGetter)
                .ninjaCookies(List.of())
                .payload(payload)
                .headers(new com.ninjaxframework.core.Request.Headers())
                .parameters(new com.ninjaxframework.core.Request.Parameters())
                .ninjaSession(Optional.empty())
                .language(Locale.ENGLISH)
                .build();
    }

}
