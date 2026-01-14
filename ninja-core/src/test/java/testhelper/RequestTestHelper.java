package testhelper;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.ninja.core.Request;
import org.ninja.core.Result;
import org.ninja.core.Router;

public class RequestTestHelper {

    public static Request createTestRequest(
            Router router,
            String routePath,
            String httpMethod,
            String requestPath
    ) {
        // Build a Route for this test; controllerMethod is unused here.
        Router.Route route = router.new Route(
                httpMethod,
                routePath,
                req -> Result.ok(), // dummy; not used by AssetsController
                List.of()
        );

        // Minimal request body; not used by AssetsController, but builder requires it.
        Request.InputStreamGetter inputStreamGetter =
                () -> new ByteArrayInputStream(new byte[0]);

        Request.FileItemGetter fileItemGetter = fieldName -> Optional.empty();
        Request.FileItemsGetter fileItemsGetter = fieldName -> List.of();

        var payload = new org.ninja.core.Request.Payload(Map.of());
        
        return Request.builder()
                .route(route)
                .requestPath(requestPath)
                .inputStreamGetter(inputStreamGetter)
                .fileItemGetter(fileItemGetter)
                .fileItemsGetter(fileItemsGetter)
                .ninjaCookies(List.of())
                .payload(payload)
                .headers(Map.of())
                .parameters(Map.of())
                .ninjaSession(Optional.empty())
                .language(Locale.ENGLISH)
                .build();
    }

}
