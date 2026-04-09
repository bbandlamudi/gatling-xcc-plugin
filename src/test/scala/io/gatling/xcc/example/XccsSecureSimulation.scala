package io.gatling.xcc.example

import io.gatling.core.Predef._
import io.gatling.xcc.Predef._
import scala.concurrent.duration._

/**
 * Simulation demonstrating XCCS (secure XCC) protocol support.
 * 
 * XCCS provides SSL/TLS encryption for XCC connections to MarkLogic.
 * 
 * Key features:
 * 1. Automatic SSL/TLS encryption using TLSv1.2
 * 2. Trust-all certificate validation (suitable for development/testing)
 * 3. Authentication preemptive by default for XCCS
 * 4. HTTP compliance enabled automatically
 * 
 * Connection URI examples:
 * - Standard XCC:  xcc://admin:admin@localhost:8000/Documents
 * - Secure XCCS:   xccs://admin:admin@localhost:8443/Documents
 * 
 * Optional query parameters:
 * - authenticationPreemptive=false : Disable preemptive authentication
 * - cacheContentSource=false : Create new ContentSource for each request (not implemented in this version)
 * 
 * Note: This uses a trust-all SSL context. For production, implement proper certificate validation.
 */
class XccsSecureSimulation extends Simulation {

  // XCCS protocol configuration (change port and URI as needed)
  val secureProtocol = xccProtocol("xccs://admin:admin@localhost:8443/Documents").build()
  
  // Alternative: disable authentication preemptive
  // val secureProtocol = xccProtocol("xccs://admin:admin@localhost:8443/Documents?authenticationPreemptive=false").build()
  
  // Standard XCC protocol for comparison
  val standardProtocol = xccProtocol("xcc://admin:admin@localhost:8000/Documents").build()

  // Scenario using secure XCCS connection
  val secureScenario = scenario("Secure XCCS Connection")
    .exec(
      xcc("Get Database Name - XCCS")
        .xquery("xdmp:database-name(xdmp:database())")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("dbName"))
        .build()
    )
    .exec { session =>
      println(s"[XCCS] Database: ${session("dbName").asOption[String].getOrElse("N/A")}")
      session
    }
    .pause(500.milliseconds)
    .exec(
      xcc("Get Server Version - XCCS")
        .xquery("xdmp:version()")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("version"))
        .build()
    )
    .exec { session =>
      println(s"[XCCS] Version: ${session("version").asOption[String].getOrElse("N/A")}")
      session
    }
  
  // Scenario using standard XCC connection
  val standardScenario = scenario("Standard XCC Connection")
    .exec(
      xcc("Get Database Name - XCC")
        .xquery("xdmp:database-name(xdmp:database())")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("dbName"))
        .build()
    )
    .exec { session =>
      println(s"[XCC] Database: ${session("dbName").asOption[String].getOrElse("N/A")}")
      session
    }
    .pause(500.milliseconds)
    .exec(
      xcc("Get Server Version - XCC")
        .xquery("xdmp:version()")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("version"))
        .build()
    )
    .exec { session =>
      println(s"[XCC] Version: ${session("version").asOption[String].getOrElse("N/A")}")
      session
    }

  // Run both scenarios - comment out the one you don't want to use
  
  // Option 1: Test secure XCCS connection
  /*
  setUp(
    secureScenario.inject(atOnceUsers(1))
  ).protocols(secureProtocol)
  */
  
  // Option 2: Test standard XCC connection
  /*
  setUp(
    standardScenario.inject(atOnceUsers(1))
  ).protocols(standardProtocol)
  */
  
  // Option 3: Test both in parallel
  setUp(
    secureScenario.inject(atOnceUsers(1)),
    standardScenario.inject(atOnceUsers(1))
  ).protocols(secureProtocol, standardProtocol)
}
