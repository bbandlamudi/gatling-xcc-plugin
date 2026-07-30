/*
 * Copyright (c) 2026 Bhagat Bandlamudi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.marklogic.gatling.xcc.request.builder

import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.{Expression, Session}
import io.gatling.core.check.Check
import com.marklogic.gatling.xcc.action.XccActionBuilder
import com.marklogic.gatling.xcc.request.XccAttributes
import com.marklogic.gatling.xcc.check.XccResponse
import com.marklogic.xcc.ResultSequence
import io.gatling.core.session.el.El

case class XccRequestBuilder(
  requestName: String,
  xquery: Option[Expression[String]] = None,
  javascript: Option[Expression[String]] = None,
  module: Option[Expression[String]] = None,
  variables: Map[String, Expression[Any]] = Map.empty,
  options: Map[String, Expression[String]] = Map.empty,
  checks: List[Check[ResultSequence]] = Nil,
  xccChecks: List[Check[XccResponse]] = Nil,
  resultMapper: Option[ResultSequence => Any] = None
) {
  
  def xquery(query: Expression[String]): XccRequestBuilder = copy(xquery = Some(query))
  
  def xquery(query: String): XccRequestBuilder = 
    xquery((_: io.gatling.core.session.Session) => io.gatling.commons.validation.Success(query))
  
  // Alias for xquery() to support legacy patterns
  def search(query: Expression[String]): XccRequestBuilder = xquery(query)
  
  def search(query: String): XccRequestBuilder = xquery(query)
  
  def javascript(js: Expression[String]): XccRequestBuilder = copy(javascript = Some(js))
  
  def javascript(js: String): XccRequestBuilder = 
    javascript((_: io.gatling.core.session.Session) => io.gatling.commons.validation.Success(js))
  
  def invoke(modulePath: Expression[String]): XccRequestBuilder = copy(module = Some(modulePath))
  
  def invoke(modulePath: String): XccRequestBuilder = 
    invoke((_: io.gatling.core.session.Session) => io.gatling.commons.validation.Success(modulePath))
  
  def queryParam(name: String, value: Expression[Any]): XccRequestBuilder = 
    copy(variables = variables + (name -> value))
  
      /**
   * Add query parameter with support for:
   * - EL expressions: "${varName}" or "#{varName}" (from session)
   * - Literal values: "literal string", 123, etc.
   */
  def queryParam(name: String, value: String): XccRequestBuilder = {
    val expr: Expression[Any] = if (value.contains("${")) {
      // Parse ${varName} syntax
      val varName = value.substring(value.indexOf("${") + 2, value.indexOf("}"))
      (session: Session) => session(varName).validate[Any]
    } else if (value.contains("#{")) {
      // Parse #{varName} syntax
      val varName = value.substring(value.indexOf("#{") + 2, value.indexOf("}"))
      (session: Session) => session(varName).validate[Any]
    } else {
      (_: Session) => io.gatling.commons.validation.Success(value)
    }
    queryParam(name, expr)
  }
  
  def queryParam(name: String, value: Any): XccRequestBuilder = 
    queryParam(name, (_: Session) => io.gatling.commons.validation.Success(value))
  
  def queryParams(vars: Map[String, Expression[Any]]): XccRequestBuilder = 
    copy(variables = this.variables ++ vars)
  
  def option(name: String, value: Expression[String]): XccRequestBuilder = 
    copy(options = options + (name -> value))
  
  def option(name: String, value: String): XccRequestBuilder = 
    option(name, (_: io.gatling.core.session.Session) => io.gatling.commons.validation.Success(value))
  
  // Result transformation support
  def mapResult(mapper: ResultSequence => Any): XccRequestBuilder = 
    copy(resultMapper = Some(mapper))
  
  // Check support
  // Main check method for XccResponse checks (recommended)
  def check(check: Check[XccResponse]): XccRequestBuilder = 
    copy(xccChecks = xccChecks :+ check)
  
  // Helper for multiple XccResponse checks
  def checks(checks: Check[XccResponse]*): XccRequestBuilder = 
    copy(xccChecks = this.xccChecks ++ checks)
  
  // Legacy ResultSequence checks (for backward compatibility)
  def legacyCheck(check: Check[ResultSequence]): XccRequestBuilder = 
    copy(checks = checks :+ check)
  
  def legacyChecks(checks: Check[ResultSequence]*): XccRequestBuilder = 
    copy(checks = this.checks ++ checks)
  
  def build(): ActionBuilder = {
    val attributes = XccAttributes(
      requestName = requestName,
      xquery = xquery,
      javascript = javascript,
      module = module,
      variables = variables,
      options = options,
      checks = checks,
      xccChecks = xccChecks,
      resultMapper = resultMapper
    )
    
    new XccActionBuilder(attributes)
  }
}
