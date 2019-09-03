## What is this?

A barebones REST API hosted in an in-process HTTP server ([Jetty](https://www.eclipse.org/jetty/)) that models working with bank accounts and money transfers.

## How do I use it?

Grab the JAR that corresponds to the [latest release](https://github.com/way-of-doing/java-moneytransfers/releases/latest) and run it. It will start an HTTP server listening at port 8888 (not configurable; if the port is not available, the only option is to recompile).

Read the [API specification](docs/API.md) to see how to talk to the server. This document includes ready-to-paste `curl` command lines for all endpoints for your convenience. In addition, there is a [Postman](https://www.getpostman.com/products) collection that you can directly import to use the API interactively [here](docs/API.postman_collection.json).

## Anything else I should know?

A [design document](docs/DESIGN.md) is available that summarizes the design of the server backend: how and why. The backend was designed before, and given more thought, than the REST API that exposes it.

The project was developed using [IntelliJ IDEA](https://www.jetbrains.com/idea/), but IDE-specific files (including the configuration to create the JAR) are not included in this repository.

The target platform is Java SE 11 ([OpenJDK](https://adoptopenjdk.net/) 11 with HotSpot JVM was used during development) and the project directly depends on the following libraries:

- [Moneta](https://javamoney.github.io/ri.html) -- a reference implementation of JSR 354, the Java Currency and Money API (provides the foundation for working with money)
- [Java Spark](http://sparkjava.com/) -- a micro framework for Java web applications (provides framework-level services for the API server; also includes [Jetty](https://www.eclipse.org/jetty/), which is used to host it)
- [Gson](https://github.com/google/gson) -- provides JSON support for the API
- [JUnit](https://junit.org/junit5/) -- for unit and functional tests