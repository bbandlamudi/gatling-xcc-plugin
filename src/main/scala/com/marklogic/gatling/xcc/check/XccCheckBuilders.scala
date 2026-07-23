package com.marklogic.gatling.xcc.check

import io.gatling.commons.validation.{Failure, Success, Validation}
import io.gatling.core.check.{Check, CheckResult}
import io.gatling.core.session.{Expression, Session}
import scala.xml.XML
import scala.util.{Try, Success => TrySuccess, Failure => TryFailure}

object XccCheckBuilders {
  
  private abstract class BaseXccCheck extends XccCheck {
    override def checkIf(condition: Expression[Boolean]): Check[XccResponse] = this
    override def checkIf(condition: (XccResponse, Session) => Validation[Boolean]): Check[XccResponse] = this
  }
  
  def simpleCheck(f: String => Boolean, errorMsg: String): XccCheck = new BaseXccCheck {
    override def check(response: XccResponse, session: Session, preparedCache: Check.PreparedCache): Validation[CheckResult] = {
      if (f(response.body)) {
        Success(CheckResult(Some(response.body), None))
      } else {
        Failure(errorMsg)
      }
    }
  }
  
  def substring(expected: String): XccCheck = simpleCheck(
    _.contains(expected),
    s"Response does not contain: '$expected'"
  )
  
  def regex(pattern: String): XccCheck = new BaseXccCheck {
    private val compiledPattern = pattern.r
    
    override def check(response: XccResponse, session: Session, preparedCache: Check.PreparedCache): Validation[CheckResult] = {
      compiledPattern.findFirstIn(response.body) match {
        case Some(matched) => Success(CheckResult(Some(matched), None))
        case None => Failure(s"Response does not match pattern: '$pattern'")
      }
    }
  }
  
  def bodyNotEmpty: XccCheck = simpleCheck(
    _.nonEmpty,
    "Response body is empty"
  )
  
  def bodyEquals(expected: String): XccCheck = simpleCheck(
    _ == expected,
    s"Response body does not equal expected value"
  )
  
      // Save response to session - returns the extracted value with session key
  def saveAs(key: String): XccCheck = new BaseXccCheck {
    override def check(response: XccResponse, session: Session, preparedCache: Check.PreparedCache): Validation[CheckResult] = {
      // Return the full body value with session key - Gatling framework will save it
      Success(CheckResult(Some(response.body), Some(key)))
    }
  }
  
  // Save specific item at index to session (0-based index)
  def saveItemAs(index: Int, key: String): XccCheck = new BaseXccCheck {
    override def check(response: XccResponse, session: Session, preparedCache: Check.PreparedCache): Validation[CheckResult] = {
      if (response.items.isEmpty) {
        Failure("Result set is empty, no items to save")
      } else if (index < 0 || index >= response.items.length) {
        Failure(s"Index $index out of bounds. Result set contains ${response.items.length} item(s)")
      } else {
        val item = response.items(index)
        Success(CheckResult(Some(item), Some(key)))
      }
    }
  }
  
  // Check if at least one item exists and save only the first item to session
  def saveFirstItemAs(key: String): XccCheck = saveItemAs(0, key)
  
  // XPath check for XML responses
  def xpath(expression: String): XccCheck = new BaseXccCheck {
    override def check(response: XccResponse, session: Session, preparedCache: Check.PreparedCache): Validation[CheckResult] = {
      Try {
        val xml = XML.loadString(response.body)
        val result = (xml \\ expression).text
        if (result.nonEmpty) {
          Success(CheckResult(Some(result), None))
        } else {
          Failure(s"XPath expression '$expression' returned no results")
        }
      } match {
        case TrySuccess(v) => v
        case TryFailure(ex) => Failure(s"XPath evaluation failed: ${ex.getMessage}")
      }
    }
  }
  
  // Count the number of items/lines in response
  def count(expected: Int): XccCheck = new BaseXccCheck {
    override def check(response: XccResponse, session: Session, preparedCache: Check.PreparedCache): Validation[CheckResult] = {
      val count = response.body.split("\n").filter(_.trim.nonEmpty).length
      if (count == expected) {
        Success(CheckResult(Some(count), None))
      } else {
        Failure(s"Expected $expected items but got $count")
      }
    }
  }
  
  // Check response contains no error indicators
  def noError: XccCheck = simpleCheck(
    body => !body.toLowerCase.contains("error") && !body.toLowerCase.contains("exception"),
    "Response contains error or exception"
  )
  
  // Extract value - returns the extracted value with session key
  def extract(extractor: String => String, saveAsKey: String): XccCheck = new BaseXccCheck {
    override def check(response: XccResponse, session: Session, preparedCache: Check.PreparedCache): Validation[CheckResult] = {
      Try(extractor(response.body)) match {
        case TrySuccess(extracted) =>
          Success(CheckResult(Some(extracted), Some(saveAsKey)))
        case TryFailure(ex) => Failure(s"Extraction failed: ${ex.getMessage}")
      }
    }
  }
  
  // JSON path-like check (simple implementation)
  def jsonPath(path: String): XccCheck = new BaseXccCheck {
    override def check(response: XccResponse, session: Session, preparedCache: Check.PreparedCache): Validation[CheckResult] = {
      val fieldName = path.stripPrefix("$.").stripPrefix(".")
      
      // Pattern 1: String values with quotes: "field":"value"
      val stringPattern = s""""$fieldName"\\s*:\\s*"([^"]*)""""".r
      // Pattern 2: Numeric/boolean values: "field":123 or "field":true  
      val numericPattern = s""""$fieldName"\\s*:\\s*([^,}\\s]+)""".r
      
      stringPattern.findFirstMatchIn(response.body) match {
        case Some(m) => Success(CheckResult(Some(m.group(1)), None))
        case None =>
          numericPattern.findFirstMatchIn(response.body) match {
            case Some(m) => Success(CheckResult(Some(m.group(1)), None))
            case None => Failure(s"JSON path '$path' not found in response. Body: ${response.body}")
          }
      }
    }
  }
  
  // Response time check
  def responseTime(maxMs: Long): XccCheck = new BaseXccCheck {
    override def check(response: XccResponse, session: Session, preparedCache: Check.PreparedCache): Validation[CheckResult] = {
      val duration = response.endTimestamp - response.startTimestamp
      if (duration <= maxMs) {
        Success(CheckResult(Some(duration), None))
      } else {
        Failure(s"Response time ${duration}ms exceeded maximum ${maxMs}ms")
      }
    }
  }
  
  // Legacy compatibility helpers
  def singleResponse: XccCheck = bodyNotEmpty
  
  def exists: XccCheck = bodyNotEmpty
}
