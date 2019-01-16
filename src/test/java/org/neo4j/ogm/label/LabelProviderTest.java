/*
 * Copyright (c) 2018 "Neo4j, Inc." <https://neo4j.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.ogm.label;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * This test lives in another package to simulate external usage
 * if the {@link LabelProvider} extension.
 *
 * @author Gerrit Meier
 */
class LabelProviderTest {

  private final LabelProvider support = LabelProvider.forLabel("NewLabel");

  @Nested
  @DisplayName("Simple queries")
  class SimpleQueries {
	@Test
	@DisplayName("that match all nodes")
	void queryThatMatchesAllNodes() {
	  String withLabel = support.addLabel("MATCH (n) RETURN n");

	  String expected = "MATCH (n:NewLabel) RETURN n";

	  assertEquals(expected, withLabel);
	}

	@Test
	@DisplayName("that match one label")
	void queryWithLabel() {
	  String withLabel = support.addLabel("MATCH (n:Existing) RETURN n");

	  String expected = "MATCH (n:Existing:NewLabel) RETURN n";

	  assertEquals(expected, withLabel);
	}

	@Test
	@DisplayName("that match nodes based on properties")
	void queryWithPropertyFilter() {
	  String withLabel = support.addLabel("MATCH (n{name:'Someone'}) RETURN n");

	  String expected = "MATCH (n:NewLabel {name: \"Someone\"}) RETURN n";

	  assertEquals(expected, withLabel);
	}
  }

  @Nested
  @DisplayName("Queries that call procedures")
  class QueriesWithProcedureCalls {

	@Test
	@DisplayName("without arguments")
	void queryWithProcedureCallNoArgument() {
	  String withLabel = support.addLabel("MATCH (n{name:'Someone'}) CALL nsp.customProcedure()");

	  String expected = "MATCH (n:NewLabel {name: \"Someone\"}) CALL nsp.customProcedure()";

	  assertEquals(expected, withLabel);
	}

	@Test
	@DisplayName("with one argument")
	void queryWithProcedureCallOneArgument() {
	  String withLabel = support.addLabel("MATCH (n{name:'Someone'}) CALL nsp.customProcedure(n)");

	  String expected = "MATCH (n:NewLabel {name: \"Someone\"}) CALL nsp.customProcedure(n)";

	  assertEquals(expected, withLabel);
	}

	@Test
	@DisplayName("with multiple arguments")
	void queryWithProcedureCallTwoArgumentsAndYield() {
	  String withLabel = support.addLabel(
		  "MATCH (c:IdEntity:ConfigEntity {objectState:'COMMITTED'}) WHERE (c)-[:PREDECESSOR]->({objectState:'DEPLOYING'}) CALL nsp.customProcedure(c, 1) YIELD nodes, rels RETURN c, nodes, rels");

	  String expected = "MATCH (c:IdEntity:ConfigEntity:NewLabel {objectState: \"COMMITTED\"}) WHERE (c)-[:PREDECESSOR]->(:NewLabel {objectState: \"DEPLOYING\"}) CALL nsp.customProcedure(c, 1) YIELD nodes, rels RETURN c, nodes, rels";

	  assertEquals(expected, withLabel);
	}
  }

  @Nested
  @DisplayName("Write queries")
  class WriteQueries {
	@Test
	void settingProperties() {
	  String withLabel = support.addLabel(
		  "MATCH (n:IdEntity:ConfigEntity {objectState:'DEPLOYING'}) WHERE NOT((n)<-[:PREDECESSOR]-({objectState:'COMMITTED'})) SET n.objectState='COMMITTED' RETURN COUNT(n)");

	  String expected = "MATCH (n:IdEntity:ConfigEntity:NewLabel {objectState: \"DEPLOYING\"}) WHERE not (n)<-[:PREDECESSOR]-(:NewLabel {objectState: \"COMMITTED\"}) SET n.objectState = \"COMMITTED\" RETURN COUNT(n)";

	  assertEquals(expected, withLabel);
	}

	@Test
	void settingMultipleProperties() {
	  String withLabel = support.addLabel(
		  "MATCH (n:IdEntity:ConfigEntity {objectState:'DEPLOYING'}) WHERE NOT((n)<-[:PREDECESSOR]-({objectState:'COMMITTED'})) SET n.objectState='COMMITTED', n.objectState2='COMMITTED' RETURN COUNT(n)");

	  String expected = "MATCH (n:IdEntity:ConfigEntity:NewLabel {objectState: \"DEPLOYING\"}) WHERE not (n)<-[:PREDECESSOR]-(:NewLabel {objectState: \"COMMITTED\"}) SET n.objectState = \"COMMITTED\", n.objectState2 = \"COMMITTED\" RETURN COUNT(n)";

	  assertEquals(expected, withLabel);
	}

	@Test
	void settingLabels() {
	  String withLabel = support.addLabel(
		  "UNWIND {rows} as row MATCH (n) WHERE ID(n)=row.nodeId SET n:`DataDNSSettings`:`ConfigEntity`:`IdEntity`:`Entity` SET n += row.props RETURN row.nodeId as ref, ID(n) as id, row.type as type");

	  String expected = "UNWIND $rows AS row MATCH (n:NewLabel) WHERE ID(n) = row.nodeId SET n:DataDNSSettings:ConfigEntity:IdEntity:Entity:NewLabel SET n += row.props RETURN row.nodeId AS ref, ID(n) AS id, row.type AS type";

	  assertEquals(expected, withLabel);
	}

	@Test
	void performingSimpleMerge() {
	  String withLabel = support.addLabel("MERGE (a:Foobar)");

	  String expected = "MERGE (a:Foobar:NewLabel)";

	  assertEquals(expected, withLabel);
	}

	@Test
	void performingMerge() {
	  String withLabel = support.addLabel(
		  "UNWIND {rows} as row MATCH (startNode) WHERE ID(startNode) = row.startNodeId MATCH (endNode) WHERE ID(endNode) = row.endNodeId MERGE (startNode)-[rel:`UNDECRYPTABLE_ACTIONS`]->(endNode) RETURN row.relRef as ref, ID(rel) as id, row.type as type");

	  String expected = "UNWIND $rows AS row MATCH (startNode:NewLabel) WHERE ID(startNode) = row.startNodeId MATCH (endNode:NewLabel) WHERE ID(endNode) = row.endNodeId MERGE (startNode)-[rel:UNDECRYPTABLE_ACTIONS]->(endNode) RETURN row.relRef AS ref, ID(rel) AS id, row.type AS type";

	  assertEquals(expected, withLabel);
	}
  }

}
