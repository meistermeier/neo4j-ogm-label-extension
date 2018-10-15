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

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.neo4j.ogm.spi.CypherModificationProvider;

/**
 * The label extension provider as implementation of the {@code CypherModificationProvider}.
 *
 * @author Gerrit Meier
 */
public class LabelModificationProvider implements CypherModificationProvider {

  public static final String CONFIGURATION_KEY = "cypher.modification.label";

  @Override
  public Function<String, String> getCypherModification(Map<String, Object> configProperties) {
	Object labelValue = configProperties.get(CONFIGURATION_KEY);
	if (labelValue == null) {
	  return Function.identity();
	}
	if (labelValue instanceof String) {
	  return LabelProvider.forLabel((String) labelValue)::addLabel;
	}
	if (labelValue instanceof Supplier) {
	  return LabelProvider.forLabel((Supplier<String>) labelValue)::addLabel;
	}
	throw new IllegalArgumentException(CONFIGURATION_KEY + " value type is not supported."
		+ " It should either be a String constant or a Function<String, String>");
  }
}
