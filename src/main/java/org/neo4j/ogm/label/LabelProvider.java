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

import static scala.collection.JavaConverters.*;

import scala.Function1;
import scala.Option;
import scala.collection.Seq;
import scala.compat.java8.JFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.opencypher.v9_0.ast.Statement;
import org.opencypher.v9_0.ast.prettifier.ExpressionStringifier;
import org.opencypher.v9_0.ast.prettifier.Prettifier;
import org.opencypher.v9_0.expressions.LabelName;
import org.opencypher.v9_0.expressions.NodePattern;
import org.opencypher.v9_0.parser.CypherParser;
import org.opencypher.v9_0.util.bottomUp;

/**
 * Support for taking a Cypher query and adding a label
 * to every node in the query.
 *
 * @author Gerrit Meier
 */
class LabelProvider {

  private static final String ILLEGAL_LABEL_MESSAGE =
	  "Only labels with alpha-numeric characters are allowed, starting with an alphabetic character. "
		  + "This label does not match the rule: ";

  private final Supplier<String> labelSupplier;

  private LabelProvider(Supplier<String> labelSupplier) {
	this.labelSupplier = labelSupplier;
  }

  /**
   * Creates a {@link LabelProvider} instance with the given
   * static label to use on each query processed with it.
   *
   * @param label Label to add to the queries nodes.
   * @return {@link LabelProvider} instance
   */
  static LabelProvider forLabel(String label) {
	return new LabelProvider(() -> label);
  }

  /**
   * Creates a {@link LabelProvider} instance with the given
   * supplier function to use on each query processed with it.
   *
   * @param labelSupplier Supplier function to get called before each cypher modification.
   * @return {@link LabelProvider} instance
   */
  static LabelProvider forLabel(Supplier<String> labelSupplier) {
	return new LabelProvider(labelSupplier);
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
   * Parses a given Cypher query, adds the label and rewrites it.
   *
   * @param cypher Cypher query to get manipulated.
   * @return Cypher query with label added.
   */
  String addLabel(String cypher) {
	String label = getLabel();
	if (hasIllegalForm(label)) {
	  throw new IllegalArgumentException(ILLEGAL_LABEL_MESSAGE + label);
	}

	CypherParser cypherParser = new CypherParser();
	Statement statement = cypherParser.parse(cypher, Option.empty());

	Function1<Object, Object> rewriter = bottomUp.apply(new AddLabelRewriter(label), JFunction.func((o) -> false));

	Statement apply = (Statement) rewriter.apply(statement);

	Prettifier prettifier = new Prettifier(new ExpressionStringifier(null));
	return prettifier.asString(apply).replaceAll("\n"," ").replaceAll(" {2}", "");
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
			.copy(nodePattern.variable(), newLabels, nodePattern.properties(), nodePattern.baseNode(),
				nodePattern.position());
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

}





