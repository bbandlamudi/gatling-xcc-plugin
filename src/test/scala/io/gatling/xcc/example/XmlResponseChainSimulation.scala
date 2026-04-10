package io.gatling.xcc.example

import io.gatling.core.Predef._
import io.gatling.xcc.Predef._
import scala.concurrent.duration._

/**
 * Demonstrates chaining XCC invokes where:
 * 1. First invoke returns XML response
 * 2. XML response is saved to session
 * 3. Second invoke uses that XML as input
 */
class XmlResponseChainSimulation extends Simulation {

  val protocol = xccProtocol("xcc://admin:admin@localhost:8000/Documents").build()

  val scn = scenario("XML Response Chain")
    
    // Step 1: Create and return an XML document
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
          return $xml
        """)
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("xmlDocument"))  // Save entire XML response to session
        .build()
    )
    .pause(500.milliseconds)
    
    // Step 2: Use the saved XML as input for another invoke
    .exec(
      xcc("Process XML Document")
        .xquery("""
          declare variable $xmlInput external;
          
          (: Parse the XML string :)
          let $doc := xdmp:unquote($xmlInput)
          let $orderId := $doc/order/orderId/text()
          let $customer := $doc/order/customer/text()
          let $itemCount := fn:count($doc/order/items/item)
          let $total := $doc/order/totalAmount/text()
          
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
        .queryParam("xmlInput", "${xmlDocument}")  // Use saved XML from session
        .check(xccBodyNotEmpty)
        .check(xccSubstring("PROCESSED"))
        .build()
    )
    .pause(500.milliseconds)
    
    // Alternative: Use the XML to insert into database
    .exec(
      xcc("Insert XML to Database")
        .xquery("""
          declare variable $xmlContent external;
          
          let $doc := xdmp:unquote($xmlContent)
          let $orderId := $doc/order/orderId/text()
          let $uri := "/orders/" || $orderId || ".xml"
          
          return (
            xdmp:document-insert($uri, $doc),
            "Document inserted at: " || $uri
          )
        """)
        .queryParam("xmlContent", "${xmlDocument}")
        .check(xccSubstring("Document inserted"))
        .build()
    )

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(protocol)
}
