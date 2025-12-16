package org.ninjax.core;

import java.util.Optional;
import static org.ninjax.core.Secure.Yes;

public record NinjaCookie(
        String name,
        String value,
        Optional<String> comment,
        Optional<String> domain,
        int maxAge,
        Optional<String> path,
        Secure secure,
        HttpOnly httpOnly) {
    
}

enum Secure { 
    Yes, No;
    
    public static Secure ofBoolean(boolean secure) {
        if (secure) {
            return Secure.Yes;
        } else {
            return Secure.No;
        }
    }
    
    public boolean toBoolean() {
        if (this.equals(Yes)) {
            return true;
        } else {
            return false;
        }     
    }
}
enum HttpOnly { 
    
    Yes, No;

    public static HttpOnly ofBoolean(boolean httpOnly) {
        if (httpOnly) {
            return HttpOnly.Yes;
        } else {
            return HttpOnly.No;
        }
    }
    
    public boolean toBoolean() {
        if (this.equals(Yes)) {
            return true;
        } else {
            return false;
        }     
    }

}

