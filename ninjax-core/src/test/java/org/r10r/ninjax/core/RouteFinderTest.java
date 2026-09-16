package org.r10r.ninjax.core;

import static com.google.common.truth.Truth.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RouteFinderTest {

    private static final Router.ControllerMethod GET_CONTROLLER = request -> Result.ok("get");
    private static final Router.ControllerMethod HEAD_CONTROLLER = request -> Result.ok("head");

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD"})
    void eachHttpMethod_findsItsOwnRoute(String httpMethod) {
        // given one route per method on the same path
        Router router = new Router();
        router.GET("/tasks/{id}").with(request -> Result.ok());
        router.POST("/tasks/{id}").with(request -> Result.ok());
        router.PUT("/tasks/{id}").with(request -> Result.ok());
        router.PATCH("/tasks/{id}").with(request -> Result.ok());
        router.DELETE("/tasks/{id}").with(request -> Result.ok());
        router.HEAD("/tasks/{id}").with(request -> Result.ok());

        // when
        Optional<RouteFinder.RouteMatch> route = new RouteFinder(router).getRouteFor(httpMethod, "/tasks/42");

        // then
        assertThat(route.isPresent()).isTrue();
        assertThat(route.get().route().httpMethod()).isEqualTo(httpMethod);
    }

    @Test
    void methodWithoutRoute_findsNothing() {
        // given only a GET route
        Router router = new Router();
        router.GET("/tasks").with(GET_CONTROLLER);

        // when
        Optional<RouteFinder.RouteMatch> route = new RouteFinder(router).getRouteFor("DELETE", "/tasks");

        // then
        assertThat(route.isPresent()).isFalse();
    }

    @Test
    void headRequest_withoutHeadRoute_fallsBackToGetRoute() {
        // given only a GET route
        Router router = new Router();
        router.GET("/tasks").with(GET_CONTROLLER);

        // when
        Optional<RouteFinder.RouteMatch> route = new RouteFinder(router).getRouteFor("HEAD", "/tasks");

        // then
        assertThat(route.isPresent()).isTrue();
        assertThat(route.get().route().controllerMethod()).isSameInstanceAs(GET_CONTROLLER);
    }

    @Test
    void headRequest_withExplicitHeadRoute_winsOverGetRoute() {
        // given a GET route registered before an explicit HEAD route on the same path
        Router router = new Router();
        router.GET("/tasks").with(GET_CONTROLLER);
        router.HEAD("/tasks").with(HEAD_CONTROLLER);

        // when
        Optional<RouteFinder.RouteMatch> route = new RouteFinder(router).getRouteFor("HEAD", "/tasks");

        // then
        assertThat(route.isPresent()).isTrue();
        assertThat(route.get().route().controllerMethod()).isSameInstanceAs(HEAD_CONTROLLER);
    }

    @Test
    void headRequest_withoutMatchingGetRoute_findsNothing() {
        // given only a POST route
        Router router = new Router();
        router.POST("/tasks").with(request -> Result.ok());

        // when
        Optional<RouteFinder.RouteMatch> route = new RouteFinder(router).getRouteFor("HEAD", "/tasks");

        // then
        assertThat(route.isPresent()).isFalse();
    }

    @Test
    void getRequest_doesNotFallBackToHeadRoute() {
        // given only a HEAD route
        Router router = new Router();
        router.HEAD("/tasks").with(HEAD_CONTROLLER);

        // when
        Optional<RouteFinder.RouteMatch> route = new RouteFinder(router).getRouteFor("GET", "/tasks");

        // then
        assertThat(route.isPresent()).isFalse();
    }
}
