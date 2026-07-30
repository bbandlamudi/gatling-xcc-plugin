# Gatling XCC Plugin

A Gatling plugin for load testing MarkLogic databases using the XCC (XML Contentbase Connector) protocol with support for both standard and secure (XCCS) connections.

## Features

- ✅ Execute XQuery scripts
- ✅ Execute JavaScript scripts
- ✅ Invoke server-side modules
- ✅ Pass variables to queries
- ✅ Configure request options (timeout, locale, timezone, etc.)
- ✅ Response validation with checks
- ✅ **Secure XCCS connections with SSL/TLS encryption**
- ✅ Full integration with Gatling 3.15.0 DSL and reporting

## Requirements

- Java 17+
- Scala 2.13.17
- Gatling 3.15.0
- MarkLogic Server with XCC enabled

## Installation

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.marklogic.gatling.xcc</groupId>
    <artifactId>gatling-xcc-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Build from Source

```bash
mvn clean install
```

## Usage

### Basic XQuery Example

```scala
import io.gatling.core.Predef._
import com.marklogic.gatling.xcc.Predef._
import scala.concurrent.duration._

class BasicSimulation extends Simulation {

  val xccProtocol = xcc("xcc://localhost:8000")
    .username("admin")
    .password("admin")
    .database("Documents")

  val scn = scenario("Basic XQuery Test")
    .exec(
      xcc("Simple Query")
        .xquery("xdmp:database-name(xdmp:database())")
    )

  setUp(
    scn.inject(atOnceUsers(10))
  ).protocols(xccProtocol)
}
```

### XQuery with Variables

```scala
val scn = scenario("Query with Variables")
  .exec(
    xcc("Parameterized Query")
      .xquery("""
        declare variable $name external;
        declare variable $age external;
        fn:concat("Name: ", $name, ", Age: ", $age)
      """)
      .variable("name", "John Doe")
      .variable("age", 30)
  )
```

### JavaScript Example

```scala
val scn = scenario("JavaScript Test")
  .exec(
    xcc("JavaScript Query")
      .javascript("""
        var result = cts.doc("/example.json");
        result;
      """)
  )
```

### Module Invocation

```scala
val scn = scenario("Module Invocation")
  .exec(
    xcc("Invoke Module")
      .module("/modules/my-module.xqy")
      .variable("param1", "value1")
      .variable("param2", 123)
  )
```

### Request Options

```scala
val scn = scenario("Query with Options")
  .exec(
    xcc("Query with Timeout")
      .xquery("xdmp:sleep(1000)")
      .option("timeout", "5000")
      .option("cacheable", "true")
  )
```

### Advanced Example with Session Variables

```scala
val scn = scenario("Advanced Test")
  .exec(session => session.set("docId", java.util.UUID.randomUUID().toString))
  .exec(
    xcc("Insert Document")
      .xquery("""
        declare variable $docId external;
        xdmp:document-insert(
          fn:concat("/docs/", $docId, ".xml"),
          <doc><id>{$docId}</id></doc>
        )
      """)
              .variable("docId", "${docId}")
  )
  .pause(1.second)
  .exec(
    xcc("Retrieve Document")
      .xquery("""
        declare variable $docId external;
        fn:doc(fn:concat("/docs/", $docId, ".xml"))
      """)
              .variable("docId", "${docId}")
  )
```

## Configuration

### Protocol Configuration

```scala
val xccProtocol = xcc("xcc://marklogic-server:8000")
  .username("user")           // Authentication username
  .password("password")       // Authentication password
  .database("MyDatabase")     // Target database
  .contentBase("/content")    // Content base path
```

### Connection URI Format

#### Standard XCC (Unencrypted)
```
xcc://[username:password@]host:port[/database]
```

Examples:
- `xcc://localhost:8000`
- `xcc://admin:admin@localhost:8000`
- `xcc://admin:admin@localhost:8000/Documents`

#### Secure XCCS (SSL/TLS Encrypted)
```
xccs://[username:password@]host:port[/database]
```

Examples:
- `xccs://localhost:8443`
- `xccs://admin:admin@localhost:8443/Documents`
- `xccs://admin:admin@secure-server:8443/MyDatabase`

## Building and Testing

### Compile

```bash
mvn clean compile
```

### Run Tests

```bash
mvn test
```

### Package

```bash
mvn package
```

### Install to Local Repository

```bash
mvn install
```

## Project Structure

