# NinjaX

[![Java CI with Maven](https://github.com/raphaelbauer/ninjax/actions/workflows/maven.yml/badge.svg)](https://github.com/raphaelbauer/ninjax/actions/workflows/maven.yml)

## About

NinjaX is a full-stack web framework for modern Java 25+.
Rock solid, fast, and super productive.

## Background

### Rails as a Role Model for Web Development
In the second half of the 2000s, Rails took the web development community by storm. Rails made it very simple to ship full-stack web apps. Rails did not work against HTTP, but with it, which resulted in very clear request → do stuff → response cycles.

### Java Web Development Sucked in the 2000s. Hard.
Java back then was… well… different. There was the Servlet API. There was J2EE. There were session beans. Entity beans. Lots of magic in how HTTP mapped to Java code and persistence.  
All of that tried to hide HTTP from the developer, with varying degrees of success.

In hindsight, it was a very academic approach to wrap something into “object-oriented dogmas” that worked much better when used plainly (HTTP). IMHO.

### Ninja Set Out to Combine the Best of Both Worlds in 2010
So we started the Ninja web framework around 2010 - heavily influenced by Rails, but with the ecosystem, IDEs, and language we loved so much: Java. We also added lots of innovations: stateless, scalable apps; SuperDevMode, which made developing in Java as productive as in a scripting language; and many more things.

Ninja rose to some prominence, with a couple of larger companies using it. But when Spring Boot was released, it became the clear path for most Java companies.

### Spring Boot Entering the Arena
Spring Boot offered a lightweight migration from both J2EE (which was almost dead) and Spring-based applications. And today, Spring Boot is the #1 web framework for Java.

### Java Web Development in 2026 Can Be Different. And More Beautiful.
From 2012, when Ninja started, to 2026, an awful lot has happened in Javaland.

Just to name a few:
- EJBs, WAR files, and Enterprise Java are dead.
- Servlets and servlet containers are no longer important. (Okay - people now use something conceptually similar called Kubernetes, but that’s a different story.)
- Null is (almost) dead.
- There’s a big push for immutability and the use of Optional.
- Streams and mapping were introduced and add a nice functional flavor to Java.
- Easy-to-use lambdas make calling functionals syntactically nice - finally.
- Records are an interesting way to get immutable data classes with `hashCode` and `equals` implemented out of the box.
- We got multiline strings. (Okay - we still don’t have template strings.)
- Messages did not support UTF-8.
- ...and many more things.

At the same time, IDE support is still amazing. Java is more fun to code than ever.

So the big question is: How would a Java web framework look in 2026 - without any baggage from the past, using all the goodies of 2026?

This is what NinjaX is about. And to be frank: it is no longer an experiment, but something based on stable tech and already used in production.

## Philosophy and Guiding North Star

### Goals and Non-Goals for NinjaX
- Be obvious. No annotations or hidden logic (e.g., aspects).
- Prefer immutability wherever possible.
- No nulls. Never use null.
- Minimize dependencies. Use as few libraries and external dependencies as possible (e.g., no Mockito, no matcher library).
- No dependency injection.
- Prefer composition over inheritance (in most cases). It’s generally easier to understand than inheritance.
- No exposure of the Servlet API whatsoever.
- One way to do things (e.g., not routing via files and annotations).
- Trading a bit of boilerplate for clarity (ease of use / debugging) is OK.
  - Validation should be done via an explicit function. No magic validation in controllers.
  - No magic injection into controller functions. All controller methods receive a request and are explicit.
  - No dependency injection. It leads to more boilerplate, but it’s more obvious and enables fast startups.

### Things that are not supported in V1
- Injection priorities won’t be part of NinjaX. If instantiation in the Assembly is done correctly, you don’t need priorities.
- No support for circular dependencies. If you have circular dependencies, you’re doing it wrong.
- A scheduler won’t be part of NinjaX. This can be done separately.
- Freemarker won’t be part of NinjaX—this is replaced by Ninja Templates.
- HTTPS is not part of NinjaX.
- No exception-based error handling to generate results.
- Changing the server is not a goal for V1. Using Jetty for now.


## Getting Started

You'll need just 1 thing to develop with Ninja:

- JDK (Java Development Kit), version 25 and above

Note: Ninja is compatible with Java 25 and we'll support future Java versions with long term support.

### Installing Java
Ninja is using the Java as programming language and the Java Virtual Machine to run your applications. 
You have to make sure that you are running at least Java in version 25.

You can check that by executing the following command:

    java -version

which prints out the following:

    openjdk version "25.0.1" 2025-10-21

As you can see, this machine is running Java 25.0.1. 
If you are using an older version please install the latest Java version from or vie apt-get or brew.

http://www.oracle.com/technetwork/java/javase/downloads/index.html


### Create your first application

The most simple way to kickstart your NinjaX application is to download our archetype from here
Just download the file and unzip it.

Youn can also use the command line like so:

    wget https://github.com/raphaelbauer/ninja-demo-todo/archive/refs/heads/main.zip
    unzip main.zip

This will create a directory called ninja-demo-todo which contains a full Ninja project that is ready to go.

Starting the project is simple:

    cd ninja-demo-todo
    ./mvn clean install     // to generate the compiled classes the first time
    ./mvn ninja:run         // to start Ninja's SuperDevMode

This starts Ninja's SuperDevMode. Simply open http://localhost:8080 in your browser. 
You'll see Ninja demo project ready to work on. That's it basically. You just created your first Ninja application!

> [!NOTE]  
> We think that fast and responsive development cycles are a key success factor for software projects. 
> SuperDevMode is our answer to that challenge. Say goodbye to long and time consuming deployment cycles while developing.
> Make sure that your IDE is compiling changes as you make edits. That way Ninja's SuperDevMode will restart and pick-up all changes.


## Session Configuration

Ninja uses a session cookie that stores the session on the client side. The browser sends
the cookie back to the server with each request, which allows the server to re-identify a user.

### The Session Secret

Keeping the session client side is convenient because you don't need any server-side caching technology, and scaling
with many Ninja instances is trivial.

In order to verify that a cookie has been issued by a server, it is digitally signed and verified
using a secret that can be set in your `application.conf` file:

```properties
application.secret=YOUR_SECRET
```

You can generate a new secret easily using the Ninja plugin:

```bash
ninja:generateSecret
```

Copy this value into your `application.conf`.

> [!NOTE]  
> Ninja uses an unencrypted, but signed Json Web Token (JWT) that is set as cookie.
> Therefore don't store sensitive information inside the Ninja Session.

### Managing the Session Secret On Production Systems

In production services it is not advisable to use an `application.conf` that is checked into a version control system.
Instead, provide the application secret as an environment variable.

You can easily set the secret as an environment variable:

```bash
# Set variable manually or using built-in tooling of your cloud provider:
export APPLICATION_SECRET=generated_application_secret

java -jar -Dninja.port=5000 \
       -Dapplication.secret=${APPLICATION_SECRET} \
       ... more system variables \
       my-app.jar
```

Most cloud providers (such as AWS or Heroku) allow you to globally set environment variables, which
lets you start multiple instances that all use the same secret. 
This allows you to run multiple Ninja instances behind a load balancer; 
it does not matter which instance handles which request.
Any instance can handle and verify any session cookie.


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



## Contributing
### Deployment to Maven Central

    # Make sure gpg is set up properly
    mvn -Prelease release:prepare release:perform
    # => Everything is released automatically and should be available after few minutes globally.

Log in to https://central.sonatype.com/ check releases.


## Roadmap
### v1 BETA TODO:
- demo project
- documentation copy and past into README.md
- fix missing proper error handling
- security review

v2 TODO:
====
- add ai compatible documentation
- caching support?
- send security headers by default (see e.g. play fraemwork)
- flashscope?

- how to make e.g. json configurable with json...
- make NinjaJetty nicer
- move from servlet to raw jetty api
- stream to x and not toString

- json
- websockets?
- global filter
- i18n support...

improve ninhahml template dev ergonomics
- compiled templates without regex...
- Maybe have NinjaTemplateString or so to signl parsed stuff...
- maybe immutable and only render one ting (not String...)
- repladce Map with List of objects that then will be rendered 
- hide juckulaTool.replacePlceholders and integrate in template...
- new Html => Html.of(...) maybe better?


- rerun security assessment and fix all open topics

- add tests to db module
 - hikari
 - jdbi
 - flyway
 - jdbc
 - big fat try catch to wrap java exceptions with a 500



    








