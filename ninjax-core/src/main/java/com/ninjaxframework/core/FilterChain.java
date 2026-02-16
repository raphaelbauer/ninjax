
package com.ninjaxframework.core;

import java.util.List;

public class FilterChain {
    private final List<NinjaFilter> filters;
    private final int index;
    private final Router.ControllerMethod controller;

    public FilterChain(List<NinjaFilter> filters, int index, Router.ControllerMethod controller) {
        this.filters = filters;
        this.index = index;
        this.controller = controller;
    }

    public Result doFilter(Request request) {
        if (index < filters.size()) {
            return filters.get(index).doFilter(request, new FilterChain(filters, index + 1, controller));
        } else {
            return controller.executeMethod(request);
        }
    }
}
