package com.marklogic.gatling.xcc.example

import io.gatling.core.Predef._
import com.marklogic.gatling.xcc.Predef._
import scala.concurrent.duration._
import scala.xml.{XML, Node, NodeSeq}
import javax.xml.xpath.{XPathFactory, XPathConstants}
import javax.xml.parsers.DocumentBuilderFactory
import java.io.StringReader
import org.xml.sax.InputSource

/**
 * Simplified examples of chaining XCC invokes with XML extraction
 * Demonstrates extracting specific XML elements using XPath and using them in subsequent requests
 */
class XmlChainSimplifiedSimulation extends Simulation {

  val protocol = xccProtocol("xccs://admin:admin@localhost:8443/Documents?authenticationPreemptive=false").build()
  
  // Helper function to extract XML element text using full XPath expressions
  private def extractXml(xpath: String, saveAs: String) = {
    xccExtract(body => {
      val factory = DocumentBuilderFactory.newInstance()
      val builder = factory.newDocumentBuilder()
      val doc = builder.parse(new InputSource(new StringReader(body)))
      val xpathExpr = XPathFactory.newInstance().newXPath()
      xpathExpr.evaluate(xpath, doc, XPathConstants.STRING).toString
    }, saveAs)
  }

  // ========================================
  // Scenario 1: Extract order ID and reuse (using full XPath)
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
        .check(extractXml("/order/orderId/text()", "orderId"))
        .check(extractXml("/order/customer/text()", "customer"))
        .check(extractXml("/order/total/text()", "total"))
        .build()
    )
    .exec(
      xcc("Process Order")
        .xquery("""
          declare variable $orderId external;
          declare variable $customer external;
          declare variable $total external;
          
          <processed>
            <orderId>{$orderId}</orderId>
            <customerName>{$customer}</customerName>
            <amount>{$total}</amount>
            <timestamp>{fn:current-dateTime()}</timestamp>
            <status>PROCESSED</status>
          </processed>
        """)
        .queryParam("orderId", "${orderId}")
        .queryParam("customer", "${customer}")
        .queryParam("total", "${total}")
        .check(xccBodyNotEmpty)
        .check(xccSubstring("PROCESSED"))
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
        .check(extractXml("//orderId/text()", "orderId"))
        .check(extractXml("//customerId/text()", "customerId"))
        .check(extractXml("//amount/text()", "amount"))
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
        .check(extractXml("//userId/text()", "userId"))
        .check(extractXml("//username/text()", "username"))
        .check(extractXml("//email/text()", "email"))
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
    // Step 1: Search for pending orders and extract key fields
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
        .check(extractXml("//orderId/text()", "orderId"))
        .check(
          xccExtract(body => {
            import scala.xml.XML
            val xml = XML.loadString(body)
            (xml \\ "item").length.toString
          }, "itemCount")
        )
        .build()
    )
    .pause(200.milliseconds)
    
    // Step 2: Validate inventory using extracted order info
    .exec(
      xcc("Validate Inventory")
        .xquery("""
          declare variable $orderId external;
          declare variable $itemCount external;
          
          <inventoryCheck>
            <orderId>{$orderId}</orderId>
            <itemCount>{$itemCount}</itemCount>
            <overallStatus>APPROVED</overallStatus>
            <checkedAt>{fn:current-dateTime()}</checkedAt>
          </inventoryCheck>
        """)
        .queryParam("orderId", "${orderId}")
        .queryParam("itemCount", "${itemCount}")
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

