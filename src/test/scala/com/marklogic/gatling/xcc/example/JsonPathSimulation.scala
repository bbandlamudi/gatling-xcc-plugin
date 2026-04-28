package com.marklogic.gatling.xcc.example

import io.gatling.core.Predef._
import com.marklogic.gatling.xcc.Predef._
import scala.concurrent.duration._

/**
 * Simple JSON path extraction and reuse simulation
 * 
 * Demonstrates both ${} and #{} EL syntax for session variable references.
 * Both syntaxes work identically in most cases - use whichever you prefer.
 * 
 * Also shows how to pass JSON as a string parameter with #{} variable interpolation.
 * Step 4 demonstrates passing all inputs as a single JSON string with interpolated values.
 * 
 * Flow:
 * 1. Execute query that returns JSON
 * 2. Extract JSON attributes using jsonPath
 * 3. Use extracted values in next query with #{} syntax
 * 4. Pass ALL inputs as a single JSON string with #{} interpolation, parse in XQuery
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
    // Using #{} syntax instead of ${} for variety
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
        .queryParam("orderId", "#{orderId}")      // Using #{} syntax
        .queryParam("customerId", "#{customerId}")  // Using #{} syntax
        .queryParam("amount", "#{amount}")        // Using #{} syntax
        .check(xccBodyNotEmpty)
        .check(xccSubstring("ISSUED"))
        .check(xccSaveAs("invoiceJson"))  // Save invoice for next step
        .build()
    )
    .pause(200.milliseconds)
    
    // Step 4: Pass ALL inputs as a single JSON string with #{} variable interpolation
    // This demonstrates building a complete JSON payload with multiple interpolated values
    .exec(
      xcc("Process Invoice with JSON Payload")
        .xquery("""
          declare variable $invoicePayload external;
          
          (: Parse the JSON string back to a map object :)
          let $invoice := xdmp:from-json-string($invoicePayload)
          
          (: Extract values from the parsed JSON :)
          let $invoiceId := map:get($invoice,"invoiceId")
          let $orderId :=  map:get($invoice,"orderId")
          let $customerId := map:get($invoice,"customerId")
          let $amount := map:get($invoice,"amount")
          let $status := map:get($invoice,"status")
          
          (: Construct response JSON using the extracted values :)
          return xdmp:to-json(map:map()
            => map:with("processedInvoiceId", $invoiceId)
            => map:with("originalOrderId", $orderId)
            => map:with("processedCustomerId", $customerId)
            => map:with("processedAmount", $amount)
            => map:with("inputStatus", $status)
            => map:with("processedBy", "System")
            => map:with("processedAt", fn:string(fn:current-dateTime()))
            => map:with("paymentStatus", "PENDING")
            => map:with("paymentMethod", "CREDIT_CARD")
          )
        """)
        .queryParam(
          "invoicePayload", 
          """{"invoiceId":"INV-#{orderId}","orderId":"#{orderId}","customerId":"#{customerId}","amount":"#{amount}","status":"PROCESSING"}"""  // Single JSON string with multiple #{} interpolations
        )
        .check(xccBodyNotEmpty)
        .check(xccSubstring("processedInvoiceId"))
        .check(xccSubstring("PENDING"))
        .check(xccSubstring("PROCESSING"))  // Verify the input status was parsed
        .build()
    )

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(protocol)
}

