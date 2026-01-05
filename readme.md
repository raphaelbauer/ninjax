# NinjaX

[![Java CI with Maven](https://github.com/raphaelbauer/ninjax/actions/workflows/maven.yml/badge.svg)](https://github.com/raphaelbauer/ninjax/actions/workflows/maven.yml)

## Intro

This is an experiment on how a Java web framework in 2026 would look like.

## Thoughts on how we started with the Ninja Web Framework in 2012

When we started in 2012 the Java world was differnt - it was Java 7 at that time.

- EJBs, WAR and Enterprise Java was still a thing
- Spring Boot did not exist back then.
- Servlets were important
- Optional did not exist and Null was used widely in code. 
- Mutability was the norm
- The stream Api did not exist back then.
- Easy-to-use Lambdas did not exist back then
- Records did not exist
- Messages did not support utf8 leading to complex code
- Even multiline strings did not exist

Huhu. But times have changed. And a web Java webframework for 2026 will look very much different to anything that
has been created in 2012.

## Goals and Non-Goals for NinjaX 
- No annotations and hidden logic (aspects)
- Immutability wherever possible
- Never use nulls
- As as few libraries and external dependencies as possible (no mockito? no matcher library?)
- No dependency injection
- Composition over inheritance for most cases. It's just easier to understand than inheritance.
- No exposure of servlet api whatsoever
- Only one way to do things (e.g. not routing file AND annotations)
- Trading a bit of boilerplate for clarity (easyof usafe / debugging) is ok
  - eg validation
  - controller methods all look the same
  - manual "Dependency Injection" (more boilerplate, but faster startup and obvious code usage).

## Things we won't do or support:
- Guice or injector. We just assemble everything on top level...
- Injection priorities won't be part of Ninja. If you do the instantiation in Assembly correctly you don't need a priority
- No support for circular dependencies. if you got circular dependencies you are doing it wrong.
- Scheduler won't be part of NinjaX. Can be done separately
- Freemarker won't be part of NinjaX - this is replaced by Juckula
- https is not part of NinjaX
- No injected Path into controller method. Context as default and only "thing". Keep it simple. one way to do things. simple debugging and tracing.
- No exception based error handling to generate results
- Ability to change server is not a goal for v1. Usung Jetty for now.

## Deployment to Maven Central

    # Make sure gpg is set up properly
    mvn deploy -Prelease

Log in to https://central.sonatype.com/ to release things.

v1 Alpha TODO:
=========

- session => Secret / httponly add flags


- juckula.replacePlaceholders ... tests + hwat if I want to render ${...}?
- juckula add escaping by default and special record to render raw
- compiled templates without regex...
- stream to x and not toString


- json und request jackson parsen mit inject bauen
- umbenennen in setAttribute (nicht payload bei request)
- assetscontroller review und test
- assetscontroller - do not update session / flash (race condition?)
- test of sessioncookie code
- remove secret in conf - maybe


- add tests to db module
-- hikari
-- jdbi
-- flyway
-- jdbc
-- big fat try catch to wrap java exceptions with a 500
- move conf to resourcs

- how to make e.g. json configurable with json...

v1 BETA TODO:
=============

- make NinjaJetty nicer
-flashscope?
- make everything immutable and stabilize Api
- header => do not return null if not present...
- move from servlet to raw jetty api
- add tests to all areas / documentation / fix missing proper error handling
- add ai compatible documentation
- send security headers by defaul (see e.g. play fraemwork)


v2 TODO:
====
- replaceable prpertties ${...} => see chatgpt
- json
- websockets?
- global filter
- i18n support...



Q&A
===

### Q Why no dependency injection framework?
- Slow fast and indirect.
- Faster startup times than before



# Documentation

## Session Configuration

Session cookies in NinjaX can be configured via application.conf.

### Session Cookie Security

By default, session cookies are created with the `Secure` flag enabled, which means they will only be transmitted over HTTPS connections. This is the recommended setting for production environments.

To configure the Secure flag, add the following to your application.conf:

    # Set to true (default) to only send session cookies over HTTPS
    # Set to false to allow session cookies over HTTP (not recommended for production)
    application.session.cookie.secure=true

If not specified, the default value is `true`.

### Session Expiry Time

To configure session expiry time in seconds:

    application.session.expire_time_in_seconds=3600

If not specified, session cookies will be session-only (expire when browser closes).

## Messages and I18N

- messages bundles in "root" (Java convention) (messages.properties, messages_de.properties and so on)
- message bundles should be utf-8
- message bundles use the message format where you can use localization and placeholders as defined by the Java Message format
- Define your supported messages in application.conf and key application.languages=en,de
- the first message in application.languages will be the fallback message
- if requested locale is not available the default locale will be used
- if a key cannot be found a warn will be logged and the message key will be used instead

In code you can use

    // to get the locale
    var locale = request.getLocale() (defuced from either accept-languages header OR a cookie to override that

    // and to get the message for a key
    // login.message=Welcome!
    var translatedMessage = ninjaMessages.getMessage("login.message", locale);

    // or if the message contains parameters
    // login.message=Welcome {0}!
    var translatedMessage = ninjaMessages.getMessage("my.key", locale, "Frank);




    








