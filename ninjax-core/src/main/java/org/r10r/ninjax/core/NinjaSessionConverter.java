package org.r10r.ninjax.core;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.r10r.ninjax.core.jwt.Jwts;
import org.r10r.ninjax.core.properties.NinjaProperties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NinjaSessionConverter {

    private static final Logger logger = Logger.getLogger(NinjaSessionConverter.class.getName());

    public static final String NINJA_SESSION_COOKIE_NAME = "NINJA_SESSION";
    private static final String NINJA_SESSION_PATH = "/";
    
    
    private final Optional<Long> sessionExpiryTimeInSeconds;
    private final boolean sessionCookieSecure;
    
    private final SecretKey secretKeyForSessionEncryption;
    
    
    public NinjaSessionConverter(NinjaProperties ninjaProperties) {
        
        String encodedSecret = ninjaProperties.get(NinjaConstants.NINJA_APPLICATION_SECRET_KEY).orElseThrow(() -> {
            return new RuntimeException(String.format("Missing key '%s' in 'conf/application.conf'. Can't start without a secret. Please create the secret using e.g. 'mvn ninja:generateSecret'.", NinjaConstants.NINJA_APPLICATION_SECRET_KEY));
        });

        byte[] decodedKey = Base64.getDecoder().decode(encodedSecret);
        this.secretKeyForSessionEncryption = new SecretKeySpec(decodedKey, 0, decodedKey.length, "HmacSHA256");

        this.sessionExpiryTimeInSeconds = ninjaProperties.get("application.session.expire_time_in_seconds").map(v -> Long.valueOf(v));
        this.sessionCookieSecure = ninjaProperties.get("application.session.cookie.secure")
                .map(v -> Boolean.parseBoolean(v))
                .orElse(true); // Default to true (secure) if not specified
    }

    public Optional<NinjaSession> extractSessionFromCookie(NinjaCookie ninjaSessionCookie) {

        var now = System.currentTimeMillis();

        try {
            var claims = Jwts.parser()
                    .verifyWith(secretKeyForSessionEncryption)
                    .build()
                    .parseSignedClaims(ninjaSessionCookie.value())
                    .getPayload();

            if (claims.getNotBefore() != null /* Not our Api. We have to do a null check :( */
                    && now < claims.getNotBefore().getTime()) {
                return Optional.empty();
            }

            if (claims.getExpiration() != null /* Not our Api. We have to do a null check :( */
                    && now > claims.getExpiration().getTime()) {
                return Optional.empty();
            }

            Map<String, String> sessionMap = new HashMap<>();
            for (var e : claims.entrySet()) {
                sessionMap.put(e.getKey(), e.getValue().toString());
            }
            var ninjaSession = new NinjaSession(sessionMap);

            return Optional.of(ninjaSession);
        } catch (Exception e) {
            logger.log(Level.FINE, "Opsi. Error parsing Ninja Session. I am ignoring this session.", e);
            return Optional.empty();
        }

    }

    public NinjaCookie createCookieToRemoveNinjaSession() {
        int REMOVE_SESSION_MAX_AGE = 0;
        var cookie = new NinjaCookie(
                NINJA_SESSION_COOKIE_NAME,
                "",
                Optional.empty(),
                REMOVE_SESSION_MAX_AGE,
                Optional.of(NINJA_SESSION_PATH),
                sessionCookieSecure ? Secure.Yes : Secure.No,
                HttpOnly.Yes);

        return cookie;
    }

    public NinjaCookie createCookieWithInformationOfNinjaSession(NinjaSession ninjaSession) {

        // some setup
        Instant now = Instant.now();

        Optional<Instant> expiryInstant = Optional.empty();

        if (ninjaSession.get("exp").isPresent()) {
            expiryInstant = Optional.of(Instant.ofEpochSecond(Long.parseLong(ninjaSession.get("exp").get())));
        }

        if (expiryInstant.isEmpty() && sessionExpiryTimeInSeconds.isPresent()) {
            expiryInstant = Optional.of(now.plusSeconds(sessionExpiryTimeInSeconds.get()));
        }

        // build jwt
        var nowDate = Date.from(now);
        var jwsBuilder = Jwts.builder()
                .notBefore(nowDate)
                .issuedAt(nowDate);

        expiryInstant.ifPresent(i -> jwsBuilder.expiration(Date.from(i)));

        String jws = jwsBuilder
                .claims(ninjaSession.keyValueStore())
                .signWith(secretKeyForSessionEncryption)
                .compact();

        var maxAge = expiryInstant.map(i -> (int) Duration.between(now, i).getSeconds())
                .orElse(0); // 0 is a session cookie

        //build cookie from jwt
        var cookie = new NinjaCookie(
                NINJA_SESSION_COOKIE_NAME,
                jws,
                Optional.empty(),
                maxAge,
                Optional.of(NINJA_SESSION_PATH),
                sessionCookieSecure ? Secure.Yes : Secure.No,
                HttpOnly.Yes);

        return cookie;
    }

}
