# Gatling XCC Plugin

A Gatling plugin for load testing MarkLogic databases using the XCC (XML Contentbase Connector) protocol, with support for both standard (`xcc://`) and secure (`xccs://`) connections.

## Features

- Execute XQuery and JavaScript scripts
- Invoke server-side modules
- Pass variables/parameters to queries
- Configure request options (timeout, locale, timezone, cacheable)
- Response validation checks (substring, regex, body equals, extract, save-as)
- Secure XCCS connections with SSL/TLS
- Full integration with Gatling's DSL and reporting

## Requirements

- Java 17+
- Scala 2.13.17
- Gatling 3.15.1
- MarkLogic XCC 11.3.0 (built/tested against this version; later versions should work fine)
- MarkLogic Server with XCC enabled

## Install

```xml
<dependency>
    <groupId>com.marklogic.gatling.xcc</groupId>
    <artifactId>gatling-xcc-plugin</artifactId>
    <version>1.0.1</version>
</dependency>
```

Or build from source: `mvn clean install`

## Quick Start

```scala
import io.gatling.core.Predef._
import com.marklogic.gatling.xcc.Predef._

class BasicSimulation extends Simulation {

  val xccProtocol = xcc("xcc://admin:admin@localhost:8000/Documents").build()

  val scn = scenario("Basic XQuery Test")
    .exec(
      xcc("Simple Query")
        .xquery("xdmp:database-name(xdmp:database())")
        .build()
    )

  setUp(scn.inject(atOnceUsers(10))).protocols(xccProtocol)
}
```

For more usage patterns (variables, JavaScript, module invocation, request options, checks, session chaining, XCCS), see the example simulations in [src/test/scala/com/marklogic/gatling/xcc/example/](src/test/scala/com/marklogic/gatling/xcc/example/) — each one is a self-contained, runnable demonstration of a specific feature. For how to write unit tests against the plugin itself (as opposed to integration-style simulations), see [src/test/scala/com/marklogic/gatling/xcc/XccPluginTest.scala](src/test/scala/com/marklogic/gatling/xcc/XccPluginTest.scala).

## Connection URI Format

```
xcc://[username:password@]host:port[/database]      # unencrypted
xccs://[username:password@]host:port[/database]      # SSL/TLS (TLSv1.2)
```

Query string options:
- `cacheContentSource=false` — create a new `ContentSource` per request instead of caching one (default: cached)
- `authenticationPreemptive=false` — disable preemptive auth on XCCS (default: enabled)

⚠️ The XCCS implementation uses trust-all certificate validation. That's fine for load testing but should not be relied on for anything security-sensitive.

## Executor Tuning

Every XCC request runs on a dedicated, bounded thread pool (kept off Gatling's core actor-dispatcher threads so blocking XCC I/O can't starve it). It's sized via system properties, with no rebuild required:

| Property | Default | Meaning |
|---|---|---|
| `gatling.xcc.executor.maxThreads` | 12000 | Hard cap on executor threads. Bounds native thread growth to avoid `pthread_create`/OOM under sustained high load. |
| `gatling.xcc.executor.coreThreads` | same as `maxThreads` | Floor the pool is kept at before growing. Only set lower than `maxThreads` if you deliberately want a low-floor-then-queue behavior — with a deep queue the pool won't grow past this until the queue fills. |
| `gatling.xcc.executor.queueCapacity` | 100000 | Requests queued once `maxThreads` is saturated. Absorbs overload as rising response time instead of rejecting. |

The pool shrinks back down when idle (`allowCoreThreadTimeOut`). One pool is shared per `XccProtocol`-class instance across the whole simulation run (not per protocol value, not per scenario) — see `XccProtocol.resolveExecutorConfig` / `newComponents`. The resolved values are logged once at `INFO` on startup: `XCC executor configured: coreThreads=..., maxThreads=..., queueCapacity=...`.

If the pool and queue are both saturated (should only happen if `queueCapacity` is undersized for the load), the request is marked failed rather than crashing the JVM — see `XccAction.execute`.

## Logging

The plugin logs through SLF4J (via `scala-logging`), with `logback-classic` as a compile-scope dependency — so it travels transitively onto the classpath of any project that depends on this plugin. Standard SLF4J rules apply: whichever `logback.xml` is found first on the classpath controls behavior, and you configure the plugin's verbosity in *your own* project by adding a `<logger>` entry for `com.marklogic.gatling.xcc`, not by changing anything here.

For a Gatling Gradle project, that's typically `src/gatling/resources/logback.xml`:

```xml
<logger name="com.marklogic.gatling.xcc" level="WARN" />
```

### What prints at each level

| Level | What you get |
|---|---|
| `ERROR` | Unhandled exceptions executing a request, the executor-saturated safety net firing (`XccAction.scala`), result-mapper failures, request build/execution failures |
| `WARN` | Check failures (response validation), request failures with their error message |
| `INFO` | **One line per request**: `Executing XCC request: <name>` — noisy at real load volumes, which is why `WARN` is the recommended default for load tests. Also: `ContentSource` creation/caching decisions, logged once per protocol build |
| `DEBUG` | Per-request lifecycle: building the request, which query type it is (XQuery/JS/module), submitting it, result item counts, success/duration |
| `TRACE` | Full response bodies, query parameter values, session/result-sequence close events — verbose, use only for targeted troubleshooting |

`com.marklogic.xcc` (the underlying MarkLogic client library, not this plugin) has its own logger and can be tuned the same way.

**Exception:** the executor sizing banner printed once at startup (`XccProtocol.scala`, `XCC executor service configured: coreThreads=..., maxThreads=..., queueCapacity=...`) is currently a `println`, not a logger call — it always goes to stdout regardless of your logback config and isn't routed to any file appender.

## Building and Testing

```bash
mvn clean compile      # compile
mvn test                # unit tests (XccPluginTest)
mvn package              # build the jar
mvn gatling:test -Dgatling.simulationClass=com.marklogic.gatling.xcc.example.BasicSimulation   # run one example simulation
```

To run the unit tests and all example simulations in sequence with a pass/fail summary:

```powershell
powershell -ExecutionPolicy Bypass -File run-all-simulations.ps1   # Windows
```
```bash
./run-all-simulations.sh                                            # bash / Git Bash
```

## Project Structure

```
gatling-xcc-plugin/
├── pom.xml
├── run-all-simulations.ps1 / .sh
├── src/
│   ├── main/scala/com/marklogic/gatling/xcc/
│   │   ├── Predef.scala
│   │   ├── action/            XccAction, XccActionBuilder
│   │   ├── protocol/          XccProtocol (connection + executor config)
│   │   └── request/           XccAttributes, XccRequestBuilder
│   └── test/scala/com/marklogic/gatling/xcc/
│       ├── XccPluginTest.scala    unit tests for the plugin itself
│       └── example/               runnable example simulations, one per feature
```

## Troubleshooting

- **Connection refused**: App Server not running or wrong port
- **SSL handshake failed**: SSL not configured on the MarkLogic App Server, or wrong port (XCCS typically uses 8443)
- **Authentication failed**: check username/password
- **XCC executor saturated / rising response times under load**: see [Executor Tuning](#executor-tuning) — raise `maxThreads`/`queueCapacity` or investigate why requests aren't draining

## License

Apache License 2.0
