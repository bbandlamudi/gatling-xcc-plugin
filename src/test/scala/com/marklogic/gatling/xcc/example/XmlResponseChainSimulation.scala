package com.marklogic.gatling.xcc.example

import io.gatling.core.Predef._
import com.marklogic.gatling.xcc.Predef._
import scala.concurrent.duration._

/**
 * Demonstrates chaining XCC invokes where:
 * 1. First invoke creates an XML document and extracts key fields
 * 2. Fields are saved to session
 * 3. Subsequent invokes use those fields to process or retrieve the document
 */
class XmlResponseChainSimulation extends Simulation {

  val protocol = xccProtocol("xcc://admin:admin@localhost:8000/Documents").build()

  val scn = scenario("XML Response Chain")
    
    // Step 1: Create XML document and save to database, return the document ID
    .exec(
      xcc("Create XML Document")
        .xquery("""
          let $docId := "doc-" || fn:string(fn:current-dateTime())
          let $xml := 
            <order>
              <orderId>{$docId}</orderId>
              <customer>John Doe</customer>
              <items>
                <item>
                  <productId>PROD-001</productId>
                  <quantity>5</quantity>
                  <price>29.99</price>
                </item>
                <item>
                  <productId>PROD-002</productId>
                  <quantity>3</quantity>
                  <price>49.99</price>
                </item>
              </items>
              <totalAmount>299.92</totalAmount>
            </order>
          let $uri := "/orders/" || $docId || ".xml"
          let $_ := xdmp:document-insert($uri, $xml)
          (: Return the document ID for use in subsequent steps :)
          return fn:string($docId)
        """)
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("orderId"))  // Save order ID to session
        .build()
    )
    .pause(500.milliseconds)
    
    // Step 2: Retrieve and process the document using the saved order ID
    .exec(
      xcc("Process XML Document")
        .xquery("""
          declare variable $docId external;
          
          (: Retrieve the document from database :)
          let $uri := "/orders/" || $docId || ".xml"
          let $doc := fn:doc($uri)/order
          let $orderId := $doc/orderId/text()
          let $customer := $doc/customer/text()
          let $itemCount := fn:count($doc/items/item)
          let $total := $doc/totalAmount/text()
          
          return 
            <processedOrder>
              <id>{$orderId}</id>
              <customerName>{$customer}</customerName>
              <numberOfItems>{$itemCount}</numberOfItems>
              <total>{$total}</total>
              <processedAt>{fn:current-dateTime()}</processedAt>
              <status>PROCESSED</status>
            </processedOrder>
        """)
        .queryParam("docId", "${orderId}")  // Use saved order ID from session
        .check(xccBodyNotEmpty)
        .check(xccSubstring("PROCESSED"))
        .build()
    )
    .pause(500.milliseconds)
    
    // Step 3: Update the document status using the saved order ID
    .exec(
      xcc("Update Document Status")
        .xquery("""
          declare variable $docId external;
          
          let $uri := "/orders/" || $docId || ".xml"
          let $doc := fn:doc($uri)/order
          let $updatedDoc := 
            <order>
              {$doc/*}
              <status>COMPLETED</status>
              <updatedAt>{fn:current-dateTime()}</updatedAt>
            </order>
          let $_ := xdmp:document-insert($uri, $updatedDoc)
          
          return "Document updated at: " || $uri
        """)
        .queryParam("docId", "${orderId}")
        .check(xccSubstring("Document updated"))
        .build()
    )

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(protocol)
}

