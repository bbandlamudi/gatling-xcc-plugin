package io.gatling.xcc.example

import io.gatling.core.Predef._
import io.gatling.xcc.Predef._
import scala.concurrent.duration._

/**
 * Simulation to generate sample test data in MarkLogic Documents database
 * This creates documents that can be searched by:
 * - FeederWithSessionSimulation
 * - ModuleInvocationSimulation
 */
class GenerateTestDataSimulation extends Simulation {

  val protocol = xccProtocol("xcc://admin:admin@localhost:8000/Documents").build()

  // Generate employee profile documents
  val employeeFeeder = Iterator.from(1).map { i =>
    Map(
      "docType" -> "profile",
      "docId" -> s"employee-${i}",
      "name" -> s"Employee ${i}",
      "department" -> Seq("Sales", "Engineering", "Marketing", "HR", "Finance")(i % 5),
      "content" -> s"employee profile information for staff member ${i}"
    )
  }

  // Generate customer account documents
  val customerFeeder = Iterator.from(1).map { i =>
    Map(
      "docType" -> "account",
      "docId" -> s"customer-${i}",
      "name" -> s"Customer ${i}",
      "tier" -> Seq("Gold", "Silver", "Bronze", "Platinum")(i % 4),
      "content" -> s"customer account details and transaction history ${i}"
    )
  }

  // Generate product catalog documents
  val productFeeder = Iterator.from(1).map { i =>
    Map(
      "docType" -> "catalog",
      "docId" -> s"product-${i}",
      "name" -> s"Product ${i}",
      "category" -> Seq("Electronics", "Clothing", "Food", "Books", "Toys")(i % 5),
      "content" -> s"product description and specifications ${i}"
    )
  }

  // Generate order transaction documents
  val orderFeeder = Iterator.from(1).map { i =>
    Map(
      "docType" -> "transaction",
      "docId" -> s"order-${i}",
      "name" -> s"Order ${i}",
      "status" -> Seq("Pending", "Shipped", "Delivered", "Cancelled")(i % 4),
      "content" -> s"order transaction details and shipping info ${i}"
    )
  }

  // Generate invoice billing documents
  val invoiceFeeder = Iterator.from(1).map { i =>
    Map(
      "docType" -> "billing",
      "docId" -> s"invoice-${i}",
      "name" -> s"Invoice ${i}",
      "amount" -> (1000 + i * 100),
      "content" -> s"invoice billing information and payment details ${i}"
    )
  }

  // Generate test documents for ModuleInvocationSimulation
  // These contain the word "test" to match the search query
  val testDocFeeder = Iterator.from(1).map { i =>
    Map(
      "docId" -> s"test-doc-${i}",
      "title" -> s"Test Document ${i}",
      "content" -> s"This is a test document number ${i} for testing search functionality",
      "category" -> Seq("Testing", "Quality Assurance", "Development", "UAT")(i % 4),
      "priority" -> Seq("High", "Medium", "Low")(i % 3)
    )
  }

  // Scenario to insert employee profiles
  val employeeScenario = scenario("Insert Employees")
    .feed(employeeFeeder)
    .exec(
      xcc("Insert Employee")
        .xquery("""
          declare variable $docId external;
          declare variable $name external;
          declare variable $department external;
          declare variable $content external;
          
          xdmp:document-insert(
            fn:concat("/data/employees/", $docId, ".xml"),
            <document>
              <type>profile</type>
              <id>{$docId}</id>
              <name>{$name}</name>
              <department>{$department}</department>
              <content>{$content}</content>
              <keywords>employee staff member person profile</keywords>
              <createdAt>{fn:current-dateTime()}</createdAt>
            </document>,
            xdmp:default-permissions(),
            ("employee", "profile")
          )
        """)
        .queryParam("docId", "${docId}")
        .queryParam("name", "${name}")
        .queryParam("department", "${department}")
        .queryParam("content", "${content}")
        .build()
    )
    .pause(100.milliseconds)

  // Scenario to insert customer accounts
  val customerScenario = scenario("Insert Customers")
    .feed(customerFeeder)
    .exec(
      xcc("Insert Customer")
        .xquery("""
          declare variable $docId external;
          declare variable $name external;
          declare variable $tier external;
          declare variable $content external;
          
          xdmp:document-insert(
            fn:concat("/data/customers/", $docId, ".xml"),
            <document>
              <type>account</type>
              <id>{$docId}</id>
              <name>{$name}</name>
              <tier>{$tier}</tier>
              <content>{$content}</content>
              <keywords>customer client account user</keywords>
              <createdAt>{fn:current-dateTime()}</createdAt>
            </document>,
            xdmp:default-permissions(),
            ("customer", "account")
          )
        """)
        .queryParam("docId", "${docId}")
        .queryParam("name", "${name}")
        .queryParam("tier", "${tier}")
        .queryParam("content", "${content}")
        .build()
    )
    .pause(100.milliseconds)

