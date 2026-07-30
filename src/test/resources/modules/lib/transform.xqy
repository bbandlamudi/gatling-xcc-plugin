xquery version "1.0-ml";
import module namespace json="http://marklogic.com/xdmp/json" at "/MarkLogic/json/json.xqy";

declare variable $inputDoc as xs:string external;
declare variable $format as xs:string external;

let $config := json:config("full") => map:with("whitespace", "ignore")
let $doc := xdmp:unquote($inputDoc)
return if($format eq "json") then json:transform-to-json($doc, $config) else fn:error(xs:QName("Error"),"Invalid format")
