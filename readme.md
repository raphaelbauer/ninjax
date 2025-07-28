# NinjaX

## Intro

Just some experiemnts on how a Java web framework in 2026 would look like.


## Thoughts on how we started with the Ninja Web Framework in 2012

When we started in 2012 the Java world was differnt

- Java was recovering from EJB and EE Servers
- war and enterprise java was still a thing
- null was still used widely
- mutability was the norm
- servlets were important / also to access lower case
- lambdas did not exist back then
- no records


## Goals and Non-Goals for NinjaX 
- Immutability wherever possible
- Never use nulls
- As few libraries and external dependencies as possible (no mockito? no matcher library?)
- No exernal dependency injection
- Composition over inheritance for clarity
- Json parser?
- No exposure of servlet api whatsoever
- No annotations and hidden logic.
- One way to do things


## Non goals:
- no guice or injector. We just assemble everything on top level...
- injection priorities won't be part of Ninja. If you do the instantiation in Assembly correctly you don't need a priority
- no support for circular dependencies. if you got circular dependencies you are doing it wrong.
- scheduler won't be part of NinjaX. Can be done separately
- Freemarker won't be part of NinjaX - this is replaced by Juckula
- https is not part of NinjaX
- Extreme performance is core goal of Ninja (Jetty)
- No injected Path into controller method. Context as default and only "thing". Keep it simple. one way to do things. simple debugging and tracing.
- No exception based error handling to generate results
- ability to change server is not a goal for v1. Usung Jetty for now.


v1 TODO:
=========
- DONE router with placeholders
- DONE Config reader (simplified)
- DONE Logging slf4j
- DONE parse paramters into context
- DONE Jukula

- DONE Assetscontroller
-- DONE clarify how write into an outputstream from result... (better than in original ninja)

- DONE json
-- DONE parsing and 
-- DONE rendering
- DONE Cookies => parser and writer...
- DONE context setXYZ
- DONE Filter + filterchain

- header handling
- session handling (jwt?) https://github.com/jwtk/jjwt
- Datenabnkmodul mit evolutions

- move from servlet to raw jetty api

- Proof of concept => Migrate team climate over
- add tests to all areas / documentation / fix missing proper error handling



v2 TODO:
====
- json
- websockets?




Q&A
===

### Q Why no dependency injection framework?
- Slow fast and indirect.
- Faster startup times than before






