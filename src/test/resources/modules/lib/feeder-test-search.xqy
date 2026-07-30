xquery version "1.0-ml";

(:
 : Module: feeder-test-search.xqy
 : Purpose: Search for documents in /test/ directory based on search term
 : Parameters:
 :   - searchTerm: The term to search for
 :
 : Usage: Invoked by FeederWithSessionSimulation from CSV feeder data
 :)

declare variable $searchTerm as xs:string external;

let $query := cts:word-query($searchTerm)
let $dir-query := cts:directory-query("/test/", "infinity")
let $combined := cts:and-query(($query, $dir-query))
let $results := cts:search(fn:collection(), $combined, ("unfiltered"), 0)

return
  <testResults>
    <query>{$searchTerm}</query>
    <directory>/test/</directory>
    <found>{fn:count($results)}</found>
    <documents>
    {
      for $doc in $results
      let $uri := fn:base-uri($doc)
      return
        <document>
          <uri>{$uri}</uri>
          <title>{$doc/document/title/text()}</title>
          <category>{$doc/document/category/text()}</category>
        </document>
    }
    </documents>
  </testResults>
