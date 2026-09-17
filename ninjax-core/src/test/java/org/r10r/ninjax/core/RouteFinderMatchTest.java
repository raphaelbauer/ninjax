package org.r10r.ninjax.core;

import static com.google.common.truth.Truth.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RouteFinderMatchTest {

    @Test
    void match_containsRouteAndItsPathParameters() {
        // given
        Router router = new Router();
        router.GET("/users").with(request -> Result.ok("list"));
        router.GET("/users/{id: [0-9]+}/posts/{slug}").with(request -> Result.ok("post"));
        RouteFinder routeFinder = new RouteFinder(router);

        // when
        var match = routeFinder.getRouteFor("GET", "/users/42/posts/hello-world").orElseThrow();

        // then
        assertThat(match.route().path()).isEqualTo("/users/{id: [0-9]+}/posts/{slug}");
        assertThat(match.pathParameters()).containsExactly("id", "42", "slug", "hello-world");
    }

    @Test
    void routeWithoutParameters_hasEmptyPathParameters() {
        // given
        Router router = new Router();
        router.POST("/tasks").with(request -> Result.ok());

        // when
        var match = new RouteFinder(router).getRouteFor("POST", "/tasks").orElseThrow();

        // then
        assertThat(match.pathParameters()).isEqualTo(Map.of());
    }

    @Test
    void noMatchingRoute_isEmpty() {
        // given
        Router router = new Router();
        router.GET("/users/{id: [0-9]+}").with(request -> Result.ok());

        // when / then
        assertThat(new RouteFinder(router).getRouteFor("GET", "/users/abc")).isEmpty();
        assertThat(new RouteFinder(router).getRouteFor("POST", "/users/1")).isEmpty();
    }
}
