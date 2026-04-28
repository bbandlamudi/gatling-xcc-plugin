package com.marklogic.gatling.xcc.example

import io.gatling.core.Predef._
import com.marklogic.gatling.xcc.Predef._
import scala.concurrent.duration._

/**
 * Example simulation demonstrating different ways to configure XCC protocol
 * 
 * This simulation shows two approaches:
 * 1. Full URI approach - credentials embedded in URI
 * 2. Individual parameters approach - credentials set via builder methods
 * 
 * Both approaches produce the same connection, choose based on your preference.
 */
class ProtocolConfigurationSimulation extends Simulation {

  // Approach 1: Full URI with embedded credentials
  // Format: xcc://username:password@host:port/database
  val protocolFullUri = xccProtocol("xcc://admin:admin@localhost:8000/Documents")
    .build()

  // Approach 2: Individual parameters (builder pattern)
  // More explicit and easier to parameterize from config files
  val protocolBuilderPattern = xccProtocol("xcc://localhost:8000")
    .username("admin")
    .password("admin")
    .database("Documents")
    .build()

  // Define scenario using full URI approach
  val scnFullUri = scenario("Full URI Configuration")
    .exec(
      xcc("Get Database Name - Full URI")
        .xquery("xdmp:database-name(xdmp:database())")
        .build()
    )
    .pause(1.second)
    .exec(
      xcc("Get Server Version - Full URI")
        .xquery("xdmp:version()")
        .build()
    )

  // Define scenario using builder pattern
  val scnBuilder = scenario("Builder Pattern Configuration")
    .exec(
      xcc("Get Database Name - Builder")
        .xquery("xdmp:database-name(xdmp:database())")
        .build()
    )
    .pause(1.second)
    .exec(
      xcc("Get Server Version - Builder")
        .xquery("xdmp:version()")
        .build()
    )

  // Setup simulation - run both scenarios
  setUp(
    scnFullUri.inject(atOnceUsers(1)).protocols(protocolFullUri),
    scnBuilder.inject(atOnceUsers(1)).protocols(protocolBuilderPattern)
  )
}
