package com.marklogic.gatling.xcc

import com.marklogic.gatling.xcc.protocol.XccProtocolBuilder
import com.marklogic.gatling.xcc.request.builder.XccRequestBuilder
import com.marklogic.gatling.xcc.check.XccCheckSupport

object Predef extends XccCheckSupport {
  
  def xccProtocol(url: String): XccProtocolBuilder = XccProtocolBuilder(url)
  
  def xcc(requestName: String): XccRequestBuilder = XccRequestBuilder(requestName)
  
  // Type aliases for user convenience
  type XccProtocolBuilder = com.marklogic.gatling.xcc.protocol.XccProtocolBuilder
  type XccProtocol = com.marklogic.gatling.xcc.protocol.XccProtocol
  type XccCheck = com.marklogic.gatling.xcc.check.XccCheck
  type XccResponse = com.marklogic.gatling.xcc.check.XccResponse
}
