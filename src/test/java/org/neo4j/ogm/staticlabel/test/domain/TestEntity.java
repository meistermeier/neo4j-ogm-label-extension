package org.neo4j.ogm.staticlabel.test.domain;

import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class TestEntity {

  private Long id;

  private String name;

  public void setName(String name) {
	this.name = name;
  }

  public String getName() {
	return name;
  }
}
