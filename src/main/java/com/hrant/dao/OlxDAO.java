package com.hrant.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;

import com.hrant.JpaUtil;
import com.hrant.model.UrlEntry;

/*
 * Database connection handler
 * Author: Hrant Vardanyan
 */
public class OlxDAO {

	/*
	 * Singletone pattern
	 */
	private static OlxDAO INSTANCE = new OlxDAO();
	private static final Logger LOGGER = Logger.getLogger(OlxDAO.class);

	public static OlxDAO getInstance() {
		return OlxDAO.INSTANCE;
	}

	private OlxDAO() {
	}

	/*
	 * Store UrlEntry object int DB if not exist
	 */
	public void addIfNotExist(UrlEntry urlEntry) {
		String childUrl = urlEntry.getChildUrl();
		if (!isExist(childUrl, true)) {
			try {
				addMessage(urlEntry);
			} catch (Exception e) {
				LOGGER.error("error with stroing object into DB ", e);
			}
		}

	}

	/*
	 * Entry Saver
	 */
	private void addMessage(UrlEntry urlEntry) {
		EntityManager entityManager = null;
		try {
			// Open connection and save entity
			entityManager = JpaUtil.getEMF().createEntityManager();
			entityManager.getTransaction().begin();
			entityManager.persist(urlEntry);
			entityManager.getTransaction().commit();
		} finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}
	}

	/*
	 * Check if childUrl exist in DB to avoid duplicates
	 */
	public boolean isExist(String checkingUrl, boolean isChild) {
		EntityManager entityManager = null;
		try {
			entityManager = JpaUtil.getEMF().createEntityManager();
			entityManager.getTransaction().begin();
			// queryPart created for checking to get parentUrl or childUrl field
			// from DB
			String queryPart = "parentUrl";
			if (isChild) {
				queryPart = "childUrl";
			}
			TypedQuery<UrlEntry> query = entityManager.createQuery("SELECT c FROM UrlEntry c WHERE c." + queryPart
					+ " = ? ", UrlEntry.class);
			query.setParameter(1, checkingUrl);
			List<UrlEntry> resultList = query.getResultList();
			entityManager.getTransaction().commit();
			if (resultList.isEmpty()) {
				return false;// does not exist
			} else {
				return true;
			}
		} finally {
			if (entityManager != null) {
				entityManager.close();
			}
		}
	}

}
