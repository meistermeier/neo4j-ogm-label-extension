package org.neo4j.ogm.staticlabel.test;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.CoreMatchers.*;
import static org.neo4j.ogm.staticlabel.StaticLabelModificationProvider.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Date;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.neo4j.driver.internal.value.NodeValue;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.configuration.BoltConnector;
import org.neo4j.kernel.configuration.Connector;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.staticlabel.test.domain.TestEntity;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StaticLabelIntegrationTest {
  private static final String BOLT_URL = "localhost:34567";
  private static SessionFactory sessionFactory;
  private Driver driver;

  @AfterAll
  static void tearDown() {
	sessionFactory.close();
  }

  @BeforeAll
  void initializeDatabase() throws IOException {
	BoltConnector bolt = new BoltConnector();
	File neo4jDb = Files.createTempDirectory("neo4j.db").toFile();

	new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(neo4jDb)
		.setConfig(bolt.type, Connector.ConnectorType.BOLT.name()).setConfig(bolt.enabled, "true")
		.setConfig(bolt.listen_address, BOLT_URL).newGraphDatabase();

	sessionFactory = new SessionFactory(
		new Configuration.Builder()
			.uri("bolt://localhost:34567")
			.withConfigProperty(CONFIGURATION_KEY, "StaticLabel")
			.build(),
		"org.neo4j.ogm.staticlabel.test.domain");

	driver = GraphDatabase.driver("bolt://" + BOLT_URL);
  }

  @AfterEach
  void cleanDb() {
	driver.session().run("MATCH (n) detach delete n");
  }

  @Test
  void loadWithLabel() {
	org.neo4j.driver.v1.Session driverSession = driver.session();

	driverSession.run("CREATE (n:TestEntity:StaticLabel{name:'Labeled'})");
	driverSession.run("CREATE (n:TestEntity{name:'NotLabeled'})");

	Session session = sessionFactory.openSession();

	TestEntity entity = new TestEntity();
	entity.setName("TestName");
	session.save(entity);

	session.clear();

	Collection<TestEntity> entities = session.loadAll(TestEntity.class);
	Assert.assertThat(entities.size(), equalTo(2));

  }

  @Test
  void saveWithLabel() {
	Session session = sessionFactory.openSession();

	TestEntity entity = new TestEntity();
	entity.setName("TestName");
	session.save(entity);

	assertLabels("TestEntity", "StaticLabel");
  }

  @Test
  void saveWithLabelSupplier() {

	SessionFactory sessionFactory = new SessionFactory(
		new Configuration.Builder()
			.uri("bolt://localhost:34567")
			.withConfigProperty(CONFIGURATION_KEY, (Supplier) () -> "StaticLabel")
			.build(),
		"org.neo4j.ogm.staticlabel.test.domain");

	Session session = sessionFactory.openSession();

	TestEntity entity = new TestEntity();
	entity.setName("TestName");
	session.save(entity);

	assertLabels("TestEntity", "StaticLabel");
  }

  @Test
  void doesNothingIfLabelIsNotSet() {
	SessionFactory sessionFactory = new SessionFactory(
		new Configuration.Builder()
			.uri("bolt://localhost:34567")
			.credentials("neo4j", "secret")
			.build(),
		"org.neo4j.ogm.staticlabel.test.domain");

	Session session = sessionFactory.openSession();
	session.save(new TestEntity());

	assertLabels("TestEntity");
  }

  @Test
  void shouldThrowExceptionIfLabelTypeDoesNotMatch() {
	Assertions.assertThrows(IllegalArgumentException.class, () ->
		new SessionFactory(
			new Configuration.Builder()
				.uri("bolt://localhost:34567")
				.credentials("neo4j", "secret")
				.withConfigProperty(CONFIGURATION_KEY, new Date())
				.build(),
			"org.neo4j.ogm.staticlabel.test.domain"));
  }

  private void assertLabels(String... expectedLabels) {
	// assert from a "raw" driver point of view
	StatementResult result = driver.session().run("MATCH (n) return n");
	for (Record record : result.list()) {
	  for (Value value : record.values()) {
		if (value instanceof NodeValue) {
		  Node node = value.asNode();
		  assertThat(node.labels()).containsExactlyInAnyOrder(expectedLabels);
		}
	  }
	}
  }
}
