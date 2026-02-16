package com.ninjaxframework.core;


public interface NinjaFilter {
    Result doFilter(Request request, FilterChain chain);
}