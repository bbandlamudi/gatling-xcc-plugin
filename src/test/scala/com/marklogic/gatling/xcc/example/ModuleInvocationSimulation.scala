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
 * Example simulation invoking server-side modules
 */
class ModuleInvocationSimulation extends Simulation {

  val protocol = xccProtocol("xcc://admin:admin@localhost:8000/Documents").build()

  /**
   * Helper method to create XML input document from a session variable value
   * @param session The Gatling session
   * @param valueKey The session key containing the value to be wrapped in XML
   * @return Updated session with xmlInput variable set
   */
  def createXmlInput(session: Session, valueKey: String): Session = {
    val value = session(valueKey).as[String]
    val xmlDoc = s"<doc><value>$value</value></doc>"
    session.set("xmlInput", xmlDoc)
  }

  val scn = scenario("Module Invocation Test")
    .exec(
      xcc("Invoke Search Module")
        .invoke("/lib/search.xqy")
        .queryParam("query", "test")
        .queryParam("pageSize", 10)
        .queryParam("pageNumber", 1)
        .option("timeout", "30000")
        .build()
    )
    .pause(2.seconds)
    .exec(
      xcc("Invoke Transform Module")
        .invoke("/lib/transform.xqy")
        .queryParam("inputDoc", "<doc><value>test</value></doc>")
        .queryParam("format", "json")
        .build()
    )

  // Scenario demonstrating session variable usage
  val scnWithSession = scenario("Module Invocation with Session Variables")
    // Step 1: Store parameters in Gatling session using _.set() shorthand
    .exec(
      _.set("searchQuery", "test")
      .set("pageSize", 5)
    )
    .exec(session => {
          session.set("pageNumber", 2)
      }
    )
    .pause(500.milliseconds)
    // Step 2: Use session variables in module invocation
    .exec(
      xcc("Search with Session Params")
        .invoke("/lib/search.xqy")
        .queryParam("query", "${searchQuery}")
        .queryParam("pageSize", "#{pageSize}")
        .queryParam("pageNumber", "${pageNumber}")
        .option("timeout", "30000")
        .build()
    )
    .pause(1.second)
    // Step 3: Update session variables dynamically
    .exec(session => {
      val newPageNumber = session("pageNumber").as[Int] + 1
      session.set("pageNumber", newPageNumber)
    })
    // Step 4: Use updated session variables
    .exec(
      xcc("Search Next Page")
        .invoke("/lib/search.xqy")
        .queryParam("query", "#{searchQuery}")
        .queryParam("pageSize", "${pageSize}")
        .queryParam("pageNumber", "${pageNumber}")
        .build()
    )
    .pause(1.second)
    // Step 5: Store test value in session variable using _.set() shorthand
    .exec(_.set("testValue", "session-test"))
    // Step 6: Use helper method to create XML input from session variable
    .exec(session => {
      createXmlInput(session, "testValue")
    })
    // Step 7: Set output format and transform the XML using _.set() shorthand
    .exec(_.set("outputFormat", "json"))
    .exec(
      xcc("Transform with Session Data")
        .invoke("/lib/transform.xqy")
        .queryParam("inputDoc", "${xmlInput}")
        .queryParam("format", "#{outputFormat}")
        .build()
    )

  /*setUp(
    scn.inject(
      incrementUsersPerSec(5)
        .times(5)
        .eachLevelLasting(10.seconds)
        .separatedByRampsLasting(5.seconds)
        .startingFrom(5)
    )
  ).protocols(protocol)
   .assertions(
     global.responseTime.mean.lt(2000),
     forAll.failedRequests.percent.lte(1)
   )*/
  
  // Run both scenarios
  setUp(
    scn.inject(atOnceUsers(1)),
    scnWithSession.inject(atOnceUsers(1))
  ).protocols(protocol)
}

