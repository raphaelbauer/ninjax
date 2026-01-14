package org.ninja.core;


public interface NinjaFilter {
    Result doFilter(Request request, FilterChain chain);
}