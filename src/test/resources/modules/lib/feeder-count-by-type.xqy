xquery version "1.0-ml";

(:
 : Module: feeder-count-by-type.xqy
 : Purpose: Count documents by document type
 : Parameters:
 :   - docType: The document type to count
 :
 : Usage: Invoked by FeederWithSessionSimulation from CSV feeder data
 :)

declare variable $docType as xs:string external;

let $count := fn:count(
  cts:search(
    fn:collection(), 
    cts:element-value-query(xs:QName("type"), $docType), 
    ("unfiltered"), 
    0
  )
)

let $sample-docs := cts:search(
  fn:collection(), 
  cts:element-value-query(xs:QName("type"), $docType), 
  ("unfiltered"), 
  0
)[1 to 5]

return
  <typeCount>
    <type>{$docType}</type>
    <count>{$count}</count>
    <sampleDocuments>
    {
      for $doc in $sample-docs
      let $uri := fn:base-uri($doc)
      return
        <document>
          <uri>{$uri}</uri>
          <name>{$doc/document/name/text()}</name>
          <id>{$doc/document/id/text()}</id>
        </document>
    }
    </sampleDocuments>
  </typeCount>
