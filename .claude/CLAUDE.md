# Learnings for working on NinjaX

The main project guide is `/CLAUDE.md` at the repo root. This file collects practical learnings.

## Java version policy
- The bytecode target is the latest **LTS** Java (currently 25), set once via `<java.release>` in the root `pom.xml`.
  The readme promises LTS support, so don't jump to non-LTS releases (26, 27) just because they exist.
- Building with a newer local JDK (e.g. 26) is fine, because the compiler uses `<release>`.
- CI (`.github/workflows/maven.yml`) runs on Temurin with the LTS version.

## Upgrading dependencies
- All versions live in the root `pom.xml` (`<properties>` + `dependencyManagement`).
  A few plugin/tooling versions sit in module poms: `ninjax-maven-plugin` (maven-plugin-api/core/annotations,
  maven-plugin-plugin) and both demo poms (`exec-maven-plugin`).
- Look up the latest stable versions straight from Maven Central metadata, e.g.
  `curl -s https://repo.maven.apache.org/maven2/org/jdbi/jdbi3-core/maven-metadata.xml`.
  Filter out alpha/beta/RC/M versions. Maven 4 is not GA yet, so stay on Maven 3.9.x (the wrapper pins it).
- Check JDK GA status via `https://api.adoptium.net/v3/info/available_releases`.
- Check the build log afterwards for new deprecation notes and compare the test count with the previous build.

## Library notes
- **Jackson 3** uses the `tools.jackson.*` packages (`tools.jackson.core:jackson-databind`) and is managed via
  `tools.jackson:jackson-bom`. java.time/Optional support is built in, so no jsr310/jdk8 modules are needed.
  Exceptions are unchecked (`JacksonException`). `JsonNode.asText()` is deprecated, so use `asString()`.
  Annotations still come from `com.fasterxml.jackson.core:jackson-annotations`.
- **JUnit 6** keeps the Jupiter coordinates and packages. It needs a recent surefire (pinned in root `pluginManagement`).
- **Testcontainers 2** renamed modules, e.g. `org.testcontainers:postgresql` became `testcontainers-postgresql`.
- **Flyway 13** logs "H2 2.5.x is newer than the version Flyway has been verified with". It is harmless and the migrations work.
- **Maven plugin**: inject `MavenProject`/`MavenSession` with `@Parameter(defaultValue = "${project}", readonly = true)`,
  not the deprecated `@Component`.
- `junit:junit:4.x` shows up on the test classpath only transitively via Google Truth. That is expected.

## Building and testing
- The IDE's Java language server (VS Code / Eclipse JDT) compiles into the same `target/classes` as Maven. After
  switching branches, Maven's incremental build can then run stale or broken class files ("Unresolved compilation
  problem" at runtime). Use `./mvnw clean test` after switching branches, or build in a git worktree outside the
  IDE workspace.
- `git stash` is shared between all worktrees of the repository. Prefer a temporary patch (`git diff > file`) or
  WIP commit when checking that a test fails without a fix.
- Tests that start a real server (`NinjaHttpServer`, `NinjaJetty`) run it in a daemon thread on a free port, because
  both constructors block until the server stops.

## Code notes
- `Request` is a final class with a hand-written builder, not a record: its public API uses `getX()` getters
  (`getLocale()`, `getFile()`, ...), which a record would turn into `x()` accessors. Null checks live in the constructor only.
- Uploaded files come from a single `FileItemsGetter`; `getFile(name)` is simply the first element of `getFiles(name)`.
