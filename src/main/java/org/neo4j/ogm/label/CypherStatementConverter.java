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

import scala.Function0;
import scala.Function1;
import scala.Option;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import scala.compat.java8.JFunction;
import scala.compat.java8.JFunction1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.neo4j.cypher.internal.frontend.v3_4.ast.*;
import org.neo4j.cypher.internal.frontend.v3_4.prettifier.ExpressionStringifier;
import org.neo4j.cypher.internal.frontend.v3_4.prettifier.Prettifier;
import org.neo4j.cypher.internal.util.v3_4.InputPosition;
import org.neo4j.cypher.internal.v3_4.expressions.LabelName;
import org.neo4j.cypher.internal.v3_4.expressions.PatternPart;

/**
 * Class in charge of handling the conversion
 * of a Cypher statement to its string representation.
 *
 * @author Gerrit Meier
 */
class CypherStatementConverter extends Prettifier {

  private static final String EMPTY_VALUE = "";
  private static final String COMMA_DELIMITER = ", ";
  private static final String DOT_DELIMITER = ".";

  private final ExpressionStringifier mkStringOf;
  private final String additionalLabel;
  private final Function0<String> emptyStringFunc = JFunction.func(() -> EMPTY_VALUE);

  CypherStatementConverter(ExpressionStringifier mkStringOf, String additionalLabel) {
	super(mkStringOf);
	this.mkStringOf = mkStringOf;
	this.additionalLabel = additionalLabel;
  }

  /**
   * Converts a Cypher statement to its string representation.
   *
   * @param statement Statement that gets converted to string.
   * @return The given statement as string.
   */
  @Override
  public String asString(Statement statement) {
	if (statement instanceof Query) {
	  Seq<Clause> clauses = ((SingleQuery) ((Query) statement).part()).clauses();
	  Collection<Clause> clauseAsJavaCollection = asJava(clauses);

	  return clauseAsJavaCollection.stream().map(this::dispatch).collect(Collectors.joining(" "));
	}
	return EMPTY_VALUE;
  }

  private String dispatch(Clause clause) {
	if (clause instanceof Return) {
	  return asString((Return) clause);
	}
	if (clause instanceof Match) {
	  return asString((Match) clause);
	}
	if (clause instanceof With) {
	  return asString((With) clause);
	}
	if (clause instanceof Create) {
	  return asString((Create) clause);
	}
	if (clause instanceof Unwind) {
	  return asString((Unwind) clause);
	}
	if (clause instanceof UnresolvedCall) {
	  return asString((UnresolvedCall) clause);
	}
	if (clause instanceof SetClause) {
	  return asString((SetClause) clause);
	}
	if (clause instanceof Delete) {
	  return asString((Delete) clause);
	}
	if (clause instanceof Merge) {
	  return asString((Merge) clause);
	}
	return clause.asCanonicalStringVal();
  }

  private String asString(Return ret) {
	return "RETURN" + distinct(ret) + items(ret) + orderBy(ret) + skip(ret) + limit(ret);
  }

  private String asString(With w) {
	return "WITH" + distinct(w) + items(w) + orderBy(w) + skip(w) + limit(w) + where(w);
  }

  private String asString(Skip o) {
	return "SKIP " + mkStringOf.apply(o.expression());
  }

  private String asString(Limit o) {
	return "LIMIT " + mkStringOf.apply(o.expression());
  }

  private String asString(OrderBy o) {

	String orderBy = asJava(o.sortItems()).stream().map(sortItem -> {
	  String sortExpression = mkStringOf.apply(sortItem.expression());
	  if (sortItem instanceof AscSortItem) {
		return sortExpression + " ASCENDING";
	  } else if (sortItem instanceof DescSortItem) {
		return sortExpression + " DESCENDING";
	  }
	  return EMPTY_VALUE;
	}).collect(Collectors.joining(COMMA_DELIMITER));

	return "ORDER BY " + orderBy;
  }

  private String asString(ReturnItem r) {
	String itemExpression = mkStringOf.apply(r.expression());
	if (r instanceof AliasedReturnItem) {
	  return itemExpression + " AS " + mkStringOf.apply(((AliasedReturnItem) r).variable());
	} else if (r instanceof UnaliasedReturnItem) {
	  return itemExpression;
	}
	return EMPTY_VALUE;
  }

  private String asString(Unwind u) {
	return "UNWIND " + mkStringOf.apply(u.expression()) + " AS " + mkStringOf.apply(u.variable());
  }

  private String asString(Create c) {
	return "CREATE " + patterns(c);
  }

  private String asString(Delete d) {
	return "DELETE " + asJava(d.expressions()).stream().map(mkStringOf::apply).collect(Collectors.joining(
		COMMA_DELIMITER));
  }

  private String asString(Merge m) {
	return "MERGE " + patterns(m).replace(":" + additionalLabel, "");
  }

