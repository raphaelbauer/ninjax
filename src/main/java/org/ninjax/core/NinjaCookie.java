package org.ninjax.core;

import java.util.Optional;

public record NinjaCookie(
        String name,
        String value,
        Optional<String> comment,
        Optional<String> domain,
        int maxAge,
        Optional<String> path,
        boolean secure,
        boolean httpOnly) {
    
}
