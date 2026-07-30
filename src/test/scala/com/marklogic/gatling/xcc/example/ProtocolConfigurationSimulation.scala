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
