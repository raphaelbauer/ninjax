package org.r10r.ninjax.core;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RouterTest {

    private final Router router = new Router();

    @Test
    void parameterNames_areListedInOrder_withAndWithoutRegex() {
        // when
        Router.Route route = router.new Route("GET", "/users/{id: [0-9]+}/posts/{slug}", request -> Result.ok(), List.of());

        // then
        assertThat(route.parameterNames).containsExactly("id", "slug").inOrder();
    }

    @Test
    void routeWithoutParameters_hasNoParameterNames() {
        // when
        Router.Route route = router.new Route("GET", "/tasks", request -> Result.ok(), List.of());

        // then
        assertThat(route.parameterNames).isEmpty();
    }
}
