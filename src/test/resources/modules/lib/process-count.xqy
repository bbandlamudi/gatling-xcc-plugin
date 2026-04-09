xquery version "1.0-ml";

(:
 : Module: process-count.xqy
 : Purpose: Process document count from previous query and provide analysis
 : Parameters:
 :   - count: The document count from previous query (can be XML or string)
 :   - docType: The document type being analyzed
 :   - testResults: Optional test results XML from previous step
 :
 : Usage: Invoked by FeederWithSessionSimulation with output from feeder-count-by-type.xqy
 :)

declare variable $count as xs:string external;
declare variable $docType as xs:string external;
declare variable $testResults as xs:string? external := ();

(: Extract count value - handle both XML and plain string :)
let $count-num := 
  try {
    if (fn:starts-with($count, "<")) then
      (: Parse as XML and extract count element :)
      let $xml := xdmp:unquote($count)
      return xs:integer($xml//*:count/text())
    else
      (: Direct string to integer conversion :)
      xs:integer($count)
  } catch ($e) {
    0
  }

let $status := 
  if ($count-num = 0) then "EMPTY"
  else if ($count-num < 10) then "LOW"
  else if ($count-num < 50) then "MEDIUM"
  else "HIGH"

let $recommendation :=
  if ($count-num = 0) then "No documents found. Consider adding data."
  else if ($count-num < 10) then "Low volume detected. May need more documents."
  else if ($count-num < 50) then "Medium volume. Adequate for testing."
  else "High volume. Good for load testing."

return
  <countAnalysis>
    <docType>{$docType}</docType>
    <count>{$count-num}</count>
    <status>{$status}</status>
    <recommendation>{$recommendation}</recommendation>
    <timestamp>{fn:current-dateTime()}</timestamp>
    {
      if (fn:exists($testResults) and fn:string-length($testResults) > 0) then
        <testResultsReceived>true</testResultsReceived>
      else
        ()
    }
  </countAnalysis>
