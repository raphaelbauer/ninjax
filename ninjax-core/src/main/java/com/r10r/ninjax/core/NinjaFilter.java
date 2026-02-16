package com.r10r.ninjax.core;


public interface NinjaFilter {
    Result doFilter(Request request, FilterChain chain);
}