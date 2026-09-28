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
package com.marklogic.gatling.xcc.protocol

import io.gatling.core.protocol.{Protocol, ProtocolKey}
import io.gatling.core.CoreComponents
import io.gatling.core.config.GatlingConfiguration
import com.marklogic.xcc.{ContentSource, ContentSourceFactory, SecurityOptions}
import com.typesafe.scalalogging.LazyLogging
import java.net.URI
import javax.net.ssl.{SSLContext, X509TrustManager}
import java.security.cert.X509Certificate
import java.util.concurrent.{ExecutorService, Executors, ThreadFactory}
import java.util.concurrent.atomic.AtomicLong

/**
 * XCC Protocol configuration
 * 
 * @param uri The MarkLogic XCC connection URI
 * @param contentSource The XCC ContentSource (Some if cached, None if created per request)
 * @param cacheContentSource If true, reuse the ContentSource; if false, create new one per request
 */
case class XccProtocol(
  uri: String,
  contentSource: Option[ContentSource],
  cacheContentSource: Boolean
) extends Protocol with LazyLogging {
  
  /**
   * Get ContentSource - returns cached one or creates new one based on cacheContentSource flag
   */
  def getContentSource(): ContentSource = {
    contentSource.getOrElse {
      logger.trace("Creating new ContentSource for this request")
      val uriObj = new URI(uri)
      XccProtocol.createContentSource(uriObj, logger)
    }
  }
}

object XccProtocol {
  val XccProtocolKey: ProtocolKey[XccProtocol, XccComponents] = new ProtocolKey[XccProtocol, XccComponents] {
    override def protocolClass: Class[Protocol] = 
      classOf[XccProtocol].asInstanceOf[Class[Protocol]]
    
    override def defaultProtocolValue(configuration: GatlingConfiguration): XccProtocol = 
      throw new IllegalStateException("XCC protocol must be explicitly configured")
    
        override def newComponents(coreComponents: CoreComponents): XccProtocol => XccComponents = {
      // One dedicated executor per simulation run, shared across all XCC actions.
      // Keeps blocking XCC/network I/O (session creation + submitRequest) off Gatling's
      // core actor-dispatcher threads, which also service the async Netty HTTP protocols.
      val executorService: ExecutorService = Executors.newCachedThreadPool(
        new ThreadFactory() {
          val identifierGenerator = new AtomicLong()
          override def newThread(r: Runnable): Thread =
            new Thread(r, "gatling-xcc-plugin-" + identifierGenerator.getAndIncrement())
        }
      )
      coreComponents.actorSystem.registerOnTermination(() => executorService.shutdown())

      xccProtocol => XccComponents(xccProtocol, executorService)
    }
  }
  
  /**
   * Create a ContentSource from a URI
   */
  def createContentSource(uriObj: URI, logger: com.typesafe.scalalogging.Logger): ContentSource = {
    if (uriObj.getScheme.equalsIgnoreCase("xccs")) {
      logger.debug(s"Creating secure XCCS ContentSource")
      val cs = ContentSourceFactory.newContentSource(uriObj, securityOptions)
      
      // Set authentication preemptive for XCCS/basic by default
      // Can be disabled by adding authenticationPreemptive=false to the query string
      if (uriObj.getQuery == null || !uriObj.getQuery.contains("authenticationPreemptive=false")) {
        logger.trace("Setting authentication preemptive for XCCS connection")
        cs.setAuthenticationPreemptive(true)
      }
      cs
    } else {
      logger.debug(s"Creating standard XCC ContentSource")
      ContentSourceFactory.newContentSource(uriObj)
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
case class XccComponents(protocol: XccProtocol, executorService: ExecutorService) extends io.gatling.core.protocol.ProtocolComponents {
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
    
    // Check if cacheContentSource=false in query string, default to true
    val shouldCache = uriObj.getQuery == null || !uriObj.getQuery.contains("cacheContentSource=false")
    logger.debug(s"Cache ContentSource: $shouldCache")
    
    val contentSource = if (shouldCache) {
      val cs = XccProtocol.createContentSource(uriObj, logger)
      logger.info(s"Created cached ContentSource for ${sanitizeUri(connectionUri)}")
      Some(cs)
    } else {
      logger.info(s"ContentSource caching disabled for ${sanitizeUri(connectionUri)}. A new instance is created for each call.")
      None
    }
    
    XccProtocol(connectionUri, contentSource, shouldCache)
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
