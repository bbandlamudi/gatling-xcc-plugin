xquery version "1.0-ml";

(:
 : Module: feeder-search.xqy
 : Purpose: Search for documents based on search term and document type
 : Parameters:
 :   - searchTerm: The term to search for
 :   - docType: The document type to filter by
 :   - maxResults: Maximum number of results to return
 :
 : Usage: Invoked by FeederWithSessionSimulation from CSV feeder data
 :)

declare variable $searchTerm as xs:string external;
declare variable $docType as xs:string external;
declare variable $maxResults as xs:string external;

let $query := cts:word-query($searchTerm)
let $type-query := cts:element-value-query(xs:QName("type"), $docType)
let $combined-query := cts:and-query(($query, $type-query))
let $results := cts:search(
  fn:collection(), 
  $combined-query, 
  ("unfiltered"), 
  0
)[1 to xs:integer($maxResults)]

return
  <searchResults>
    <query>{$searchTerm}</query>
    <docType>{$docType}</docType>
    <maxResults>{$maxResults}</maxResults>
    <count>{fn:count($results)}</count>
    <documents>
    {
      for $doc in $results
      let $uri := fn:base-uri($doc)
      return
        <document>
          <uri>{$uri}</uri>
          <score>{cts:score($doc)}</score>
        </document>
    }
    </documents>
  </searchResults>
