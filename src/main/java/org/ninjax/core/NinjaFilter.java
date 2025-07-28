package org.ninjax.core;


public interface NinjaFilter {
    Result doFilter(Context context, FilterChain chain);
}