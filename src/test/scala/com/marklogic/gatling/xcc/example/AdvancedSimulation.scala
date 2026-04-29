package com.marklogic.gatling.xcc.example

import io.gatling.core.Predef._
import com.marklogic.gatling.xcc.Predef._
import scala.concurrent.duration._

/**
 * Advanced simulation demonstrating document CRUD operations
 */
class AdvancedSimulation extends Simulation {

  val protocol = xccProtocol("xcc://admin:admin@localhost:8000/Documents").build()

  // Feeder for generating test data
  val documentFeeder = Iterator.continually(Map(
    "docId" -> java.util.UUID.randomUUID().toString,
    "title" -> s"Document ${scala.util.Random.nextInt(10000)}",
    "content" -> s"Content ${scala.util.Random.alphanumeric.take(100).mkString}"
  ))

  val scn = scenario("Document CRUD Operations")
    .feed(documentFeeder)
    
    // Insert document
    .exec(
      xcc("Insert Document")
        .xquery("""
          declare variable $docId external;
          declare variable $title external;
          declare variable $content external;
          
          xdmp:document-insert(
            fn:concat("/test/", $docId, ".xml"),
            <document>
              <id>{$docId}</id>
              <title>{$title}</title>
              <content>{$content}</content>
              <timestamp>{fn:current-dateTime()}</timestamp>
            </document>
          )
        """)
        .queryParam("docId", "${docId}")
        .queryParam("title", "${title}")
        .queryParam("content", "${content}")
        .option("timeout", "10000")
        .build()
    )
    .pause(500.milliseconds)
    
               // Read document and save first item to session
    .exec(
      xcc("Read Document")
        .xquery("""
          declare variable $docId external;
          fn:doc(fn:concat("/test/", $docId, ".xml"))
        """)
        .queryParam("docId", "${docId}")
        .check(xccBodyNotEmpty)
        .check(xccSaveFirstItemAs("savedDocument"))
        .build()
    )
    .exec(session => {
      println(s"[DEBUG] Session after Read: savedDocument = ${session("savedDocument").asOption[String].map(_.take(100)).getOrElse("NOT SET")}...")
      session
    })
    .pause(500.milliseconds)
    
    // Update document - extract docId from saved document and use it
    .exec(
      xcc("Update Document")
        .xquery("""
          declare variable $documentXml external;
          declare variable $newContent external;
          
          let $doc := xdmp:unquote($documentXml)
          let $extractedDocId := $doc/document/id/text()
          let $uri := fn:concat("/test/", $extractedDocId, ".xml")
          let $existingDoc := fn:doc($uri)
          return
            if (fn:exists($existingDoc))
            then (
              xdmp:node-replace(
                $existingDoc/document/content,
                <content>{$newContent}</content>
              ),
              fn:concat("Updated document with ID: ", $extractedDocId)
            )
            else "Document not found"
        """)
        .queryParam("documentXml", "${savedDocument}")
        .queryParam("newContent", "Updated content")
        .build()
    )
    .pause(500.milliseconds)
    
    // Delete document
    .exec(
      xcc("Delete Document")
        .xquery("""
          declare variable $docId external;
          let $uri := fn:concat("/test/", $docId, ".xml")
          return xdmp:document-delete($uri)
        """)
        .queryParam("docId", "${docId}")
        .build()
    )

  /*setUp(
    scn.inject(
      rampUsersPerSec(1).to(10).during(1.minute),
      constantUsersPerSec(10).during(2.minutes),
      rampUsersPerSec(10).to(1).during(1.minute)
    )
  ).protocols(protocol)
   .assertions(
     global.responseTime.percentile3.lt(3000),
     global.successfulRequests.percent.gt(99)
   )*/
   setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(protocol)
}

