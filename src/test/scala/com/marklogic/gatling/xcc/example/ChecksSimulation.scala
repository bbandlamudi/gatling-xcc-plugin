
package com.marklogic.gatling.xcc.example

import io.gatling.core.Predef._
import com.marklogic.gatling.xcc.Predef._
import scala.concurrent.duration._

/**
 * Simulation demonstrating the various check capabilities
 * Including both success and failure cases
 */
class ChecksSimulation extends Simulation {

  val protocol = xccProtocol("xcc://admin:admin@localhost:8000/Documents").build()

  val scn = scenario("XCC Checks Demo - Success Cases")
    
    // Basic checks
    .exec(
      xcc("Check Body Not Empty")
        .xquery("xdmp:database-name(xdmp:database())")
        .check(xccBodyNotEmpty)
        .build()
    )
    .pause(500.milliseconds)
    
    // Substring check
    .exec(
      xcc("Check Contains Substring")
        .xquery("xdmp:product-name()")  // Returns "MarkLogic Server"
        .check(xccSubstring("MarkLogic"))
        .build()
    )
    .pause(500.milliseconds)
    
    // Regex check
    .exec(
      xcc("Check Matches Regex")
        .xquery("xdmp:version()")
        .check(xccRegex("""\d+\.\d+"""))
        .build()
    )
    .pause(500.milliseconds)
    
    // Save response to session
    .exec(
      xcc("Save Database Name")
        .xquery("xdmp:database-name(xdmp:database())")
        .check(xccSaveAs("dbName"))
        .build()
    )
    .exec { session =>
      println(s"Database name: ${session("dbName").as[String]}")
      session
    }
    .pause(500.milliseconds)
    
    // XML/XPath check
    .exec(
      xcc("XPath Check")
        .xquery("""
          <document>
            <id>12345</id>
            <title>Test Document</title>
            <content>Sample content</content>
          </document>
        """)
        .check(xccXPath("id"))
        .build()
    )
    .pause(500.milliseconds)
    
    // Response time check
    .exec(
      xcc("Response Time Check")
        .xquery("xdmp:database-name(xdmp:database())")
        .check(xccResponseTime(1000)) // Max 1 second
        .build()
    )
    .pause(500.milliseconds)
    
    // No error check
    .exec(
      xcc("No Error Check")
        .xquery("xdmp:version()")
        .check(xccNoError)
        .build()
    )
    .pause(500.milliseconds)
    
    // JSON path check
    .exec(
      xcc("JSON Path Check")
        .xquery("""
          xdmp:to-json(map:map()
            => map:with("status", "success")
            => map:with("count", 42)
            => map:with("message", "Operation completed"))
        """)
        .check(xccJsonPath("status"))
        .build()
    )
    .pause(500.milliseconds)
    
    // Extract and save
    .exec(
      xcc("Extract Version Number")
        .xquery("xdmp:version()")
        .check(xccExtract(
          body => body.split(" ").headOption.getOrElse("unknown"),
          "versionNumber"
        ))
        .build()
    )
    .exec { session =>
      println(s"Version: ${session("versionNumber").as[String]}")
      session
    }
    .pause(500.milliseconds)
    
    // Multiple checks using .checks() helper
    .exec(
      xcc("Multiple Checks")
        .xquery("""
          <response>
            <status>OK</status>
            <timestamp>{fn:current-dateTime()}</timestamp>
          </response>
        """)
        .checks(
          xccBodyNotEmpty,
          xccSubstring("OK"),
          xccNoError,
          xccResponseTime(2000)
        )
        .build()
    )
    .pause(500.milliseconds)
    
    // Test that whitespace-only is NOT considered empty (string is not empty)
    .exec(
      xcc("Whitespace Not Empty")
        .xquery("' '")  // Returns single space
        .check(xccBodyNotEmpty)  // Should PASS - space is not empty string
        .build()
    )

  // Scenario demonstrating failure cases (these requests will fail as expected)
  val failureScn = scenario("XCC Checks Demo - Expected Failures")
    
    // Test empty string - xccBodyNotEmpty should FAIL
    .exec(
      xcc("Empty String Check - Expected to Fail")
        .xquery("''")  // Returns empty string
        .check(xccBodyNotEmpty)  // Will FAIL with "Response body is empty"
        .build()
    )
    .pause(500.milliseconds)
    
    // Test substring not found - should FAIL
    .exec(
      xcc("Substring Not Found - Expected to Fail")
        .xquery("'Hello World'")
        .check(xccSubstring("Goodbye"))  // Will FAIL
        .build()
    )
    .pause(500.milliseconds)
    
    // Test regex not matching - should FAIL
    .exec(
      xcc("Regex No Match - Expected to Fail")
        .xquery("'abc123'")
        .check(xccRegex("^\\d+$"))  // Expects only digits, will FAIL
        .build()
    )

  // Run only success scenario by default
  // To run failure scenario, uncomment the second line below
  setUp(
    scn.inject(atOnceUsers(1))
    // Uncomment to also run failure cases (will show failed requests):
    // failureScn.inject(atOnceUsers(1))
  ).protocols(protocol)
}

