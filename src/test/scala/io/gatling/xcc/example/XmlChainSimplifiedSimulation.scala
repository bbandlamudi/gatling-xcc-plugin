package io.gatling.xcc.example

import io.gatling.core.Predef._
import io.gatling.xcc.Predef._
import scala.concurrent.duration._

/**
 * Simplified examples of chaining XCC invokes with XML extraction
 * Shows both approaches:
 * 1. Using saveAs to save entire XML response
 * 2. Using custom extract function to parse specific elements
 */
class XmlChainSimplifiedSimulation extends Simulation {

  val protocol = xccProtocol("xccs://admin:admin@localhost:8443/Documents").build()

  // ========================================
  // Scenario 1: Save entire XML and reuse
  // ========================================
  val saveEntireXml = scenario("Save Entire XML")
    .exec(
      xcc("Get Order XML")
        .xquery("""
          <order>
            <orderId>ORD-001</orderId>
            <customer>Alice</customer>
            <total>150.00</total>
          </order>
        """)
        .check(xccSaveAs("fullOrder"))
        .build()
    )
    .exec(
      xcc("Process Saved XML")
        .xquery("""
          declare variable $orderXml external;
          let $doc := xdmp:unquote($orderXml)
          return 
            <processed>
              <original>{$doc}</original>
              <timestamp>{fn:current-dateTime()}</timestamp>
            </processed>
        """)
        .queryParam("orderXml", "${fullOrder}")
        .check(xccBodyNotEmpty)
        .build()
    )

  // ========================================
  // Scenario 2: Extract specific fields
  // ========================================
  val extractSpecificFields = scenario("Extract Specific Fields")
    .exec(
      xcc("Get Customer Order")
        .xquery("""
          <order>
            <orderId>ORD-12345</orderId>
            <customerId>CUST-67890</customerId>
            <amount>299.99</amount>
            <status>PENDING</status>
          </order>
        """)
        .check(
          xccExtract(body => {
            import scala.xml.XML
            val xml = XML.loadString(body)
            (xml \\ "orderId").text
          }, "orderId")
        )
        .check(
          xccExtract(body => {
            import scala.xml.XML
            val xml = XML.loadString(body)
            (xml \\ "customerId").text
          }, "customerId")
        )
        .check(
          xccExtract(body => {
            import scala.xml.XML
            val xml = XML.loadString(body)
            (xml \\ "amount").text
          }, "amount")
        )
        .build()
    )
    .exec(session => {
      println(s"[INFO] Extracted: orderId=${session("orderId").as[String]}, " +
              s"customerId=${session("customerId").as[String]}, " +
              s"amount=${session("amount").as[String]}")
      session
    })
    .exec(
      xcc("Create Invoice")
        .xquery("""
          declare variable $orderId external;
          declare variable $customerId external;
          declare variable $amount external;
          
          let $invoiceId := "INV-" || fn:substring-after($orderId, "ORD-")
          return
            <invoice>
              <invoiceId>{$invoiceId}</invoiceId>
              <relatedOrder>{$orderId}</relatedOrder>
              <customerId>{$customerId}</customerId>
              <amount>{$amount}</amount>
              <issueDate>{fn:current-date()}</issueDate>
              <dueDate>{fn:current-date() + xs:dayTimeDuration('P30D')}</dueDate>
            </invoice>
        """)
        .queryParam("orderId", "${orderId}")
        .queryParam("customerId", "${customerId}")
        .queryParam("amount", "${amount}")
        .check(xccBodyNotEmpty)
        .check(xccSubstring("invoice"))
        .build()
    )

