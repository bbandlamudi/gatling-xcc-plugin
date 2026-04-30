package com.marklogic.gatling.xcc.example

import io.gatling.core.Predef._
import com.marklogic.gatling.xcc.Predef._
import scala.concurrent.duration._

/**
 * Simple simulation demonstrating:
 * 1. Reading multiple documents from a result set
 * 2. Selecting specific documents by index
 * 3. Using saved documents in update operations
 */
class MultipleDocumentsSimulation extends Simulation {

  val protocol = xccProtocol("xcc://admin:admin@localhost:8000/Documents").build()

  val scn = scenario("Multiple Documents Processing")
    
    // Step 1: Insert 5 test documents
    .exec(
      xcc("Insert Test Documents")
        .xquery("""
          for $i in 1 to 5
          let $docId := fn:concat("test-doc-", $i)
          let $uri := fn:concat("/test/multi/", $docId, ".xml")
          return
            xdmp:document-insert(
              $uri,
              <document>
                <id>{$docId}</id>
                <title>Test Document {$i}</title>
                <content>Content for document {$i}</content>
                <status>DRAFT</status>
              </document>
            )
        """)
        .build()
    )
    .pause(500.milliseconds)
    
    // Step 2: Query to get only the 5 documents we just created (most recent)
    .exec(
      xcc("Get All Test Documents")
        .xquery("""
          let $docs := 
            for $uri in cts:uri-match("/test/multi/*.xml")
            let $doc := fn:doc($uri)
            order by $doc/document/id
            return $doc
          return fn:subsequence($docs, 1, 5)
        """)
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("allDocs"))
        .build()
    )
    
    // Step 3: Process results and select specific documents
    .exec(session => {
      val allDocs = session("allDocs").as[String]
      val lines = allDocs.split("\n").map(_.trim)
      
      // Filter to get only document lines (not XML declarations)
      val docLines = lines.filter(_.startsWith("<document>"))
      
      println(s"[INFO] Retrieved ${docLines.length} documents")
      
      // Select specific documents by index (1st, 3rd, and last)
      val firstDoc = if (docLines.length > 0) docLines(0) else ""
      val thirdDoc = if (docLines.length > 2) docLines(2) else ""
      val lastDoc = if (docLines.nonEmpty) docLines.last else ""
      
      println(s"[INFO] Selected first doc: ${firstDoc.take(100)}...")
      println(s"[INFO] Selected third doc: ${thirdDoc.take(100)}...")
      println(s"[INFO] Selected last doc: ${lastDoc.take(100)}...")
      
      // Combine all three documents into a single XML wrapper (no need to remove XML declaration)
      val combinedDocs = s"""
        <documents>
          <first>$firstDoc</first>
          <third>$thirdDoc</third>
          <last>$lastDoc</last>
        </documents>
      """.trim
      
      session.set("combinedDocs", combinedDocs)
    })
    .pause(500.milliseconds)
    
    // Step 4: Update all three documents in a single call using one variable
    .exec(
      xcc("Update All Selected Documents")
        .xquery("""
          declare variable $combinedDocsXml external;
          
          let $wrapper := xdmp:unquote($combinedDocsXml)
          
          let $firstDoc := $wrapper/documents/first/document
          let $firstDocId := $firstDoc/id/text()
          let $firstUri := fn:concat("/test/multi/", $firstDocId, ".xml")
          
          let $thirdDoc := $wrapper/documents/third/document
          let $thirdDocId := $thirdDoc/id/text()
          let $thirdUri := fn:concat("/test/multi/", $thirdDocId, ".xml")
          
          let $lastDoc := $wrapper/documents/last/document
          let $lastDocId := $lastDoc/id/text()
          let $lastUri := fn:concat("/test/multi/", $lastDocId, ".xml")
          
          return (
            (: Update first document to PUBLISHED :)
            if (fn:exists(fn:doc($firstUri)))
            then (
              xdmp:node-replace(
                fn:doc($firstUri)/document/status,
                <status>PUBLISHED</status>
              ),
              <result>Updated {$firstDocId} to PUBLISHED</result>
            )
            else <result>First document not found: {$firstDocId}</result>,
            
            (: Update third document to REVIEWED :)
            if (fn:exists(fn:doc($thirdUri)))
            then (
              if (fn:exists(fn:doc($thirdUri)/document/status))
              then (
                xdmp:node-replace(
                  fn:doc($thirdUri)/document/status,
                  <status>REVIEWED</status>
                ),
                <result>Updated {$thirdDocId} to REVIEWED</result>
              )
              else (
                xdmp:node-insert-child(
                  fn:doc($thirdUri)/document,
                  <status>REVIEWED</status>
                ),
                <result>Added REVIEWED status to {$thirdDocId}</result>
              )
            )
            else <result>Third document not found: {$thirdDocId}</result>,
            
            (: Update last document with timestamp :)
            if (fn:exists(fn:doc($lastUri)))
            then (
              xdmp:node-insert-child(
                fn:doc($lastUri)/document,
                <updatedAt>{fn:current-dateTime()}</updatedAt>
              ),
              <result>Added timestamp to {$lastDocId}</result>
            )
            else <result>Last document not found: {$lastDocId}</result>
          )
        """)
        .queryParam("combinedDocsXml", "${combinedDocs}")
        .check(xccBodyNotEmpty)
        .check(xccSaveAs("updateResponse"))
        .build()
    )
    .exec(session => {
      val response = session("updateResponse").as[String]
      println(s"[DEBUG] Update Response: $response")
      session
    })
    .pause(500.milliseconds)
    
    // Step 5: Verify updates by reading all documents again
    .exec(
      xcc("Verify All Updates")
        .xquery("""
          for $uri in cts:uri-match("/test/multi/*.xml")
          let $doc := fn:doc($uri)
          order by $doc/document/id
          return 
            <summary>
              <id>{$doc/document/id/text()}</id>
              <status>{$doc/document/status/text()}</status>
              <hasTimestamp>{fn:exists($doc/document/updatedAt)}</hasTimestamp>
            </summary>
        """)
        .check(xccBodyNotEmpty)
        .build()
    )
    .pause(500.milliseconds)
    
    // Step 6: Clean up - delete all test documents
    .exec(
      xcc("Delete Test Documents")
        .xquery("""
          for $uri in cts:uri-match("/test/multi/*.xml")
          return xdmp:document-delete($uri)
        """)
        .build()
    )

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(protocol)
}
