package com.hrant;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * JPA implementation
 * Author: Hrant Vardanyan
 */
public class JpaUtil {
	private static final Logger LOG = LoggerFactory.getLogger(JpaUtil.class);

	private static final EntityManagerFactory emf;
	static {
		try {
			emf = Persistence.createEntityManagerFactory("urlEntry");
			
		} catch (Throwable ex) {
			LOG.error("Initial  EntityManagerFactory creation failed", ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static EntityManagerFactory getEMF() {
		return emf;
	}

}
