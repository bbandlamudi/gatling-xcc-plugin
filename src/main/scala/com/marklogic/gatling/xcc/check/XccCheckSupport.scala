package com.marklogic.gatling.xcc.check

import io.gatling.core.check.Check
import com.marklogic.xcc.ResultSequence

/**
 * DSL support for XCC checks
 */
trait XccCheckSupport {
  
  // Convenience checks
  val xccSubstring: String => XccCheck = XccCheckBuilders.substring
  val xccRegex: String => XccCheck = XccCheckBuilders.regex
    val xccBodyNotEmpty: XccCheck = XccCheckBuilders.bodyNotEmpty
  val xccBodyEquals: String => XccCheck = XccCheckBuilders.bodyEquals
  val xccSaveAs: String => XccCheck = XccCheckBuilders.saveAs
  val xccSaveFirstItemAs: String => XccCheck = XccCheckBuilders.saveFirstItemAs
  val xccXPath: String => XccCheck = XccCheckBuilders.xpath
  val xccJsonPath: String => XccCheck = XccCheckBuilders.jsonPath
  val xccCount: Int => XccCheck = XccCheckBuilders.count
  val xccNoError: XccCheck = XccCheckBuilders.noError
  val xccResponseTime: Long => XccCheck = XccCheckBuilders.responseTime
  
  // Advanced extraction
  def xccExtract(extractor: String => String, saveAs: String): XccCheck = 
    XccCheckBuilders.extract(extractor, saveAs)
  
  // Legacy compatibility aliases
  def singleResponse[T]: Check[ResultSequence] = XccCheckBuilders.singleResponse.asInstanceOf[Check[ResultSequence]]
  val xccExists: XccCheck = XccCheckBuilders.exists
  
}

object XccCheckSupport extends XccCheckSupport
