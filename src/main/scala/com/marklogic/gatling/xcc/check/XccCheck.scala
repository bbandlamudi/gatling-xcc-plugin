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
package com.marklogic.gatling.xcc.check

import io.gatling.commons.validation.Validation
import io.gatling.core.check.{Check, CheckResult}
import io.gatling.core.session.{Expression, Session}

/**
 * Response wrapper for XCC requests
 */
case class XccResponse(
  body: String,
  requestName: String,
  startTimestamp: Long,
  endTimestamp: Long,
  firstItem: Option[String] = None,  // First item from ResultSequence
  items: List[String] = List.empty    // All items from ResultSequence
)

/**
 * XCC-specific Check trait
 */
trait XccCheck extends Check[XccResponse] {
  // Check interface requires implementing:
  // - check(response: XccResponse, session: Session, preparedCache: PreparedCache): Validation[CheckResult]
  // - checkIf(condition: Expression[Boolean]): Check[XccResponse]
  // - checkIf(condition: String): Check[XccResponse]
}
