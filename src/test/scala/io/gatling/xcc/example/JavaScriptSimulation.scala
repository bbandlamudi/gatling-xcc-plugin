package io.gatling.xcc.example

import io.gatling.core.Predef._
import io.gatling.xcc.Predef._
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
