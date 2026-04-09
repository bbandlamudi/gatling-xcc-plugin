package io.gatling.xcc.example

import io.gatling.core.Predef._
import io.gatling.xcc.Predef._
import scala.concurrent.duration._

/**
 * Example simulation invoking server-side modules
 */
class ModuleInvocationSimulation extends Simulation {

  val protocol = xccProtocol("xcc://admin:admin@localhost:8000/Documents").build()

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
  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(protocol)
}
