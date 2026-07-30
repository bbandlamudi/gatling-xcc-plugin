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
 * Example simulation demonstrating mapResult functionality
 * Shows how to transform ResultSequence into different types
 */
class MapResultSimulation extends Simulation {

  val protocol = xccProtocol("xcc://admin:admin@localhost:8000/Documents").build()

  val scn = scenario("MapResult Test")
    // Example 1: Extract first item as string
    .exec(
      xcc("Get Database Name - Extract String")
        .xquery("xdmp:database-name(xdmp:database())")
        .mapResult(rs => {
          if (rs.hasNext) rs.next().asString() 
          else "No result"
        })
        .build()
    )
    .pause(500.milliseconds)
    
    // Example 2: Count results
    .exec(
      xcc("Count Documents")
        .xquery("fn:count(fn:collection())")
        .mapResult(rs => {
          if (rs.hasNext) {
            val count = rs.next().asString()
            s"Total documents: $count"
          } else "0"
        })
        .build()
    )
    .pause(500.milliseconds)
    
    // Example 3: Collect all results as list
    .exec(
      xcc("List Test Documents")
        .xquery("""
          for $doc in fn:collection()[1 to 5]
          return fn:base-uri($doc)
        """)
        .mapResult(rs => {
          val uris = scala.collection.mutable.ListBuffer[String]()
          while (rs.hasNext) {
            uris += rs.next().asString()
          }
          val count = uris.size
          val uriList = uris.mkString(", ")
          s"Found $count documents: $uriList"
        })
        .build()
    )
    .pause(500.milliseconds)
    
    // Example 4: Extract and parse numeric result
    .exec(
      xcc("Server Uptime")
        .xquery("xdmp:elapsed-time()")
        .mapResult(rs => {
          if (rs.hasNext) {
            val duration = rs.next().asString()
            s"Server uptime: $duration"
          } else "Unknown"
        })
        .build()
    )

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(protocol)
}
