package io.gatling.xcc

import io.gatling.core.Predef._
import io.gatling.xcc.Predef._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class XccPluginTest extends AnyFlatSpec with Matchers {

  "XccProtocolBuilder" should "build a protocol with basic configuration" in {

    val protocol = xccProtocol("xcc://localhost:8000")
      .username("admin")
      .password("admin")
      .database("Documents")
      .build()
    
    protocol should not be null
    protocol.uri should be("xcc://admin:admin@localhost:8000/Documents")
  }

  "XccRequestBuilder" should "build a request with XQuery" in {
    val requestBuilder = xcc("Test Query")
      .xquery("xdmp:database-name(xdmp:database())")
    
    requestBuilder should not be null
    requestBuilder.requestName should be("Test Query")
  }

  it should "build a request with variables" in {
    val requestBuilder = xcc("Parameterized Query")
      .xquery("declare variable $name external; $name")
      .queryParam("name", "John")
      .queryParam("age", 30)
    
    requestBuilder.variables should have size 2
    requestBuilder.variables.keys should contain allOf ("name", "age")
  }

  it should "build a request with JavaScript" in {
    val requestBuilder = xcc("JS Query")
      .javascript("cts.doc('/test.json')")
    
    requestBuilder.javascript should not be None
  }

  it should "build a request with module invocation" in {
    val requestBuilder = xcc("Module Invocation")
      .invoke("/modules/test.xqy")
      .queryParam("param", "value")
    
    requestBuilder.module should not be None
  }

  it should "build a request with options" in {
    val requestBuilder = xcc("Query with Options")
      .xquery("xdmp:sleep(1000)")
      .option("timeout", "5000")
      .option("cacheable", "true")
    
    requestBuilder.options should have size 2
  }
}
