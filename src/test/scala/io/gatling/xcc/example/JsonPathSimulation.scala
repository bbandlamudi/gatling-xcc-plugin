package io.gatling.xcc.example

import io.gatling.core.Predef._
import io.gatling.xcc.Predef._
import scala.concurrent.duration._

/**
 * Simple JSON path extraction and reuse simulation
 * 
 * Flow:
 * 1. Execute query that returns JSON
 * 2. Extract JSON attributes using jsonPath
 * 3. Use extracted values in next query
 */
class JsonPathSimulation extends Simulation {

  val protocol = xccProtocol("xcc://admin:admin@localhost:8000/Documents").build()

  val scn = scenario("JSON Path Extract and Reuse")
    
    // Step 1: Execute query that returns JSON document
    .exec(
      xcc("Get Order JSON")
        .xquery("""
          xdmp:to-json(map:map()
            => map:with("orderId", "ORD-98765")
            => map:with("customerId", "CUST-12345")
            => map:with("amount", 299.99)
            => map:with("status", "PENDING"))
        """)
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("orderJson"))
        .build()
    )
    .pause(200.milliseconds)
    
    // Step 2: Extract JSON attributes using regex patterns
    .exec(session => {
      val json = session("orderJson").as[String]
      
      val orderIdPattern = """"orderId"\s*:\s*"([^"]+)"""".r
      val customerIdPattern = """"customerId"\s*:\s*"([^"]+)"""".r
      val amountPattern = """"amount"\s*:\s*([0-9.]+)""".r
      
      val orderId = orderIdPattern.findFirstMatchIn(json).map(_.group(1)).getOrElse("")
      val customerId = customerIdPattern.findFirstMatchIn(json).map(_.group(1)).getOrElse("")
      val amount = amountPattern.findFirstMatchIn(json).map(_.group(1)).getOrElse("0")
      
      println(s"[DEBUG] Extracted - OrderId: $orderId, CustomerId: $customerId, Amount: $amount")
      
      session
        .set("orderId", orderId)
        .set("customerId", customerId)
        .set("amount", amount)
    })
    
    // Step 3: Use extracted values as parameters in next query
    .exec(
      xcc("Create Invoice")
        .xquery("""
          declare variable $orderId external;
          declare variable $customerId external;
          declare variable $amount external;
          
          xdmp:to-json(map:map()
            => map:with("invoiceId", "INV-" || fn:substring-after($orderId, "ORD-"))
            => map:with("orderId", $orderId)
            => map:with("customerId", $customerId)
            => map:with("amount", $amount)
            => map:with("status", "ISSUED")
            => map:with("issueDate", fn:string(fn:current-date())))
        """)
        .queryParam("orderId", "${orderId}")
        .queryParam("customerId", "${customerId}")
        .queryParam("amount", "${amount}")
        .check(xccBodyNotEmpty)
        .check(xccSubstring("ISSUED"))
        .build()
    )

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(protocol)
}
