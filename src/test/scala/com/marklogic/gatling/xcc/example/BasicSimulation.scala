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
 * Example simulation demonstrating basic XCC plugin usage
 * Uses individual parameter approach for protocol configuration
 */
class BasicSimulation extends Simulation {

  // Configure XCC protocol using individual parameters
  val protocol = xccProtocol("xcc://localhost:8000")
    .username("admin")
    .password("admin")
    .database("Documents")
    .build()

  // Define scenario
  val scn = scenario("Basic XQuery Load Test")
    .exec(
      xcc("Get Database Name")
        .xquery("xdmp:database-name(xdmp:database())")
        .build()
    )
    .pause(1.second)
    .exec(
      xcc("Get Server Version")
        .xquery("xdmp:version()")
        .build()
    )

  // Setup simulation
  /*setUp(
    scn.inject(
      atOnceUsers(10),
      rampUsers(50).during(30.seconds)
    )
  ).protocols(protocol)
   .assertions(
     global.responseTime.max.lt(5000),
     global.successfulRequests.percent.gt(95)
   )*/
  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(protocol)

}

