package com.marklogic.gatling.xcc.protocol

import io.gatling.core.protocol.{Protocol, ProtocolKey}
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import com.marklogic.xcc.{ContentSource, ContentSourceFactory, SecurityOptions}
import com.typesafe.scalalogging.LazyLogging
import java.net.URI
import javax.net.ssl.{SSLContext, X509TrustManager}
import java.security.cert.X509Certificate

/**
 * XCC Protocol configuration
 * 
 * @param uri The MarkLogic XCC connection URI
 * @param contentSource The XCC ContentSource
 */
case class XccProtocol(
  uri: String,
  contentSource: ContentSource
) extends Protocol

object XccProtocol {
  val XccProtocolKey: ProtocolKey[XccProtocol, XccComponents] = new ProtocolKey[XccProtocol, XccComponents] {
    override def protocolClass: Class[Protocol] = 
      classOf[XccProtocol].asInstanceOf[Class[Protocol]]
    
    override def defaultProtocolValue(configuration: GatlingConfiguration): XccProtocol = 
      throw new IllegalStateException("XCC protocol must be explicitly configured")
    
    override def newComponents(coreComponents: CoreComponents): XccProtocol => XccComponents = {
      xccProtocol => XccComponents(xccProtocol)
    }
  }
  
  /**
   * SecurityOptions for XCCS (secure XCC) connections
   * Creates a trust-all SSL context for TLSv1.2
   */
  val securityOptions: SecurityOptions = {
    val sslContext = SSLContext.getInstance("TLSv1.2")
    val trustAllCerts = new X509TrustManager() {
      def getAcceptedIssuers(): Array[X509Certificate] = new Array[X509Certificate](0)
      def checkClientTrusted(certs: Array[X509Certificate], authType: String): Unit = ()
      def checkServerTrusted(certs: Array[X509Certificate], authType: String): Unit = ()
    }
    sslContext.init(null, Array(trustAllCerts), null)
    new SecurityOptions(sslContext)
  }
}

/**
 * Components holder for XCC protocol
 */
case class XccComponents(protocol: XccProtocol) extends io.gatling.core.protocol.ProtocolComponents {
  override def onStart: io.gatling.core.session.Session => io.gatling.core.session.Session = identity
  override def onExit: io.gatling.core.session.Session => Unit = _ => ()
}

/**
 * Builder for XCC Protocol
 */
case class XccProtocolBuilder(
  uri: String,
  username: Option[String] = None,
  password: Option[String] = None,
  database: Option[String] = None,
  contentBase: Option[String] = None
) extends LazyLogging {
  
  /**
   * Set username for authentication
   */
  def username(username: String): XccProtocolBuilder = copy(username = Some(username))
  
  /**
   * Set password for authentication
   */
  def password(password: String): XccProtocolBuilder = copy(password = Some(password))
  
  /**
   * Set target database
   */
  def database(database: String): XccProtocolBuilder = copy(database = Some(database))
  
  /**
   * Set content base
   */
  def contentBase(contentBase: String): XccProtocolBuilder = copy(contentBase = Some(contentBase))
  
  /**
   * Build the XCC protocol
   * Automatically detects if URI contains credentials (full URI mode)
   * or requires building from individual components.
   * Supports both XCC and XCCS (secure) protocols.
   */
  def build(): XccProtocol = {
    // Set system property for HTTP compliance
    System.setProperty("xcc.httpcompliant", "true")
    
    val connectionUri = if (isFullUri) uri else buildConnectionUri()
    logger.debug(s"Building XCC protocol with URI: ${sanitizeUri(connectionUri)}")
    
    val uriObj = new URI(connectionUri)
    val contentSource = if (uriObj.getScheme.equalsIgnoreCase("xccs")) {
      logger.info(s"Creating secure XCCS ContentSource for ${sanitizeUri(connectionUri)}")
      val cs = ContentSourceFactory.newContentSource(uriObj, XccProtocol.securityOptions)
      
      // Set authentication preemptive for XCCS/basic by default
      // Can be disabled by adding authenticationPreemptive=false to the query string
      if (uriObj.getQuery == null || !uriObj.getQuery.contains("authenticationPreemptive=false")) {
        logger.debug("Setting authentication preemptive for XCCS connection")
        cs.setAuthenticationPreemptive(true)
      }
      cs
    } else {
      logger.debug(s"Creating standard XCC ContentSource for ${sanitizeUri(connectionUri)}")
      ContentSourceFactory.newContentSource(uriObj)
    }
    
    logger.info(s"Successfully created ContentSource for ${sanitizeUri(connectionUri)}")
    XccProtocol(connectionUri, contentSource)
  }
  
  /**
   * Check if the URI is a full URI with embedded credentials
   */
  private def isFullUri: Boolean = {
    // Check if URI contains userInfo (username:password@) and no builder parameters were set
    val uriObj = new URI(uri)
    uriObj.getUserInfo != null && username.isEmpty && password.isEmpty
  }
  
  private def sanitizeUri(uri: String): String = {
    // Hide password in logs
    uri.replaceAll(":[^:@]+@", ":****@")
  }
  
  private def buildConnectionUri(): String = {
    val baseUri = uri
    
    // Parse the URI and add authentication if needed
    val uriObj = new URI(baseUri)
    val scheme = if (uriObj.getScheme != null) uriObj.getScheme else "xcc"
    val host = if (uriObj.getHost != null) uriObj.getHost else "localhost"
    val port = if (uriObj.getPort != -1) uriObj.getPort else 8000
    
    val auth = (username, password) match {
      case (Some(u), Some(p)) => 
        logger.debug(s"Using authentication with username: $u")
        s"$u:$p@"
      case _ => 
        logger.debug("No authentication configured")
        ""
    }
    
    val dbPath = database.map(db => s"/$db").getOrElse("")
    database.foreach(db => logger.debug(s"Target database: $db"))
    
    s"$scheme://$auth$host:$port$dbPath"
  }
}
