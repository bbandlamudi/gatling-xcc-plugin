package io.gatling.xcc.example

import io.gatling.core.Predef._
import io.gatling.xcc.Predef._
import scala.concurrent.duration._

/**
 * Quick test simulation to verify XCC plugin logging
 * Uses full URI approach for protocol configuration
 */
class QuickTestSimulation extends Simulation {

  // Configure XCC protocol using full URI
  val protocol = xccProtocol("xcc://admin:admin@localhost:8000/Documents").build()

  // Define scenario
  val scn = scenario("Quick XCC Test")
    .exec(
      xcc("Get Database Name")
        .xquery("xdmp:database-name(xdmp:database())")
        .build()
    )

  // Setup simulation - just 1 user, 1 execution
  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(protocol)
}
