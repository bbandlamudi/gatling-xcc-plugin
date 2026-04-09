package io.gatling.xcc.check

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
  endTimestamp: Long
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