```
gatling-xcc-plugin/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   └── scala/
│   │       └── com/
│   │           └── marklogic/
│   │               └── gatling/
│   │                   └── xcc/
│   │                   ├── Predef.scala
│   │                   ├── action/
│   │                   │   ├── XccAction.scala
│   │                   │   └── XccActionBuilder.scala
│   │                   ├── protocol/
│   │                   │   └── XccProtocol.scala
│   │                   └── request/
│   │                       ├── XccAttributes.scala
│   │                       └── builder/
│   │                           └── XccRequestBuilder.scala
│   └── test/
│       └── scala/
│           └── com/
│               └── marklogic/
│                   └── gatling/
│                       └── xcc/
│                       └── XccPluginTest.scala
```

## Secure Connections (XCCS)

### Quick Start with XCCS

```scala
import io.gatling.core.Predef._
import com.marklogic.gatling.xcc.Predef._

class SecureSimulation extends Simulation {

  val xccsProtocol = xcc("xccs://admin:admin@localhost:8443/Documents")
    .build()

  val scn = scenario("Secure XCC Test")
    .exec(
      xcc("Secure Query")
        .xquery("xdmp:database-name(xdmp:database())")
        .check(xccBodyNotEmpty)
        .build()
    )

  setUp(scn.inject(atOnceUsers(10))).protocols(xccsProtocol)
}
```

### XCC vs XCCS Comparison

| Feature | XCC | XCCS |
|---------|-----|------|
| **Encryption** | ❌ None | ✅ SSL/TLS (TLSv1.2) |
| **Default Port** | 8000 | 8443 |
| **URI Scheme** | `xcc://` | `xccs://` |
| **Use Case** | Development, Internal | Production, External |
| **Performance** | Faster | Slight overhead |

### When to Use Each

**Use XCC** for:
- Development/testing on localhost
- Internal networks with no external access
- Performance testing where encryption overhead matters

**Use XCCS** for:
- Production environments
- Connections over untrusted networks
- Handling sensitive data
- Compliance requirements

### Security Note

⚠️ **Important**: The current XCCS implementation uses trust-all certificate validation, suitable for development/testing. For production, implement proper certificate validation according to your security policies.

## Response Validation

Validate responses using built-in checks:

```scala
xcc("Validated Query")
  .xquery("xdmp:database-name(xdmp:database())")
  .check(xccBodyNotEmpty)           // Ensure response is not empty
  .check(xccSubstring("Documents"))  // Check for substring
  .check(xccRegex("\\w+"))           // Regex pattern matching
  .check(xccBodyEquals("Documents")) // Exact match
  .build()
```

## Configuration Options

### Authentication Preemptive (XCCS)

```scala
// Enabled by default for XCCS, disable with:
val protocol = xcc("xccs://admin:admin@localhost:8443/Documents?authenticationPreemptive=false")
  .build()
```

### Request Options

Available options:
- `timeout` - Request timeout in milliseconds
- `locale` - Request locale
- `timezone` - Request timezone
- `cacheable` - Whether the query is cacheable

## Troubleshooting

### XCC Connection Issues
- Verify MarkLogic App Server is running on the specified port
- Check network/firewall allows connections to the port
- Ensure XCC Server is enabled on the App Server

### XCCS Connection Issues
- Verify MarkLogic is configured with SSL/TLS on the target port
- Check that SSL certificate is properly configured
- Ensure the port number is correct (typically 8443 for SSL)

### Common Errors
- **Connection refused**: App Server not running or wrong port
- **SSL handshake failed**: SSL not configured on MarkLogic
- **Authentication failed**: Check username/password

## Chaining Invokes: XML Response Extraction and Reuse

### Example 1: Save Entire XML Response and Reuse

```scala
val scn = scenario("XML Response Chain")
  // Step 1: Get XML response
  .exec(
    xcc("Get Order XML")
      .xquery("""
        <order>
          <orderId>ORD-12345</orderId>
          <customer>John Doe</customer>
          <total>299.99</total>
        </order>
      """)
      .check(xccSaveAs("orderXml"))  // Save entire XML to session
      .build()
  )
  // Step 2: Use saved XML in next invoke
  .exec(
    xcc("Process Order")
      .xquery("""
        declare variable $orderXml external;
        let $doc := xdmp:unquote($orderXml)
        return <processed>{$doc}</processed>
      """)
      .queryParam("orderXml", "${orderXml}")  // Use saved XML
      .check(xccBodyNotEmpty)
      .build()
  )
```

