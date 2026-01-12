package org.ninjax.core;


public interface NinjaFilter {
    Result doFilter(Request request, FilterChain chain);
}