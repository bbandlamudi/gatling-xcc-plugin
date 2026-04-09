package io.gatling.xcc

import io.gatling.xcc.protocol.XccProtocolBuilder
import io.gatling.xcc.request.builder.XccRequestBuilder
import io.gatling.xcc.check.XccCheckSupport

object Predef extends XccCheckSupport {
  
  def xccProtocol(url: String): XccProtocolBuilder = XccProtocolBuilder(url)
  
  def xcc(requestName: String): XccRequestBuilder = XccRequestBuilder(requestName)
  
  // Type aliases for user convenience
  type XccProtocolBuilder = io.gatling.xcc.protocol.XccProtocolBuilder
  type XccProtocol = io.gatling.xcc.protocol.XccProtocol
  type XccCheck = io.gatling.xcc.check.XccCheck
  type XccResponse = io.gatling.xcc.check.XccResponse
}
