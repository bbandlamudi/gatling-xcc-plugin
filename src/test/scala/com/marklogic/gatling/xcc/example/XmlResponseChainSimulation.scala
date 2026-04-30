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
          let $docId := "doc-" || xdmp:random()
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
        .check(xccSaveFirstItemAs("orderId"))  // Save order ID to session
        .build()
    )
    .pause(500.milliseconds)
    
    // Step 2: Retrieve and process the document using the saved order ID
    .exec(
      xcc("Process XML Document")
        .xquery("""
          declare variable $docId external;
          
          (: Construct URI and check if document exists :)
          let $uri := "/orders/" || $docId || ".xml"
          let $doc-exists := fn:doc-available($uri)
          
          return
            if ($doc-exists) then
              let $order := fn:doc($uri)//order
              let $orderId := $order/orderId/text()
              let $customer := $order/customer/text()
              let $itemCount := fn:count($order/items/item)
              let $total := $order/totalAmount/text()
              return
                <processedOrder>
                  <id>{$orderId}</id>
                  <customerName>{$customer}</customerName>
                  <numberOfItems>{$itemCount}</numberOfItems>
                  <total>{$total}</total>
                  <processedAt>{fn:current-dateTime()}</processedAt>
                  <status>PROCESSED</status>
                </processedOrder>
            else
              <error>Document not found at URI: {$uri}</error>
        """)
        .queryParam("docId", "${orderId}")  // Use saved order ID from session
        .check(xccBodyNotEmpty)
        .build()
    )
    .pause(500.milliseconds)
    
    // Step 3: Update the document status using the saved order ID
    .exec(
      xcc("Update Document Status")
        .xquery("""
          declare variable $docId external;
          
          let $uri := "/orders/" || $docId || ".xml"
          let $doc-exists := fn:doc-available($uri)
          
          return
            if ($doc-exists) then
              let $doc := fn:doc($uri)//order
              let $updatedDoc := 
                <order>
                  {$doc/*}
                  <status>COMPLETED</status>
                  <updatedAt>{fn:current-dateTime()}</updatedAt>
                </order>
              let $_ := xdmp:document-insert($uri, $updatedDoc)
              return "Document updated at: " || $uri
            else
              "Document not found at: " || $uri
        """)
        .queryParam("docId", "${orderId}")
        .check(xccSubstring("Document updated"))
        .build()
    )

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(protocol)
}

