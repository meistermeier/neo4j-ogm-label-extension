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
package org.neo4j.ogm.staticlabel;

import static scala.collection.JavaConverters.*;

import scala.Function1;
import scala.Option;
import scala.collection.Seq;
import scala.compat.java8.JFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.cypher.internal.frontend.v3_4.ast.Statement;
import org.neo4j.cypher.internal.frontend.v3_4.parser.CypherParser;
import org.neo4j.cypher.internal.frontend.v3_4.prettifier.ExpressionStringifier;
import org.neo4j.cypher.internal.util.v3_4.bottomUp;
import org.neo4j.cypher.internal.v3_4.expressions.Expression;
import org.neo4j.cypher.internal.v3_4.expressions.LabelName;
import org.neo4j.cypher.internal.v3_4.expressions.NodePattern;

/**
 * Support for taking a Cypher query and adding a static label
 * to every node in the query.
 *
 * @author Gerrit Meier
 */
class StaticLabel {

  private static final String ILLEGAL_LABEL_MESSAGE =
	  "Only labels with alpha-numeric characters are allowed, starting with an alphabetic character. "
		  + "This label does not match the rule: ";

  private final Supplier<String> labelSupplier;

  private StaticLabel(Supplier<String> labelSupplier) {
	this.labelSupplier = labelSupplier;
  }

  /**
   * Creates a {@link StaticLabel} instance with the given
   * static label to use on each query processed with it.
   *
   * @param label Label to add to the queries nodes.
   * @return {@link StaticLabel} instance
   */
   static StaticLabel forLabel(String label) {
	return new StaticLabel(() -> label);
  }

  /**
   * Creates a {@link StaticLabel} instance with the given
   * supplier function to use on each query processed with it.
   *
   * @param labelSupplier Supplier function to get called before each cypher modification.
   * @return {@link StaticLabel} instance
   */
  static StaticLabel forLabel(Supplier<String> labelSupplier) {
	return new StaticLabel(labelSupplier);
  }

  private static boolean hasIllegalForm(String label) {
	return !(
		// check the first character first to be alphabetic
		StringUtils.isAlphanumeric(label)
			// finds null, empty, only blanks, blanks within the string and special characters
			|| StringUtils.isAlpha(label.substring(0, 1))
	);
  }

  /**
   * Parses a given Cypher query, adds the static label
   * and rewrites it.
   *
   * @param cypher Cypher query to get manipulated.
   * @return Cypher query with static label added.
   */
  String addLabel(String cypher) {
	String label = getLabel();
	if (hasIllegalForm(label)) {
	  throw new IllegalArgumentException(ILLEGAL_LABEL_MESSAGE + label);
	}

	CypherParser cypherParser = new CypherParser();
	Statement statement = cypherParser.parse(cypher, Option.empty());

	Function1<Object, Object> rewriter = bottomUp.apply(new AddLabelRewriter(label), JFunction.func((o) -> false));

	ExpressionStringifier expressionStringifier = new Stringifier();
	CypherStatementConverter prettifier = new CypherStatementConverter(expressionStringifier, label);

	Statement apply = (Statement) rewriter.apply(statement);
	return prettifier.asString(apply);
  }

  private String getLabel() {
	return labelSupplier.get();
  }

  private class AddLabelRewriter extends scala.runtime.AbstractFunction1<Object, Object> {

	private final String label;

	private AddLabelRewriter(String label) {
	  this.label = label;
	}

	@Override
	public Object apply(Object patternElement) {

	  if (patternElement instanceof NodePattern) {
		NodePattern nodePattern = (NodePattern) patternElement;
		Seq<LabelName> newLabels = addLabel(nodePattern);

		return nodePattern
			.copy(nodePattern.variable(), newLabels, nodePattern.properties(), nodePattern.position());
	  } else {
		return patternElement;
	  }
	}

	private Seq<LabelName> addLabel(NodePattern nodePattern) {
	  Collection<LabelName> existingLabels = new ArrayList<>(
		  asJavaCollectionConverter(nodePattern.labels()).asJavaCollection());

	  existingLabels.add(new LabelName(label, nodePattern.position()));

	  return collectionAsScalaIterableConverter(existingLabels).asScala().toSeq();
	}

  }

  private class Stringifier extends ExpressionStringifier {

	private static final String EMPTY_VALUE = "";

	Stringifier() {
	  super(null);
	}

	public String node(NodePattern nodePattern) {

	  String name = nodePattern.variable().map(JFunction.func(this::apply))
		  .getOrElse(JFunction.func(() -> EMPTY_VALUE));

	  Collection<LabelName> labelNames = asJavaCollectionConverter(nodePattern.labels()).asJavaCollection();

	  String labels = (labelNames.isEmpty()) ? EMPTY_VALUE :
		  ":" + labelNames.stream().map((l) -> backtick(l.name())).collect(Collectors.joining(":"));

	  String expression = props(name + labels, nodePattern.properties());

	  return "(" + expression + ")";
	}

	private String props(String prepend, Option<Expression> e) {
	  return prepend + e.map(JFunction.func(this::apply)).getOrElse(JFunction.func(() -> EMPTY_VALUE));
	}

	private String backtick(String txt) {
	  boolean needsBackticks = !(Character.isJavaIdentifierStart(txt.charAt(0)));
	  if (needsBackticks)
		return "`" + txt + "`";
	  else
		return txt;
	}
  }

}





