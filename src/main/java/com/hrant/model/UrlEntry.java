package com.hrant.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/*
 * Database URL entity
 * Author: Hrant Vardanyan
 */
@Entity
public class UrlEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;
	private String parentUrl;
	private String childUrl;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getParentUrl() {
		return parentUrl;
	}

	public void setParentUrl(String parentUrl) {
		this.parentUrl = parentUrl;
	}

	public String getChildUrl() {
		return childUrl;
	}

	public void setChildUrl(String childUrl) {
		this.childUrl = childUrl;
	}

}
