package org.neo4j.ogm.staticlabel;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.neo4j.ogm.spi.CypherModificationProvider;

public class StaticLabelModificationProvider implements CypherModificationProvider {

  @Override
  public Function<String, String> getCypherModification(Map<String, Object> configProperties) {
	Object staticLabelValue = configProperties.get("cypher.modification.staticlabel");
	if (staticLabelValue == null) {
	  return StaticLabel.noOp()::addLabel;
	}
	if (staticLabelValue instanceof String) {
	  return StaticLabel.forLabel((String) staticLabelValue)::addLabel;
	}
	if (staticLabelValue instanceof Supplier) {
	  return StaticLabel.forLabel((Supplier<String>) staticLabelValue)::addLabel;
	}
	throw new IllegalArgumentException("cypher.modification.staticlabel value type is not supported."
		+ " It should either be a String constant or a Function<String, String>");
  }
}