  // Scenario to insert products
  val productScenario = scenario("Insert Products")
    .feed(productFeeder)
    .exec(
      xcc("Insert Product")
        .xquery("""
          declare variable $docId external;
          declare variable $name external;
          declare variable $category external;
          declare variable $content external;
          
          xdmp:document-insert(
            fn:concat("/data/products/", $docId, ".xml"),
            <document>
              <type>catalog</type>
              <id>{$docId}</id>
              <name>{$name}</name>
              <category>{$category}</category>
              <content>{$content}</content>
              <keywords>product item catalog merchandise</keywords>
              <createdAt>{fn:current-dateTime()}</createdAt>
            </document>,
            xdmp:default-permissions(),
            ("product", "catalog")
          )
        """)
        .queryParam("docId", "${docId}")
        .queryParam("name", "${name}")
        .queryParam("category", "${category}")
        .queryParam("content", "${content}")
        .build()
    )
    .pause(100.milliseconds)

  // Scenario to insert orders
  val orderScenario = scenario("Insert Orders")
    .feed(orderFeeder)
    .exec(
      xcc("Insert Order")
        .xquery("""
          declare variable $docId external;
          declare variable $name external;
          declare variable $status external;
          declare variable $content external;
          
          xdmp:document-insert(
            fn:concat("/data/orders/", $docId, ".xml"),
            <document>
              <type>transaction</type>
              <id>{$docId}</id>
              <name>{$name}</name>
              <status>{$status}</status>
              <content>{$content}</content>
              <keywords>order purchase transaction sale</keywords>
              <createdAt>{fn:current-dateTime()}</createdAt>
            </document>,
            xdmp:default-permissions(),
            ("order", "transaction")
          )
        """)
        .queryParam("docId", "${docId}")
        .queryParam("name", "${name}")
        .queryParam("status", "${status}")
        .queryParam("content", "${content}")
        .build()
    )
    .pause(100.milliseconds)

  // Scenario to insert invoices
  val invoiceScenario = scenario("Insert Invoices")
    .feed(invoiceFeeder)
    .exec(
      xcc("Insert Invoice")
        .xquery("""
          declare variable $docId external;
          declare variable $name external;
          declare variable $amount external;
          declare variable $content external;
          
          xdmp:document-insert(
            fn:concat("/data/invoices/", $docId, ".xml"),
            <document>
              <type>billing</type>
              <id>{$docId}</id>
              <name>{$name}</name>
              <amount>{$amount}</amount>
              <content>{$content}</content>
              <keywords>invoice bill payment billing</keywords>
              <createdAt>{fn:current-dateTime()}</createdAt>
            </document>,
            xdmp:default-permissions(),
            ("invoice", "billing")
          )
        """)
        .queryParam("docId", "${docId}")
        .queryParam("name", "${name}")
        .queryParam("amount", "${amount}")
        .queryParam("content", "${content}")
        .build()
    )
    .pause(100.milliseconds)

  // Scenario to insert test documents in /test/ directory
  // These are specifically for ModuleInvocationSimulation
  val testDocScenario = scenario("Insert Test Documents")
    .feed(testDocFeeder)
    .exec(
      xcc("Insert Test Document")
        .xquery("""
          declare variable $docId external;
          declare variable $title external;
          declare variable $content external;
          declare variable $category external;
          declare variable $priority external;
          
          xdmp:document-insert(
            fn:concat("/test/", $docId, ".xml"),
            <document>
              <id>{$docId}</id>
              <title>{$title}</title>
              <content>{$content}</content>
              <category>{$category}</category>
              <priority>{$priority}</priority>
              <status>active</status>
              <keywords>test testing quality assurance qa validation</keywords>
              <createdAt>{fn:current-dateTime()}</createdAt>
            </document>,
            xdmp:default-permissions(),
            ("test", "qa")
          )
        """)
        .queryParam("docId", "${docId}")
        .queryParam("title", "${title}")
        .queryParam("content", "${content}")
        .queryParam("category", "${category}")
        .queryParam("priority", "${priority}")
        .build()
    )
    .pause(100.milliseconds)

  // Setup: Insert 20 documents of each type
  // Total: 120 documents
  setUp(
    employeeScenario.inject(atOnceUsers(20)),
    customerScenario.inject(atOnceUsers(20)),
    productScenario.inject(atOnceUsers(20)),
    orderScenario.inject(atOnceUsers(20)),
    invoiceScenario.inject(atOnceUsers(20)),
    testDocScenario.inject(atOnceUsers(20))
  ).protocols(protocol)
    .assertions(
      global.successfulRequests.percent.is(100)
    )
}