/*
 * Copyright (c) 2026 Bhagat Bandlamudi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.marklogic.gatling.xcc.example

import io.gatling.core.Predef._
import com.marklogic.gatling.xcc.Predef._
import scala.concurrent.duration._

/**
 * Simulation demonstrating XCCS (secure XCC) protocol support with different authentication modes.
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
 * - Secure XCCS (default):   xccs://admin:admin@localhost:8443/Documents
 * - Secure XCCS (no preemptive): xccs://admin:admin@localhost:8443/Documents?authenticationPreemptive=false
 * 
 * Optional query parameters:
 * - authenticationPreemptive=false : Disable preemptive authentication (use challenge-response)
 * - cacheContentSource=false : Create new ContentSource for each request (not implemented in this version)
 * 
 * This simulation demonstrates three scenarios:
 * 1. XCCS with default preemptive authentication (recommended for most use cases)
 * 2. XCCS with preemptive authentication disabled (for servers requiring challenge-response)
 * 3. Standard XCC for comparison
 * 
 * Note: This uses a trust-all SSL context. For production, implement proper certificate validation.
 */
class XccsSecureSimulation extends Simulation {

  // XCCS protocol with default preemptive authentication (recommended)
  val secureProtocol = xccProtocol("xccs://admin:admin@localhost:8443/Documents").build()
  
  // XCCS protocol with preemptive authentication disabled (for challenge-response auth)
  val secureNoPreemptiveProtocol = xccProtocol("xccs://admin:admin@localhost:8443/Documents?authenticationPreemptive=false").build()
  
  // Standard XCC protocol for comparison
  val standardProtocol = xccProtocol("xcc://admin:admin@localhost:8000/Documents").build()

  // Scenario 1: XCCS with default preemptive authentication
  val secureScenario = scenario("Secure XCCS Connection (Preemptive Auth)")
    .exec(
      xcc("Get Database Name - XCCS Preemptive")
        .xquery("xdmp:database-name(xdmp:database())")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("dbName"))
        .build()
    )
    .exec { session =>
      println(s"[XCCS-Preemptive] Database: ${session("dbName").asOption[String].getOrElse("N/A")}")
      session
    }
    .pause(500.milliseconds)
    .exec(
      xcc("Get Server Version - XCCS Preemptive")
        .xquery("xdmp:version()")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("version"))
        .build()
    )
    .exec { session =>
      println(s"[XCCS-Preemptive] Version: ${session("version").asOption[String].getOrElse("N/A")}")
      session
    }
  
  // Scenario 2: XCCS with preemptive authentication disabled (challenge-response)
  val secureNoPreemptiveScenario = scenario("Secure XCCS Connection (Challenge-Response Auth)")
    .exec(
      xcc("Get Database Name - XCCS No Preemptive")
        .xquery("xdmp:database-name(xdmp:database())")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("dbName"))
        .build()
    )
    .exec { session =>
      println(s"[XCCS-ChallengeResponse] Database: ${session("dbName").asOption[String].getOrElse("N/A")}")
      session
    }
    .pause(500.milliseconds)
    .exec(
      xcc("Get Server Version - XCCS No Preemptive")
        .xquery("xdmp:version()")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("version"))
        .build()
    )
    .exec { session =>
      println(s"[XCCS-ChallengeResponse] Version: ${session("version").asOption[String].getOrElse("N/A")}")
      session
    }
  
  // Scenario 3: Standard XCC connection (for comparison)
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

  // Run scenarios - comment out the options you don't want to use
  
  // Option 1: Test XCCS with preemptive auth only
  /*
  setUp(
    secureScenario.inject(atOnceUsers(1))
  ).protocols(secureProtocol)
  */
  
  // Option 2: Test XCCS with challenge-response auth only
  /*
  setUp(
    secureNoPreemptiveScenario.inject(atOnceUsers(1))
  ).protocols(secureNoPreemptiveProtocol)
  */
  
  // Option 3: Test standard XCC connection only
  /*
  setUp(
    standardScenario.inject(atOnceUsers(1))
  ).protocols(standardProtocol)
  */
  
  // Option 4: Test all three authentication modes in parallel (demonstrates the differences)
  setUp(
    secureScenario.inject(atOnceUsers(1)),
    secureNoPreemptiveScenario.inject(atOnceUsers(1)),
    standardScenario.inject(atOnceUsers(1))
  ).protocols(secureProtocol, secureNoPreemptiveProtocol, standardProtocol)
}

