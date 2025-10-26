# NinjaX

[![Java CI with Maven](https://github.com/raphaelbauer/ninjax/actions/workflows/maven.yml/badge.svg)](https://github.com/raphaelbauer/ninjax/actions/workflows/maven.yml)

## Intro

This is an experiment on how a Java web framework in 2026 would look like.


## Thoughts on how we started with the Ninja Web Framework in 2012

When we started in 2012 the Java world was differnt - it was Java 7 at that time.

- Java was recovering from EJB and EE Servers
- War and enterprise java was still a thing
- Servlets were important
- Null was used widely / Optoinal did not exist
- Mutability was the norm
- The stream Api did not exist back then.
- Easy to use Lambdas did not exist back then
- Records did not exist
- Messages did not support utf8 leading to complex code


## Goals and Non-Goals for NinjaX 
- Immutability wherever possible
- Never use nulls
- As as few libraries and external dependencies as possible (no mockito? no matcher library?)
- No dependency injection
- Composition over inheritance for clarity
- No exposure of servlet api whatsoever
- No annotations and hidden logic.
- Only one way to do things (e.g. not routing file AND annotations)
- Trading a bit of boilerplate for clarity (easyof usafe / debugging) is ok
  - eg validation
  - controller methods all look the same
  - manual "di"


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

## Deployment


    # Make sure gpg is set up properly
    mvn deploy -Prelease

Log in to https://central.sonatype.com/ to release things.


v1 Alpha TODO:
=========
DONE httpServletRequest.getLocale() /* TODO local can also be set by a lang cookie to override headers of accept... */
DONE - TO BE DISCUSSED => remove ninjasession when it is valid in request, but not set in result (-1)
- WON'T DO get rid of guava as dependency

- session => Secret / httponly add flags

- router.GET("/app/dashboard").filter(ensureLogin).with(appController::dashboard);
-- filter und with is ugly and does not read well.
-- maybe: - router.GET("/app/dashboard").with(appController::dashboard).andfilter(...) or so???

- juckula.replacePlaceholders ... tests + hwat if I want to render ${...}?
- juckula add escaping by default and special record to render raw
- compiled templates without regex...


- json und request jackson parsen mit inject bauen
- umbenennen in setAttribute (nicht payload bei request)
- assetscontroller review und test
- assetscontroller - do not update session / flash (race condition?)
- test of sessioncookie code
- remove secret in conf - maybe
-- request vs result and what to do where? (keep immutable) or request

- add tests to db module
-- hikari
-- jdbi
-- flyway
-- jdbc
-- big fat try catch to wrap java exceptions with a 500
- move conf to resourcs

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




    