  @Override
  public String asString(Match m) {
	String optionalPart = m.optional() ? "OPTIONAL " : EMPTY_VALUE;

	Function1<Where, String> whereClauseFunc = JFunction
		.func((JFunction1<Where, String>) (a) -> " WHERE " + mkStringOf.apply(a.expression()));

	String where = m.where().map(whereClauseFunc).getOrElse(emptyStringFunc);

	return optionalPart + "MATCH " + patterns(m) + where;
  }

  private String asString(UnresolvedCall unresolvedCall) {

	String parts = asJava(unresolvedCall.procedureNamespace().parts())
		.stream().map(String::toString)
		.collect(Collectors.joining(DOT_DELIMITER));

	String arguments = unresolvedCall.declaredArguments().isEmpty() ? EMPTY_VALUE :
		asJava(unresolvedCall.declaredArguments().get()).stream().map(mkStringOf::apply)
			.collect(Collectors.joining(COMMA_DELIMITER));

	JFunction1<ProcedureResult, String> procedureYieldItemsFunc = (JFunction1<ProcedureResult, String>) JFunction
		.func((ProcedureResult ret) ->
			asJava(ret.items()).stream()
				.map(item -> mkStringOf.apply(item.variable()))
				.collect(Collectors.joining(COMMA_DELIMITER)));

	Option<ProcedureResult> declaredResult = unresolvedCall.declaredResult();
	String returns = declaredResult.isEmpty() ? EMPTY_VALUE : " YIELD " +
		declaredResult.map(JFunction.func(procedureYieldItemsFunc)).get();

	return "CALL " + parts + DOT_DELIMITER + unresolvedCall.procedureName().name() + "(" + arguments + ")" + returns;
  }

  private String asString(SetClause setClause) {
	Function<SetItem, String> propertyToStringFunc = (s) -> {
	  if (s instanceof SetPropertyItem) {
		return mkStringOf.apply(((SetPropertyItem) s).property()) + "=" +
			mkStringOf.apply(((SetPropertyItem) s).expression());

	  } else if (s instanceof SetLabelItem) {
		List<LabelName> labelNames = new ArrayList<>(asJava(((SetLabelItem) s).labels()));
		labelNames.add(new LabelName(additionalLabel, InputPosition.NONE()));
		String labels = labelNames.stream().map(label -> "`" + label.name() + "`")
			.collect(Collectors.joining(":"));
		return mkStringOf.apply(((SetLabelItem) s).variable()) + ":" + labels;

	  } else if (s instanceof SetIncludingPropertiesFromMapItem) {
		SetIncludingPropertiesFromMapItem sc = (SetIncludingPropertiesFromMapItem) s;
		String variable = mkStringOf.apply(sc.variable());
		String expression = mkStringOf.apply(sc.expression());
		return variable + " += " + expression;

	  } else if (s instanceof SetExactPropertiesFromMapItem) {
		SetExactPropertiesFromMapItem sc = (SetExactPropertiesFromMapItem) s;
		String variable = mkStringOf.apply(sc.variable());
		String expression = mkStringOf.apply(sc.expression());
		return variable + " = " + expression;

	  } else {
		return s.asCanonicalStringVal();
	  }
	};

	String items = asJava(setClause.items()).stream().map(propertyToStringFunc).collect(
		Collectors.joining(DOT_DELIMITER));

	return "SET " + items;
  }

  private String patterns(Match m) {
	Seq<PatternPart> patternPartSeq = m.pattern().patternParts();
	return patterns(patternPartSeq);
  }

  private String patterns(Merge m) {
	Seq<PatternPart> patternPartSeq = m.pattern().patternParts();
	return patterns(patternPartSeq);
  }

  private String patterns(Create c) {
	Seq<PatternPart> patternPartSeq = c.pattern().patternParts();
	return patterns(patternPartSeq);
  }

  private String patterns(Seq<PatternPart> patternParts) {
	Collection<PatternPart> javaPatternParts = JavaConverters.asJavaCollectionConverter(patternParts)
		.asJavaCollection();
	return javaPatternParts.stream().map(this::asString).collect(Collectors.joining(COMMA_DELIMITER));
  }

  private String distinct(ProjectionClause clause) {
	return clause.distinct() ? " DISTINCT " : " ";
  }

  private String items(ProjectionClause clause) {
	return asJava(clause.returnItems().items()).stream().map(this::asString)
		.collect(Collectors.joining(COMMA_DELIMITER));
  }

  private String orderBy(ProjectionClause clause) {
	return clause.orderBy().map(JFunction.func(this::asString)).getOrElse(emptyStringFunc);
  }

  private String limit(ProjectionClause clause) {
	return clause.limit().map(JFunction.func(this::asString)).getOrElse(emptyStringFunc);
  }

  private String skip(ProjectionClause clause) {
	return clause.skip().map(JFunction.func(this::asString)).getOrElse(emptyStringFunc);
  }

  private String where(With with) {
	return with.where()
		.map(JFunction.func(wh -> " WHERE " + mkStringOf.apply(wh.expression())))
		.getOrElse(emptyStringFunc);
  }

  private <T> Collection<T> asJava(Seq<T> scalaSequence) {
	return JavaConverters.asJavaCollectionConverter(scalaSequence).asJavaCollection();
  }
}
