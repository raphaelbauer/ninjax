# NinjaX

## Intro

Just some experiemnts how a Java web framework in 2026 would look like.



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
- scheduler won't be part of Ninja. If you do the instantiation in Assembly correctly you don't need a scheduler
- Freemarker won't be part of NinjaX - this is replaced by Juckula
- https is not part of NinjaX
- Extreme performance is not part of Ninja(Jetty)


v1 TODO:
=========
- DONE router with placeholders
- parse paramters into context
- Assetscontroller
-- clarify how write into an outputstream from result... (better than in original ninja)
- json parsing
- Logging slf4j
- Cookies => parser and writer...
- Config reader
- Datenabnkmodul mit evolutions

- Jukula
- Filter?
- Proof of concept => Migrate team climate over



v2 TODO:
====
- websockets?




Q&A
===

### Q Why no dependency injection framework?
- Slow fast and indirect.
- Faster startup times than before






