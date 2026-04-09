package io.gatling.xcc.action

import io.gatling.commons.stats.{KO, OK}
import io.gatling.commons.util.Clock
import io.gatling.commons.validation.{Failure, Success, Validation}
import io.gatling.core.action.{Action, RequestAction}
import io.gatling.core.session.{Expression, Session}
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import io.gatling.xcc.protocol.XccComponents
import io.gatling.xcc.request.XccAttributes
import io.gatling.xcc.check.XccResponse
import com.marklogic.xcc.{Request, RequestOptions, ResultSequence, Session => XccSession}
import com.marklogic.xcc.types.{XdmValue, XdmVariable, XName}

import scala.util.{Try, Success => TrySuccess, Failure => TryFailure}

class XccAction(
  attributes: XccAttributes,
  xccComponents: XccComponents,
  ctx: ScenarioContext,
  val next: Action
) extends RequestAction {

  override val name: String = attributes.requestName
  override val statsEngine: StatsEngine = ctx.coreComponents.statsEngine
  override val clock: Clock = ctx.coreComponents.clock

  override def requestName: Expression[String] = _ => Success(attributes.requestName)

  override def sendRequest(session: Session): Validation[Unit] = {
    logger.info(s"Executing XCC request: ${attributes.requestName}")
    val startTime = clock.nowMillis
    
    val result = for {
      request <- buildRequest(session)
      resultSequence <- executeRequest(request)
    } yield resultSequence
    
    result match {
      case Success(resultSequence) =>
        val endTime = clock.nowMillis
        val duration = endTime - startTime
        val responseStr = resultSequenceToString(resultSequence)
        
        // Apply result mapper if provided
        val mappedResult = attributes.resultMapper match {
          case Some(mapper) => 
            Try(mapper(resultSequence)) match {
              case TrySuccess(mapped) => mapped
              case TryFailure(ex) => 
                logger.error(s"Result mapping failed: ${ex.getMessage}")
                resultSequence
            }
          case None => resultSequence
        }
        
        // Apply legacy ResultSequence checks if provided
        val legacyCheckResult = if (attributes.checks.nonEmpty) {
          io.gatling.core.check.Check.check(resultSequence, session, attributes.checks, null)
        } else {
          (session, None)
        }
        
        val (sessionAfterLegacyChecks, legacyCheckError) = legacyCheckResult
        
        // Apply XccResponse checks if provided
        val xccResponse = XccResponse(
          body = responseStr,
          requestName = attributes.requestName,
          startTimestamp = startTime,
          endTimestamp = endTime
        )
        
        val xccCheckResult = if (attributes.xccChecks.nonEmpty) {
          io.gatling.core.check.Check.check(xccResponse, sessionAfterLegacyChecks, attributes.xccChecks, null)
        } else {
          (sessionAfterLegacyChecks, None)
        }
        
        val (finalSession, xccCheckError) = xccCheckResult
        val checkError = legacyCheckError.orElse(xccCheckError)
        
        checkError match {
          case Some(error) =>
            logger.warn(s"Check failed for '${attributes.requestName}': $error")
            statsEngine.logResponse(
              session.scenario,
              session.groups,
              attributes.requestName,
              startTime,
              endTime,
              KO,
              None,
              Some(error.message)
            )
            next ! finalSession.markAsFailed
            Failure(error.message)
            
          case None =>
            logger.debug(s"Request '${attributes.requestName}' succeeded in ${duration}ms")
            logger.trace(s"Response: $responseStr")
            statsEngine.logResponse(
              session.scenario,
              session.groups,
              attributes.requestName,
              startTime,
              endTime,
              OK,
              None,
              Some(responseStr)
            )
            next ! finalSession.markAsSucceeded
            Success(())
        }
        
      case Failure(errorMessage) =>
        val endTime = clock.nowMillis
        val duration = endTime - startTime
        logger.warn(s"Request '${attributes.requestName}' failed after ${duration}ms: $errorMessage")
        statsEngine.logResponse(
          session.scenario,
          session.groups,
          attributes.requestName,
          startTime,
          endTime,
          KO,
          None,
          Some(errorMessage)
        )
        next ! session.markAsFailed
        Failure(errorMessage)
    }
  }

  private def buildRequest(session: Session): Validation[Request] = {
    logger.debug(s"Building XCC request: ${attributes.requestName}")
    Try {
      val xccSession: XccSession = xccComponents.protocol.contentSource.newSession()
      
      val request: Request = (attributes.xquery, attributes.javascript, attributes.module) match {
        case (Some(xqueryExpr), None, None) =>
          xqueryExpr(session) match {
            case Success(query) => 
              logger.debug(s"Creating ad-hoc XQuery request")
              xccSession.newAdhocQuery(query)
            case Failure(error) => throw new IllegalArgumentException(s"Failed to resolve XQuery: $error")
          }
          
        case (None, Some(jsExpr), None) =>
          jsExpr(session) match {
            case Success(js) => 
              logger.debug(s"Creating ad-hoc JavaScript request")
              val req = xccSession.newAdhocQuery(js)
              val options = new RequestOptions()
              options.setQueryLanguage("javascript")
              req.setOptions(options)
              req
            case Failure(error) => throw new IllegalArgumentException(s"Failed to resolve JavaScript: $error")
          }
          
        case (None, None, Some(moduleExpr)) =>
          moduleExpr(session) match {
            case Success(modulePath) => 
              logger.debug(s"Creating module invocation request: $modulePath")
              xccSession.newModuleInvoke(modulePath)
            case Failure(error) => throw new IllegalArgumentException(s"Failed to resolve module: $error")
          }
          
        case _ =>
          throw new IllegalArgumentException("Must specify exactly one of: xquery, javascript, or module")
      }
      
      attributes.variables.foreach { case (name, valueExpr) =>
        valueExpr(session) match {
          case Success(value) =>
            logger.trace(s"Setting variable: $name = $value")
            val xdmVariable = createXdmVariable(name, value)
            request.setVariable(xdmVariable)
          case Failure(error) =>
            throw new IllegalArgumentException(s"Failed to resolve variable $name: $error")
        }
      }
      
      val options = request.getEffectiveOptions
      attributes.options.foreach { case (name, valueExpr) =>
        valueExpr(session) match {
          case Success(value) =>
            applyOption(options, name, value)
          case Failure(error) =>
            throw new IllegalArgumentException(s"Failed to resolve option $name: $error")
        }
      }
      
      request
    } match {
      case TrySuccess(req) => 
        logger.debug(s"Successfully built request for '${attributes.requestName}'")
        Success(req)
      case TryFailure(ex) => 
        logger.error(s"Failed to build request: ${ex.getMessage}", ex)
        Failure(s"Failed to build request: ${ex.getMessage}")
    }
  }

  private def executeRequest(request: Request): Validation[ResultSequence] = {
    logger.debug(s"Executing request to MarkLogic")
    Try {
      val resultSequence: ResultSequence = request.getSession.submitRequest(request)
      val itemCount = resultSequence.size()
      logger.debug(s"Received $itemCount result items")
      resultSequence
    } match {
      case TrySuccess(result) => 
        logger.debug(s"Request executed successfully")
        Success(result)
      case TryFailure(ex) => 
        logger.error(s"Request execution failed: ${ex.getMessage}", ex)
        Failure(s"Request failed: ${ex.getMessage}")
    }
  }
  
  private def resultSequenceToString(resultSequence: ResultSequence): String = {
    Try {
      val results = new StringBuilder
      while (resultSequence.hasNext) {
        val item = resultSequence.next()
        results.append(item.asString()).append("\n")
      }
      results.toString()
    } match {
      case TrySuccess(str) => str
      case TryFailure(_) => "[Unable to convert result to string]"
    }
  }

  private def createXdmVariable(name: String, value: Any): XdmVariable = {
    val xdmValue: XdmValue = value match {
      case s: String => com.marklogic.xcc.ValueFactory.newXSString(s)
      case i: Int => com.marklogic.xcc.ValueFactory.newXSInteger(i)
      case l: Long => com.marklogic.xcc.ValueFactory.newXSInteger(l)
      case d: Double => com.marklogic.xcc.ValueFactory.newXSString(d.toString)
      case f: Float => com.marklogic.xcc.ValueFactory.newXSString(f.toString)
      case b: Boolean => com.marklogic.xcc.ValueFactory.newXSBoolean(b)
      case _ => com.marklogic.xcc.ValueFactory.newXSString(value.toString)
    }
    
    com.marklogic.xcc.ValueFactory.newVariable(new XName(name), xdmValue)
  }

  private def applyOption(options: RequestOptions, name: String, value: String): Unit = {
    name.toLowerCase match {
      case "timeout" => options.setTimeoutMillis(value.toInt)
      case "locale" => options.setLocale(new java.util.Locale(value))
      case "timezone" => options.setTimeZone(java.util.TimeZone.getTimeZone(value))
      case "cachable" | "cacheable" => options.setCacheResult(value.toBoolean)
      case _ => // Ignore unknown options
    }
  }
}
