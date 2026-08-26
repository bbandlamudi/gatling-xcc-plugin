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
 * Simulation to test ContentSource caching behavior
 * 
 * This simulation tests three scenarios:
 * 1. Cached ContentSource (default) - ContentSource created once and reused
 * 2. Cached ContentSource (explicit) - ContentSource created once with explicit cacheContentSource=true
 * 3. Non-cached ContentSource - New ContentSource created for each request
 * 
 * To verify caching behavior, check logs for:
 * - Cached: "Successfully created ContentSource" appears ONCE during protocol setup
 * - Non-cached: "Creating new ContentSource for this request" appears for EACH request (3 times)
 * 
 * Run with:
 * mvn gatling:test -Dgatling.simulationClass=com.marklogic.gatling.xcc.example.CachedContentSourceSimulation
 */
class CachedContentSourceSimulation extends Simulation {

  // Cached ContentSource protocol (default behavior) - EXACT SAME as XccsSecureSimulation
  val cachedDefaultProtocol = xccProtocol("xccs://admin:admin@localhost:8443/Documents").build()
  
  // Cached ContentSource protocol (explicit for testing) - Explicitly set cacheContentSource=true
  val cachedExplicitProtocol = xccProtocol("xccs://admin:admin@localhost:8443/Documents?cacheContentSource=true").build()
  
  // Non-cached ContentSource protocol - Create new ContentSource for each request
  val nonCachedProtocol = xccProtocol("xccs://admin:admin@localhost:8443/Documents?cacheContentSource=false").build()
  
  // Scenario 1: Test with cached ContentSource (default) - SIMILAR to XccsSecureSimulation
  val cachedDefaultScenario = scenario("Cached ContentSource - Default")
    .exec(
      xcc("Cached Default - Request 1")
        .xquery("xdmp:database-name(xdmp:database())")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("dbName"))
        .build()
    )
    .exec { session =>
      println(s"[Cached-Default] Database: ${session("dbName").asOption[String].getOrElse("N/A")}")
      session
    }
    .pause(500.milliseconds)
    .exec(
      xcc("Cached Default - Request 2")
        .xquery("xdmp:version()")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("version"))
        .build()
    )
    .exec { session =>
      println(s"[Cached-Default] Version: ${session("version").asOption[String].getOrElse("N/A")}")
      session
    }
    .pause(500.milliseconds)
    .exec(
      xcc("Cached Default - Request 3")
        .xquery("1 + 1")
        .check(xccBodyNotEmpty)
        .build()
    )

  // Scenario 2: Test with cached ContentSource (explicit)
  val cachedExplicitScenario = scenario("Cached ContentSource - Explicit")
    .exec(
      xcc("Cached Explicit - Request 1")
        .xquery("xdmp:database-name(xdmp:database())")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("dbName"))
        .build()
    )
    .exec { session =>
      println(s"[Cached-Explicit] Database: ${session("dbName").asOption[String].getOrElse("N/A")}")
      session
    }
    .pause(100.milliseconds)
    .exec(
      xcc("Cached Explicit - Request 2")
        .xquery("xdmp:version()")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("version"))
        .build()
    )
    .exec { session =>
      println(s"[Cached-Explicit] Version: ${session("version").asOption[String].getOrElse("N/A")}")
      session
    }
    .pause(100.milliseconds)
    .exec(
      xcc("Cached Explicit - Request 3")
        .xquery("1 + 1")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("result"))
        .build()
    )
    .exec { session =>
      println(s"[Cached-Explicit] Result: ${session("result").asOption[String].getOrElse("N/A")}")
      session
    }

  // Scenario 3: Test with non-cached ContentSource
  val nonCachedScenario = scenario("Non-Cached ContentSource")
    .exec(
      xcc("Non-Cached - Request 1")
        .xquery("xdmp:database-name(xdmp:database())")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("dbName"))
        .build()
    )
    .exec { session =>
      println(s"[Non-Cached] Database: ${session("dbName").asOption[String].getOrElse("N/A")}")
      session
    }
    .pause(100.milliseconds)
    .exec(
      xcc("Non-Cached - Request 2")
        .xquery("xdmp:version()")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("version"))
        .build()
    )
    .exec { session =>
      println(s"[Non-Cached] Version: ${session("version").asOption[String].getOrElse("N/A")}")
      session
    }
    .pause(100.milliseconds)
    .exec(
      xcc("Non-Cached - Request 3")
        .xquery("1 + 1")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("result"))
        .build()
    )
    .exec { session =>
      println(s"[Non-Cached] Result: ${session("result").asOption[String].getOrElse("N/A")}")
      session
    }

  // Run scenarios - comment out the options you don't want to use
  
  // Option 1: Test Cached Default only
  /*
  setUp(
    cachedDefaultScenario.inject(atOnceUsers(1))
  ).protocols(cachedDefaultProtocol)
  */
  
  // Option 2: Test Cached Explicit only
  /*
  setUp(
    cachedExplicitScenario.inject(atOnceUsers(1))
  ).protocols(cachedExplicitProtocol)
  */
  
  // Option 3: Test Non-Cached only
  /*
  setUp(
    nonCachedScenario.inject(atOnceUsers(1))
  ).protocols(nonCachedProtocol)
  */
  
  // Option 4: Test all three caching modes in parallel (demonstrates the differences)
  setUp(
    cachedDefaultScenario.inject(atOnceUsers(1)),
    cachedExplicitScenario.inject(atOnceUsers(1)),
    nonCachedScenario.inject(atOnceUsers(1))
  ).protocols(cachedDefaultProtocol, cachedExplicitProtocol, nonCachedProtocol)
}
