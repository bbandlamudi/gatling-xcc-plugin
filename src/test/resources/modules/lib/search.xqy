xquery version "1.0-ml";

declare variable $query as xs:string external;
declare variable $pageSize as xs:integer external;
declare variable $pageNumber as xs:integer external;

let $start := (($pageNumber - 1) * $pageSize) + 1
let $end := $pageNumber * $pageSize

let $results := cts:search(
  fn:doc(),
  cts:word-query($query),
  ("unfiltered", "score-simple")
)/element()

let $total := fn:count($results)
let $page-results := fn:subsequence($results, $start, $pageSize)

return $page-results 