### Example 2: Extract Specific Elements via XPath

```scala
import scala.xml.XML

val scn = scenario("XPath Extract")
  // Step 1: Get XML and extract specific fields
  .exec(
    xcc("Get Customer Order")
      .xquery("""
        <order>
          <orderId>ORD-12345</orderId>
          <customerId>CUST-67890</customerId>
          <amount>299.99</amount>
        </order>
      """)
      .check(
        xccExtract(body => {
          val xml = XML.loadString(body)
          (xml \\ "orderId").text
        }, "orderId")
      )
      .check(
        xccExtract(body => {
          val xml = XML.loadString(body)
          (xml \\ "customerId").text
        }, "customerId")
      )
      .check(
        xccExtract(body => {
          val xml = XML.loadString(body)
          (xml \\ "amount").text
        }, "amount")
      )
      .build()
  )
  // Step 2: Use extracted values in next invoke
  .exec(
    xcc("Create Invoice")
      .xquery("""
        declare variable $orderId external;
        declare variable $customerId external;
        declare variable $amount external;
        
        <invoice>
          <invoiceId>{"INV-" || fn:substring-after($orderId, "ORD-")}</invoiceId>
          <orderId>{$orderId}</orderId>
          <customerId>{$customerId}</customerId>
          <amount>{$amount}</amount>
          <issueDate>{fn:current-date()}</issueDate>
        </invoice>
      """)
      .queryParam("orderId", "${orderId}")
      .queryParam("customerId", "${customerId}")
      .queryParam("amount", "${amount}")
      .check(xccBodyNotEmpty)
      .build()
  )
```

### Example 3: Complex XPath Extraction Workflow

```scala
val scn = scenario("Order Processing Workflow")
  // Get order details
  .exec(
    xcc("Get Order")
      .xquery("""
        <order>
          <orderId>ORD-99999</orderId>
          <customerId>CUST-11111</customerId>
          <items>
            <item><productId>PROD-A</productId><qty>2</qty></item>
            <item><productId>PROD-B</productId><qty>5</qty></item>
          </items>
          <total>475.50</total>
        </order>
      """)
      .check(xccSaveAs("orderXml"))  // Save full XML
      .check(
        xccExtract(body => {
          val xml = XML.loadString(body)
          (xml \\ "orderId").text
        }, "orderId")
      )
      .build()
  )
  // Validate inventory with saved XML
  .exec(
    xcc("Validate Inventory")
      .xquery("""
        declare variable $orderXml external;
        let $order := xdmp:unquote($orderXml)
        return <inventoryCheck>
          <orderId>{$order/order/orderId/text()}</orderId>
          <status>APPROVED</status>
        </inventoryCheck>
      """)
      .queryParam("orderXml", "${orderXml}")
      .check(xccSubstring("APPROVED"))
      .build()
  )
  // Update order status with extracted ID
  .exec(
    xcc("Update Status")
      .xquery("""
        declare variable $orderId external;
        <result>
          <orderId>{$orderId}</orderId>
          <newStatus>APPROVED</newStatus>
        </result>
      """)
      .queryParam("orderId", "#{orderId}")
      .check(xccBodyNotEmpty)
      .build()
  )
```

## Example Simulations

The plugin includes several example simulations in `src/test/scala/io/gatling/xcc/example/`:
- `BasicSimulation.scala` - Simple XQuery execution
- `AdvancedSimulation.scala` - CRUD operations with session variables
- `JavaScriptSimulation.scala` - JavaScript execution
- `ModuleInvocationSimulation.scala` - Module invocation
- `XccsSecureSimulation.scala` - Secure XCCS connections
- **`XmlResponseChainSimulation.scala`** - Save entire XML and reuse in subsequent invokes
- **`XPathExtractAndReuseSimulation.scala`** - Extract specific XML elements via XPath and use in chains
- **`XmlChainSimplifiedSimulation.scala`** - Simplified examples of XML chaining patterns

## Technology Stack

- **Java**: 17
- **Scala**: 2.13.17
- **Gatling**: 3.15.0
- **MarkLogic XCC**: 11.3.0
- **Build Tool**: Maven 3.6+

## Contributing

We welcome contributions! Here's how to get started:

### Development Setup

**Prerequisites:**
- Java 17 or higher
- Maven 3.6+
- Scala 2.13.17
- MarkLogic Server (for integration testing)

