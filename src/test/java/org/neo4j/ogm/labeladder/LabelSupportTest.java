package org.neo4j.ogm.labeladder;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LabelSupportTest {

  private LabelSupport support = LabelSupport.forLabel("NewLabel");

  @Test
  void simpleMatchQuery() {
	String withLabel = support.withLabel("MATCH (n) RETURN n");

	String expected = "MATCH (n:NewLabel) RETURN n";

	assertEquals(expected, withLabel);
  }

  @Test
  void simpleMatchQueryWithLabel() {
	String withLabel = support.withLabel("MATCH (n:Existing) RETURN n");

	String expected = "MATCH (n:Existing:NewLabel) RETURN n";

	assertEquals(expected, withLabel);
  }

  @Test
  void simpleMatchQueryWithPropertyFilter() {
	String withLabel = support.withLabel("MATCH (n{name:'Someone'}) RETURN n");

	String expected = "MATCH (n:NewLabel{name: \"Someone\"}) RETURN n";

	assertEquals(expected, withLabel);
  }

  @Test
  void matchQueryWithProcedureCallNoArgument() {
	String withLabel = support.withLabel("MATCH (n{name:'Someone'}) CALL nsp.customProcedure()");

	String expected = "MATCH (n:NewLabel{name: \"Someone\"}) CALL nsp.customProcedure()";

	assertEquals(expected, withLabel);
  }

  @Test
  void matchQueryWithProcedureCallOneArgument() {
	String withLabel = support.withLabel("MATCH (n{name:'Someone'}) CALL nsp.customProcedure(n)");

	String expected = "MATCH (n:NewLabel{name: \"Someone\"}) CALL nsp.customProcedure(n)";

	assertEquals(expected, withLabel);
  }

  @Test
  void matchQueryWithProcedureCallTwoArgumentsAndYield() {
	String withLabel = support.withLabel(
		"MATCH (c:IdEntity:ConfigEntity {objectState:'COMMITTED'}) WHERE (c)-[:PREDECESSOR]->({objectState:'DEPLOYING'}) CALL cisco.entity.populate(c, 1) YIELD nodes, rels RETURN c, nodes, rels");

	String expected = "MATCH (c:IdEntity:ConfigEntity:NewLabel{objectState: \"COMMITTED\"}) WHERE (c:NewLabel)-[:PREDECESSOR]->(:NewLabel{objectState: \"DEPLOYING\"}) CALL cisco.entity.populate(c, 1) YIELD nodes, rels RETURN c, nodes, rels";

	assertEquals(expected, withLabel);
  }

  @Test
  void matchQueryWithSet() {
	String withLabel = support.withLabel(
		"MATCH (n:IdEntity:ConfigEntity {objectState:'DEPLOYING'}) WHERE NOT((n)<-[:PREDECESSOR]-({objectState:'COMMITTED'})) SET n.objectState='COMMITTED' RETURN COUNT(n)");

	String expected = "MATCH (n:IdEntity:ConfigEntity:NewLabel{objectState: \"DEPLOYING\"}) WHERE not (n:NewLabel)<-[:PREDECESSOR]-(:NewLabel{objectState: \"COMMITTED\"}) SET n.objectState=\"COMMITTED\" RETURN COUNT(n)";

	assertEquals(expected, withLabel);
  }

  @Test
  void matchQueryWithLabelSet() {
	String withLabel = support.withLabel(
		"UNWIND {rows} as row MATCH (n) WHERE ID(n)=row.nodeId SET n:`DataDNSSettings`:`ConfigEntity`:`IdEntity`:`Entity` SET n += row.props RETURN row.nodeId as ref, ID(n) as id, row.type as type");

	String expected = "UNWIND $rows AS row MATCH (n:NewLabel) WHERE ID(n) = row.nodeId SET n:`DataDNSSettings`:`ConfigEntity`:`IdEntity`:`Entity`:`NewLabel` SET n += row.props RETURN row.nodeId AS ref, ID(n) AS id, row.type AS type";

	assertEquals(expected, withLabel);
  }

  @Test
  void queryWithMerge() {
	String withLabel = support.withLabel(
		"UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MERGE (startNode)-[rel:`UNDECRYPTABLE_ACTIONS`]->(endNode) RETURN row.relRef as ref, ID(rel) as id, row.type as type");

	String expected = "UNWIND $rows AS row MATCH (startNode:NewLabel) WHERE ID(startNode) = row.startNodeId MATCH (endNode:NewLabel) WHERE ID(endNode) = row.endNodeId MERGE (startNode)-[rel:UNDECRYPTABLE_ACTIONS]->(endNode) RETURN row.relRef AS ref, ID(rel) AS id, row.type AS type";

	assertEquals(expected, withLabel);
  }

}
