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

