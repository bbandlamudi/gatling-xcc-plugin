package com.marklogic.gatling.xcc.example

import io.gatling.core.Predef._
import com.marklogic.gatling.xcc.Predef._
import scala.concurrent.duration._
import scala.xml.XML

/**
 * Demonstrates extracting specific XML elements using XPath and using them in subsequent invokes
 * 
 * Example flows:
 * 1. Get XML response -> Extract value via XPath -> Use in next invoke
 * 2. Get order XML -> Extract orderId and customerId -> Create invoice
 * 3. Query document -> Extract specific field -> Update related document
 * 4. XML with namespaces -> Extract using namespace-aware XPath -> Use values
 */
class XPathExtractAndReuseSimulation extends Simulation {

  val protocol = xccProtocol("xcc://admin:admin@localhost:8000/Documents").build()

  val scn = scenario("XPath Extract and Reuse")
    
    // ========================================
    // Example 1: Simple XPath extraction
    // ========================================
    .exec(
      xcc("Get Customer Order")
        .xquery("""
          <order>
            <orderId>ORD-12345</orderId>
            <customerId>CUST-9876</customerId>
            <customerName>Jane Smith</customerName>
            <orderDate>2024-01-15</orderDate>
            <items>
              <item>
                <productId>PROD-001</productId>
                <productName>Widget</productName>
                <quantity>10</quantity>
                <price>25.50</price>
              </item>
            </items>
            <totalAmount>255.00</totalAmount>
            <status>PENDING</status>
          </order>
        """)
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("orderXml"))
        .build()
    )
    .pause(200.milliseconds)
    
    // Extract orderId using custom extractor
    .exec(session => {
      val orderXml = session("orderXml").as[String]
      val xml = XML.loadString(orderXml)
      val orderId = (xml \\ "orderId").text
      val customerId = (xml \\ "customerId").text
      val totalAmount = (xml \\ "totalAmount").text
      
      session
        .set("orderId", orderId)
        .set("customerId", customerId)
        .set("totalAmount", totalAmount)
    })
    .exec(session => {
      println(s"[DEBUG] Extracted orderId: ${session("orderId").as[String]}")
      println(s"[DEBUG] Extracted customerId: ${session("customerId").as[String]}")
      println(s"[DEBUG] Extracted totalAmount: ${session("totalAmount").as[String]}")
      session
    })
    
    // Use extracted values in next invoke
    .exec(
      xcc("Create Invoice from Order")
        .xquery("""
          declare variable $orderId external;
          declare variable $customerId external;
          declare variable $amount external;
          
          let $invoiceId := "INV-" || fn:substring-after($orderId, "ORD-")
          let $invoice :=
            <invoice>
              <invoiceId>{$invoiceId}</invoiceId>
              <orderId>{$orderId}</orderId>
              <customerId>{$customerId}</customerId>
              <amount>{$amount}</amount>
              <invoiceDate>{fn:current-date()}</invoiceDate>
              <dueDate>{fn:current-date() + xs:dayTimeDuration('P30D')}</dueDate>
              <status>ISSUED</status>
            </invoice>
          
          return (
            (: Insert invoice into database :)
            xdmp:document-insert(
              "/invoices/" || $invoiceId || ".xml",
              $invoice
            ),
            $invoice
          )
        """)
        .queryParam("orderId", "${orderId}")
        .queryParam("customerId", "${customerId}")
        .queryParam("amount", "${totalAmount}")
        .check(xccBodyNotEmpty)
        .check(xccSubstring("ISSUED"))
        .check(xccSaveAs("invoiceXml"))
        .build()
    )
    .pause(500.milliseconds)
    
    // ========================================
    // Example 2: Extract multiple items
    // ========================================
    .exec(
      xcc("Get Product Catalog")
        .xquery("""
          <catalog>
            <products>
              <product>
                <id>PROD-001</id>
                <name>Widget</name>
                <category>Electronics</category>
                <price>25.50</price>
                <stock>100</stock>
              </product>
              <product>
                <id>PROD-002</id>
                <name>Gadget</name>
                <category>Electronics</category>
                <price>49.99</price>
                <stock>50</stock>
              </product>
              <product>
                <id>PROD-003</id>
                <name>Tool</name>
                <category>Hardware</category>
                <price>15.00</price>
                <stock>200</stock>
              </product>
            </products>
          </catalog>
        """)
        .check(xccSaveAs("catalogXml"))
        .build()
    )
    .pause(200.milliseconds)
    
    // Extract first product ID and price
    .exec(session => {
      val catalogXml = session("catalogXml").as[String]
      val xml = XML.loadString(catalogXml)
      val firstProduct = (xml \\ "product").head
      val productId = (firstProduct \ "id").text
      val productName = (firstProduct \ "name").text
      val productPrice = (firstProduct \ "price").text
      
      session
        .set("firstProductId", productId)
        .set("firstProductName", productName)
        .set("firstProductPrice", productPrice)
    })
    .exec(session => {
      println(s"[DEBUG] First product - ID: ${session("firstProductId").as[String]}, " +
              s"Name: ${session("firstProductName").as[String]}, " +
              s"Price: ${session("firstProductPrice").as[String]}")
      session
    })
    
    // Create order for extracted product
    .exec(
      xcc("Create Order for Product")
        .xquery("""
          declare variable $productId external;
          declare variable $productName external;
          declare variable $price external;
          declare variable $quantity external;
          
          let $orderId := "ORD-" || fn:string(xdmp:random())
          let $priceNum := try { xs:decimal($price) } catch ($e) { 0.0 }
          let $quantityNum := try { xs:integer($quantity) } catch ($e) { 0 }
          let $totalPrice := $priceNum * $quantityNum
          
          let $order :=
            <order>
              <orderId>{$orderId}</orderId>
              <customerId>CUST-DEFAULT</customerId>
              <orderDate>{fn:current-dateTime()}</orderDate>
              <items>
                <item>
                  <productId>{$productId}</productId>
                  <productName>{$productName}</productName>
                  <quantity>{$quantityNum}</quantity>
                  <unitPrice>{$priceNum}</unitPrice>
                  <totalPrice>{$totalPrice}</totalPrice>
                </item>
              </items>
              <totalAmount>{$totalPrice}</totalAmount>
              <status>NEW</status>
            </order>
          
          return $order
        """)
        .queryParam("productId", "${firstProductId}")
        .queryParam("productName", "${firstProductName}")
        .queryParam("price", "${firstProductPrice}")
        .queryParam("quantity", "5")
        .check(xccBodyNotEmpty)
        .check(xccSubstring("NEW"))
        .build()
    )
    .pause(500.milliseconds)
    
    // ========================================
    // Example 3: Extract and update pattern
    // ========================================
    .exec(
      xcc("Get Document Status")
        .xquery("""
          <document>
            <docId>DOC-98765</docId>
            <title>Important Document</title>
            <status>DRAFT</status>
            <version>1</version>
            <author>
              <userId>USER-123</userId>
              <name>John Doe</name>
            </author>
            <lastModified>2024-01-15T10:30:00</lastModified>
          </document>
        """)
        .check(xccSaveAs("documentXml"))
        .build()
    )
    .pause(200.milliseconds)
    
    // Extract document details
    .exec(session => {
      val docXml = session("documentXml").as[String]
      val xml = XML.loadString(docXml)
      
      val docId = (xml \\ "docId").text
      val currentVersion = (xml \\ "version").text.toInt
      val userId = (xml \\ "author" \ "userId").text
      val currentStatus = (xml \\ "status").text
      
      session
        .set("docId", docId)
        .set("currentVersion", currentVersion)
        .set("nextVersion", currentVersion + 1)
        .set("userId", userId)
        .set("oldStatus", currentStatus)
    })
    .exec(session => {
      println(s"[DEBUG] Document ${session("docId").as[String]} - " +
              s"Version: ${session("currentVersion").as[Int]} -> ${session("nextVersion").as[Int]}, " +
              s"Status: ${session("oldStatus").as[String]} -> PUBLISHED")
      session
    })
    
    // Update document with extracted values
    .exec(
      xcc("Publish Document")
        .xquery("""
          declare variable $docId external;
          declare variable $version external;
          declare variable $userId external;
          
          <document>
            <docId>{$docId}</docId>
            <title>Important Document</title>
            <status>PUBLISHED</status>
            <version>{$version}</version>
            <author>
              <userId>{$userId}</userId>
              <name>John Doe</name>
            </author>
            <lastModified>{fn:current-dateTime()}</lastModified>
            <publishedDate>{fn:current-dateTime()}</publishedDate>
            <publishedBy>{$userId}</publishedBy>
          </document>
        """)
                  .queryParam("docId", "${docId}")
          .queryParam("version", "${nextVersion}")
          .queryParam("userId", "${userId}")
        .check(xccBodyNotEmpty)
        .check(xccSubstring("PUBLISHED"))
        .check(xccSubstring("publishedDate"))
        .build()
    )
    .pause(500.milliseconds)
    
    // ========================================
    // Example 4: XML with Namespaces
    // ========================================
    .exec(
      xcc("Get Namespaced Book Data")
        .xquery("""
          declare namespace bk="http://example.com/book";
          declare namespace au="http://example.com/author";
          
          <bk:book xmlns:bk="http://example.com/book" xmlns:au="http://example.com/author">
            <bk:isbn>978-1234567890</bk:isbn>
            <bk:title>XQuery in Action</bk:title>
            <bk:price currency="USD">49.99</bk:price>
            <au:author>
              <au:name>John Smith</au:name>
              <au:id>AUTH-001</au:id>
            </au:author>
          </bk:book>
        """)
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("bookXml"))
        .build()
    )
    .pause(200.milliseconds)
    
    // Extract from namespaced XML in Scala (namespace-aware)
    .exec(session => {
      val bookXml = session("bookXml").as[String]
      // Use regex to extract from namespaced XML since Scala XML doesn't handle namespaces well in this way
      val isbnPattern = """<[^:]+:isbn>([^<]+)</[^:]+:isbn>""".r
      val authorIdPattern = """<[^:]+:id>([^<]+)</[^:]+:id>""".r
      
      val isbn = isbnPattern.findFirstMatchIn(bookXml).map(_.group(1)).getOrElse("")
      val authorId = authorIdPattern.findFirstMatchIn(bookXml).map(_.group(1)).getOrElse("")
      
      session
        .set("bookIsbn", isbn)
        .set("authorId", authorId)
    })
    .exec(session => {
      println(s"[DEBUG] Extracted from namespaced XML - ISBN: ${session("bookIsbn").as[String]}, AuthorID: ${session("authorId").as[String]}")
      session
    })
    
    // Use extracted values in namespaced response
    .exec(
      xcc("Create Order for Namespaced Book")
        .xquery("""
          declare namespace ord="http://example.com/order";
          declare variable $isbn external;
          declare variable $authorId external;
          
          <ord:order xmlns:ord="http://example.com/order">
            <ord:orderId>{'ORD-' || fn:string(xdmp:random())}</ord:orderId>
            <ord:isbn>{$isbn}</ord:isbn>
            <ord:authorId>{$authorId}</ord:authorId>
            <ord:quantity>1</ord:quantity>
            <ord:orderDate>{fn:current-date()}</ord:orderDate>
          </ord:order>
        """)
        .queryParam("isbn", "${bookIsbn}")
        .queryParam("authorId", "${authorId}")
        .check(xccBodyNotEmpty)
        .check(xccSubstring("ord:isbn"))
        .check(xccSubstring("ord:authorId"))
        .build()
    )

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(protocol)
}

