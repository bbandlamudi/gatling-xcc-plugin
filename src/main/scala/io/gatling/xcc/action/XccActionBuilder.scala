package io.gatling.xcc.action

import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.structure.ScenarioContext
import io.gatling.xcc.request.XccAttributes
import io.gatling.xcc.protocol.{XccComponents, XccProtocol}

/**
 * Builder for XCC actions
 */
class XccActionBuilder(attributes: XccAttributes) extends ActionBuilder {
  
  override def build(ctx: ScenarioContext, next: Action): Action = {
    val xccComponents = ctx.protocolComponentsRegistry.components(XccProtocol.XccProtocolKey)
    new XccAction(attributes, xccComponents, ctx, next)
  }
}