  // ========================================
  // Scenario 3: Simulated module invoke chain
  // ========================================
  val moduleInvokeChain = scenario("Module Invoke Chain")
    // First invoke: Get user details (simulating module call)
    .exec(
      xcc("Get User Details")
        .xquery("""
          (: Simulating /modules/get-user.xqy :)
          declare variable $userId external;
          
          <user>
            <userId>{$userId}</userId>
            <username>john.doe</username>
            <email>john.doe@example.com</email>
            <department>Engineering</department>
          </user>
        """)
        .queryParam("userId", "USER-123")
        .check(
          xccExtract(body => {
            import scala.xml.XML
            val xml = XML.loadString(body)
            (xml \\ "userId").text
          }, "userId")
        )
        .check(
          xccExtract(body => {
            import scala.xml.XML
            val xml = XML.loadString(body)
            (xml \\ "username").text
          }, "username")
        )
        .check(
          xccExtract(body => {
            import scala.xml.XML
            val xml = XML.loadString(body)
            (xml \\ "email").text
          }, "email")
        )
        .build()
    )
    // Second invoke: Create audit log with extracted data
    .exec(
      xcc("Create Audit Log")
        .xquery("""
          (: Simulating /modules/create-audit.xqy :)
          declare variable $userId external;
          declare variable $username external;
          declare variable $action external;
          declare variable $timestamp external;
          
          let $auditId := "AUDIT-" || fn:string(xdmp:random())
          return
            <auditLog>
              <auditId>{$auditId}</auditId>
              <userId>{$userId}</userId>
              <username>{$username}</username>
              <action>{$action}</action>
              <timestamp>{$timestamp}</timestamp>
              <status>LOGGED</status>
            </auditLog>
        """)
        .queryParam("userId", "${userId}")
        .queryParam("username", "${username}")
        .queryParam("action", "LOGIN")
        .queryParam("timestamp", java.time.Instant.now().toString)
        .check(xccBodyNotEmpty)
        .check(xccSubstring("LOGGED"))
        .build()
    )

  // ========================================
  // Scenario 4: Real-world workflow
  // ========================================
  val realWorldWorkflow = scenario("Real World Workflow")
    // Step 1: Search for pending orders
    .exec(
      xcc("Search Pending Orders")
        .xquery("""
          (: Return the first pending order :)
          <order>
            <orderId>ORD-99999</orderId>
            <customerId>CUST-11111</customerId>
            <items>
              <item><productId>PROD-A</productId><qty>2</qty></item>
              <item><productId>PROD-B</productId><qty>5</qty></item>
            </items>
            <total>475.50</total>
            <status>PENDING</status>
          </order>
        """)
        .check(xccSaveAs("pendingOrder"))
        .check(
          xccExtract(body => {
            import scala.xml.XML
            val xml = XML.loadString(body)
            (xml \\ "orderId").text
          }, "orderId")
        )
        .build()
    )
    .pause(200.milliseconds)
    
    // Step 2: Validate inventory using extracted order
    .exec(
      xcc("Validate Inventory")
        .xquery("""
          declare variable $orderXml external;
          
          let $order := xdmp:unquote($orderXml)
          let $items := $order/order/items/item
          
          (: Simulate inventory check :)
          let $validations :=
            for $item in $items
            let $productId := $item/productId/text()
            let $qty := $item/qty/text()
            return
              <validation>
                <productId>{$productId}</productId>
                <requestedQty>{$qty}</requestedQty>
                <availableQty>{fn:ceiling(xs:integer($qty) * 1.5)}</availableQty>
                <status>AVAILABLE</status>
              </validation>
          
          return
            <inventoryCheck>
              <orderId>{$order/order/orderId/text()}</orderId>
              <validations>{$validations}</validations>
              <overallStatus>APPROVED</overallStatus>
            </inventoryCheck>
        """)
        .queryParam("orderXml", "${pendingOrder}")
        .check(xccSubstring("APPROVED"))
        .build()
    )
    .pause(200.milliseconds)
    
    // Step 3: Update order status
    .exec(
      xcc("Update Order Status")
        .xquery("""
          declare variable $orderId external;
          
          <result>
            <orderId>{$orderId}</orderId>
            <previousStatus>PENDING</previousStatus>
            <newStatus>APPROVED</newStatus>
            <updatedAt>{fn:current-dateTime()}</updatedAt>
            <message>Order approved and ready for processing</message>
          </result>
        """)
                  .queryParam("orderId", "${orderId}")
        .check(xccSubstring("APPROVED"))
        .check(xccSubstring("ready for processing"))
        .build()
    )

  setUp(
    saveEntireXml.inject(atOnceUsers(1)),
    extractSpecificFields.inject(atOnceUsers(1)),
    moduleInvokeChain.inject(atOnceUsers(1)),
    realWorldWorkflow.inject(atOnceUsers(1))
  ).protocols(protocol)
}
