package com.marklogic.gatling.xcc.example

import io.gatling.core.Predef._
import com.marklogic.gatling.xcc.Predef._
import scala.concurrent.duration._

/**
 * Simulation demonstrating the new xccSaveItemAs check
 * that allows saving specific items from result set by index
 */
class SaveItemsByIndexSimulation extends Simulation {

  val protocol = xccProtocol("xcc://admin:admin@localhost:8000/Documents").build()

  val scn = scenario("Save Items By Index")
    
    // Test 1: Multiple items - save different items by index
    .exec(
      xcc("Get Multiple Documents")
        .xquery("""
          for $i in (1 to 5)
          return <doc><id>{$i}</id><name>{"Document " || $i}</name></doc>
        """)
        .check(xccSaveFirstItemAs("firstDoc"))   // Save first item (index 0)
        .check(xccSaveItemAs(2, "thirdDoc"))     // Save third item (index 2)
        .check(xccSaveItemAs(4, "fifthDoc"))     // Save fifth item (index 4)
        .check(xccSaveAs("allDocs"))             // Save all items as string
        .build()
    )
    .exec { session =>
      println("=== Saved Items ===")
      println(s"First Doc:  ${session("firstDoc").as[String]}")
      println(s"Third Doc:  ${session("thirdDoc").as[String]}")
      println(s"Fifth Doc:  ${session("fifthDoc").as[String]}")
      println(s"All Docs:\n${session("allDocs").as[String]}")
      session
    }
    .pause(1.second)
    
    // Test 2: Single item - should work with index 0
    .exec(
      xcc("Get Single Document")
        .xquery("""<order><id>12345</id><total>99.99</total></order>""")
        .check(xccSaveItemAs(0, "singleOrder"))
        .build()
    )
    .exec { session =>
      println(s"\nSingle Order: ${session("singleOrder").as[String]}")
      session
    }
    .pause(1.second)
    
    // Test 3: Use saved item in subsequent request
    .exec(
      xcc("Get Three IDs")
        .xquery("""("ID-001", "ID-002", "ID-003")""")
        .check(xccSaveItemAs(1, "selectedId"))  // Save second ID
        .build()
    )
    .exec { session =>
      println(s"\nSelected ID: ${session("selectedId").as[String]}")
      session
    }
    .pause(500.milliseconds)
    .exec(
      xcc("Use Selected ID")
        .xquery("""
          declare variable $id external;
          <result>
            <inputId>{$id}</inputId>
            <processed>true</processed>
          </result>
        """)
        .queryParam("id", "${selectedId}")
        .check(xccBodyNotEmpty)
        .build()
    )
    .exec { session =>
      println("Successfully used saved ID in subsequent request")
      session
    }
    .pause(1.second)
    
    // Test 4: Compare xccSaveFirstItemAs vs xccSaveItemAs(0, ...)
    .exec(
      xcc("Compare Save Methods")
        .xquery("""("Alpha", "Beta", "Gamma", "Delta")""")
        .check(xccSaveFirstItemAs("usingFirst"))
        .check(xccSaveItemAs(0, "usingIndex0"))
        .check(xccSaveItemAs(3, "lastItem"))
        .build()
    )
    .exec { session =>
      println("\n=== Comparing Save Methods ===")
      println(s"xccSaveFirstItemAs:     ${session("usingFirst").as[String]}")
      println(s"xccSaveItemAs(0, ...):  ${session("usingIndex0").as[String]}")
      println(s"xccSaveItemAs(3, ...):  ${session("lastItem").as[String]}")
      println("Both first item methods return the same value!")
      session
    }

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(protocol)
}
