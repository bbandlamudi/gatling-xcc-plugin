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
 * Advanced simulation demonstrating:
 * 1. Reading input variables from a CSV feeder file
 * 2. Invoking server-side modules with feeder variables using clean EL syntax
 * 3. Chaining multiple module invocations with feeder data
 * 4. Passing response from one invoke as input to another invoke using session variables
 * 5. Saving XCC response to session with xccSaveAs() and using it in subsequent requests
 * 
 * Key patterns:
 * - Save response: .check(xccSaveAs("variableName"))
 * - Use saved value: .queryParam("paramName", "${variableName}") or "#{variableName}"
 * 
 * Note: Both ${} and #{} EL syntaxes are supported. This simulation uses both for demonstration.
 * 
 * Modules to deploy manually:
 * - /lib/feeder-search.xqy
 * - /lib/feeder-test-search.xqy
 * - /lib/feeder-count-by-type.xqy
 * - /lib/process-count.xqy
 */
class FeederWithSessionSimulation extends Simulation {

  val protocol = xccProtocol("xcc://admin:admin@localhost:8000/Documents").build()

  // CSV feeder with search parameters
  val searchDataFeeder = csv("search-data.csv").circular

  val scn = scenario("Feeder with Session Variables")
    .feed(searchDataFeeder)
    
    // Step 1: Search documents - clean EL syntax!
    // Save the search results to session for later use
    .exec(
      xcc("Search Documents Module")
        .invoke("/lib/feeder-search.xqy")
        .queryParam("searchTerm", "${searchTerm}")
        .queryParam("docType", "${docType}")
        .queryParam("maxResults", "${maxResults}")
        .mapResult(rs => if (rs.hasNext) rs.next().asString() else "")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("searchResults"))  // Save response to session
        .option("timeout", "30000")
        .build()
    )
    .pause(500.milliseconds)
    
    // Step 2: Search test documents - Using #{} syntax for variety
    // Uses searchTerm from feeder, saves result to session
    .exec(
      xcc("Search Test Documents Module")
        .invoke("/lib/feeder-test-search.xqy")
        .queryParam("searchTerm", "#{searchTerm}")  // #{} syntax alternative
        .queryParam("previousResults", "#{searchResults}")  // Use saved results from Step 1 with #{}
        .mapResult(rs => if (rs.hasNext) rs.next().asString() else "")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("testSearchResults"))  // Save for next step
        .build()
    )
    .pause(500.milliseconds)
    
    // Step 3: Count by type
    // Uses docType from feeder and searchResults from Step 1
    .exec(
      xcc("Count by Type Module")
        .invoke("/lib/feeder-count-by-type.xqy")
        .queryParam("docType", "${docType}")
        .queryParam("searchResults", "${searchResults}")  // Use results from Step 1
        .mapResult(rs => if (rs.hasNext) rs.next().asString() else "")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("documentCount"))  // Save count XML for next step
        .build()
    )
    .pause(500.milliseconds)
    
    // Step 4: Process count - uses saved XML from Step 3 (XQuery extracts the count)
    .exec(
      xcc("Process Count Module - Chained Response")
        .invoke("/lib/process-count.xqy")
        .queryParam("count", "${documentCount}")  // Pass full XML from Step 3
        .queryParam("docType", "${docType}")  // Use docType from feeder
        .queryParam("testResults", "${testSearchResults}")  // Use results from Step 2
        .mapResult(rs => if (rs.hasNext) rs.next().asString() else "")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("finalResult"))  // Save final result
        .build()
    )
    .exec { session =>
      // Debug: Print all session variables from the chain
      println(s"Session Variables:")
      println(s"  searchTerm: ${session("searchTerm").asOption[String].getOrElse("N/A")}")
      println(s"  docType: ${session("docType").asOption[String].getOrElse("N/A")}")
      println(s"  maxResults: ${session("maxResults").asOption[String].getOrElse("N/A")}")
      println(s"  searchResults (length): ${session("searchResults").asOption[String].map(_.length).getOrElse(0)} chars")
      println(s"  testSearchResults (length): ${session("testSearchResults").asOption[String].map(_.length).getOrElse(0)} chars")
      println(s"  documentCount (length): ${session("documentCount").asOption[String].map(_.length).getOrElse(0)} chars")
      println(s"  finalResult (length): ${session("finalResult").asOption[String].map(_.length).getOrElse(0)} chars")
      session
    }

  // Setup: Run with multiple users cycling through feeder data
  /*setUp(
    scn.inject(
      atOnceUsers(5) // Run 5 users at once, each will get different feeder data
    )
  ).protocols(protocol)
    .assertions(
      global.successfulRequests.percent.gt(95),
      global.responseTime.max.lt(5000)
    )*/

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(protocol)
}
