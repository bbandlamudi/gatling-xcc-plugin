package io.gatling.xcc.request

import io.gatling.core.session.Expression
import io.gatling.core.check.Check
import io.gatling.xcc.check.XccResponse
import com.marklogic.xcc.ResultSequence

case class XccAttributes(
  requestName: String,
  xquery: Option[Expression[String]],
  javascript: Option[Expression[String]],
  module: Option[Expression[String]],
  variables: Map[String, Expression[Any]],
  options: Map[String, Expression[String]],
  checks: List[Check[ResultSequence]] = Nil,
  xccChecks: List[Check[XccResponse]] = Nil,
  resultMapper: Option[ResultSequence => Any] = None
)
  
