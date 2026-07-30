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
 * Example simulation using JavaScript queries
 */
class JavaScriptSimulation extends Simulation {

  val protocol = xccProtocol("xcc://localhost:8000")
    .username("admin")
    .password("admin")
    .database("Documents")
    .build()

  val scn = scenario("JavaScript Query Test")
    .exec(
      xcc("Get Server Info")
        .javascript("""
          var info = {
            host: xdmp.host(),
            hostName: xdmp.hostName(),
            platform: xdmp.platform(),
            version: xdmp.version()
          };
          info;
        """)
        .build()
    )
    .pause(1.second)
    .exec(
      xcc("Search Documents")
        .javascript("""
          var results = cts.search(
            cts.andQuery([
              cts.directoryQuery('/test/', 'infinity')
            ]),
            ['unfiltered']
          );
          fn.count(results);
        """)
        .build()
    )

  /*setUp(
    scn.inject(constantUsersPerSec(5).during(30.seconds))
  ).protocols(protocol)*/
  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(protocol)
}