**Building:**
```bash
mvn clean compile
```

**Running Tests:**
```bash
mvn test
```

### Contribution Guidelines

1. **Fork** the repository
2. **Create a feature branch**: `git checkout -b feature/my-new-feature`
3. **Make your changes** with clear, atomic commits
4. **Add tests** for new functionality
5. **Update documentation** as needed
6. **Run tests**: `mvn test`
7. **Commit**: `git commit -am 'Add feature'`
8. **Push**: `git push origin feature/my-new-feature`
9. **Submit a Pull Request**

### Code Style

- Follow Scala best practices
- Use 2 spaces for indentation
- Maximum line length: 120 characters
- Add scaladoc comments for public APIs
- Use meaningful variable and method names

### Pull Request Checklist

- [ ] Tests added/updated and passing
- [ ] Documentation updated
- [ ] Commits are atomic and well-described
- [ ] Code follows project style guidelines
- [ ] No breaking changes (or clearly documented)

### Reporting Issues

When reporting issues, please include:
- Clear title and description
- Steps to reproduce
- Expected vs actual behavior
- Relevant logs and error messages
- Version information (Java, Scala, Gatling, MarkLogic)

## License

Apache License 2.0

## Support

For issues, questions, or feature requests, please open an issue on the project repository.

## Advanced Topics

### XCCS Security Implementation Details

The XCCS implementation uses:
- **SSL/TLS Protocol**: TLSv1.2
- **Certificate Validation**: Trust-all (development/testing)
- **Authentication**: Preemptive by default
- **HTTP Compliance**: Automatically enabled

#### Production Security Recommendations

For production environments:
1. **Use Valid Certificates**: Configure MarkLogic with certificates from a trusted CA
2. **Implement Custom Certificate Validation**: Extend the `SecurityOptions` to validate certificates properly
3. **Use Certificate Pinning**: Pin specific certificates for additional security
4. **Configure Proper TLS Versions**: Ensure you're using TLS 1.2 or higher

### Performance Considerations

| Metric | XCC | XCCS |
|--------|-----|------|
| Latency | Baseline | +5-15ms (SSL handshake) |
| Throughput | Higher | Slightly lower |
| CPU Usage | Lower | Higher (encryption) |
| Security | None | High |

### Migration from XCC to XCCS

**Step 1**: Test with XCC
```scala
val protocol = xcc("xcc://admin:admin@localhost:8000/Documents").build()
```

**Step 2**: Enable SSL in MarkLogic
Configure App Server with SSL certificate on port 8443

**Step 3**: Switch to XCCS
```scala
val protocol = xcc("xccs://admin:admin@localhost:8443/Documents").build()
```

**Step 4**: Verify
Run your simulations and verify successful connections

## Changelog

### Version 1.0.0-SNAPSHOT (Current)

**Features:**
- Initial release of Gatling XCC Plugin
- XQuery and JavaScript execution support
- Server-side module invocation
- Variable binding for parameterized queries
- Request options configuration
- Full integration with Gatling 3.15.0 DSL
- Secure XCCS connections with SSL/TLS encryption
- Response validation checks
- Compatible with Java 17 and Scala 2.13.17
- MarkLogic XCC 11.3.0 support

**Components:**
- `XccProtocol` - Protocol configuration for MarkLogic connections
- `XccAction` - Core action for executing XCC requests
- `XccRequestBuilder` - Fluent DSL for building XCC requests
- `XccAttributes` - Request attribute container
- `XccCheckSupport` - Response validation checks

### Roadmap

**Version 1.1.0 (Planned):**
- Connection pooling optimization
- Retry mechanism for failed requests
- Enhanced error handling and reporting
- Support for binary document operations
- Streaming result support for large datasets
- Custom result extractors and checks

**Version 1.2.0 (Planned):**
- Support for transactions
- Batch request operations
- Advanced authentication methods (Kerberos, Certificate)
- Custom SSL/TLS certificate validation
- Query console integration

**Version 2.0.0 (Future):**
- Gatling 4.x compatibility
- Reactive streaming support
- Enhanced monitoring and observability
- Cloud-native MarkLogic support

## Acknowledgments

XCCS implementation inspired by the Nuxeo Gatling MarkLogic Plugin.

---

**Status**: ✅ Ready for Testing | **Version**: 1.0.0-SNAPSHOT
