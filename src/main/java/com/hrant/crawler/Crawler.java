package com.hrant.crawler;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.hrant.dao.OlxDAO;
import com.hrant.model.UrlEntry;
import com.hrant.utils.Constants;

/*
 * BFS Crawler
 * Author: Hrant Vardanyan
 */
public class Crawler {

	private static final Logger LOGGER = Logger.getLogger(Crawler.class);

	/*
	 * Simple testing
	 */
	public static void main(String[] args) throws IOException {

		Crawler crawler = new Crawler();
		LinkedList<String> queue = new LinkedList<>();
		queue.add("http://www.cypherincorporated.co.in/");
		crawler.bfsLogic(queue, 2, 0.7);

	}

	/*
	 * Harmony main logic
	 */
	private void harmonyLogic(Set<String> childUrlSet, int hms, double hmcr, String parent) {
		// Step 1: Initialize Harmony Memory (HM)
		// HMS is size of HM
		Set<String> hm = getRandomSet(childUrlSet, hms);
		
		// Stopping criteria is - 7 loops of harmony reached
		// Max harmony loops would be given by user (now hardcoded)
		for (int i = 0; i < 7; i++) {
			// Step 2.1: Check if new url must be selected for HM
			// Generate random number and check if it is smaller then HMCR
			Random rand = new Random();
			double randNumber = rand.nextDouble() * (1);
			
			if (randNumber < hmcr) {
				// Step 2.2: A random url must be taken from all urls set
				String randUrl = getRandomItemInSet(childUrlSet);
				// Compute the subdirectory level of that url
				int randUrlSubDirLevel = getSubDirectoryLevelOfUrl(randUrl);

				// Step 3.1: Get worst case in HM (maximum url)
				// Worst case (maximum url) is the url with maximum number of subdirectories
				String worstHMUrl = "";
				int worstHMUrlSubDirLevel = 1;
				for (String currUrl : hm) {
					int currLevel = getSubDirectoryLevelOfUrl(currUrl);
					if (worstHMUrlSubDirLevel < currLevel) {
						worstHMUrl = currUrl;
						worstHMUrlSubDirLevel = currLevel;
					}
				}

				// Step 3.2: Check if randUrl minimizes the HM
				// (has less subdirectories then worstHMUrl)
				if (randUrlSubDirLevel < worstHMUrlSubDirLevel) {
					// Step 3.3: It minimizes
					// Replce worstHMUrl with randUrl (minimize the HM)
					hm.remove(worstHMUrl);
					hm.add(randUrl);

					// Save randUrl in database
					UrlEntry urlEntry = new UrlEntry();
					urlEntry.setChildUrl(randUrl);
					urlEntry.setParentUrl(parent);
					urlEntry.setHarmonyScore(true);
					try {
						OlxDAO.getInstance().addMessage(urlEntry);
					} catch (Exception e) {
						LOGGER.error("error with storing db", e);
					}
				}
			} else {
				// randUrl is not minimizing the HM, continue (goto step 2)
				continue;
			}
		}

	}

	/*
	 * Detect subdirectories of url
	 */
	private int getSubDirectoryLevelOfUrl(String url) {
		int count = 0;
		Matcher subDirMatcher = Constants.REGEX_SUBDIRECTORY.matcher(url);
		while (subDirMatcher.find()) {
			count++;
		}
		return count - 1;
	}

	/*
	 * Generated Random Set (HM) based on 'HMS'
	 */
	private Set<String> getRandomSet(Set<String> set, int hms) {
		
		Set<String> randomSet = new HashSet<>();
		if (set.size() <= hms) {
			hms = set.size() - 1;
		}
		while (randomSet.size() != hms) {
			String currentRandomItem = getRandomItemInSet(set);
			randomSet.add(currentRandomItem);
		}
		return randomSet;
	}

	/*
	 * Get Random Item In Set
	 */
	private String getRandomItemInSet(Set<String> set) {

		int size = set.size();
		int item = new Random().nextInt(size);
		int i = 0;
		for (String random : set) {
			if (i == item)
				return random;
			i = i + 1;
		}
		return "";
	}

	/*
	 * BFS main logic
	 */
	private void bfsLogic(LinkedList<String> queue, int hms, double hmcr) {
		// Set to hold all passed urls
		// And check for them being unique (HashSet class holds only unique
		// objects)
		Set<String> used = new HashSet<>();
		used.addAll(queue);

		// BFS loop
		// Works while no unchecked url is left
		while (!queue.isEmpty()) {
			// Get and remove queue first element
			String currUrl = queue.poll();
			LOGGER.info("Parent: " + used.size());
			// check if parentUrl exist in DB ignore getting its childs
			if (OlxDAO.getInstance().isExist(currUrl, false)) {
				continue;
			}
			try {
				// Retrieve all links from url
				Set<String> allLinksInPage = getAllLinksInPage(currUrl);
				Set<String> harmonyLinks = new HashSet<>();
				LOGGER.info("Child: " + allLinksInPage.size());
				// For every child url
				for (String child : allLinksInPage) {
					// Check for child being unique
					// add method of HashSet class returns false, if add fails
					if (used.add(child)) {
						// Child is unique, add it at tail of queue
						if (getSubDirectoryLevelOfUrl(child) > 1) {
							harmonyLinks.add(child);
						} else {
							queue.addLast(child);
						}

						// Save entry in database
						UrlEntry urlEntry = new UrlEntry();
						urlEntry.setChildUrl(child);
						urlEntry.setParentUrl(currUrl);
						urlEntry.setHarmonyScore(false);
						try {
							OlxDAO.getInstance().addIfNotExist(urlEntry);
						} catch (Exception e) {
							LOGGER.error("error with storing db", e);
						}
					}

				}
				if (!harmonyLinks.isEmpty()) {
					harmonyLogic(harmonyLinks, hms, hmcr, currUrl);
				}

			} catch (IOException e) {
				LOGGER.error("error with getting page links ", e);
			}
		}

	}

	/*
	 * Gets all urls from page
	 */
	private Set<String> getAllLinksInPage(String pageUrl) throws IOException {
		// Get page data
		Document docOfPage = Jsoup.connect(pageUrl).ignoreContentType(true).userAgent(Constants.BROWSER)
				.timeout(Constants.TIMEOUT).get();

		// Set to hold all gathered urls
		// And check for them being unique (HashSet class holds only unique
		// objects)
		Set<String> linkSet = new HashSet<>();

		// Add '/' if url does not end with '/'
		if (!pageUrl.endsWith("/")) {
			pageUrl = pageUrl + "/";
		}

		String domain = "";
		// Extarct protocol
		String protocol = StringUtils.substringBefore(pageUrl, "//");
		// Check if url contains domain name
		Matcher domMatcher = Constants.REGEX_DOMAIN.matcher(pageUrl);

		if (domMatcher.find()) {
			// Save domain name
			domain = domMatcher.group(1);
		} else if (domain.endsWith("/")) {
			// Save domain for extended urls
			domain = StringUtils.substringBeforeLast(domain, "/");
		}

		// Get all 'a' tags
		Elements aEl = docOfPage.select("a");
		String link = "";

		// For each 'a' tag
		for (Element a : aEl) {
			// Extract its url
			String href = a.attr("href");

			// Check if url is empty
			if (!StringUtils.isEmpty(href)) {
				// Check if url is extended or not
				if (href.startsWith("http")) {
					// Protocol exists, valid url
					link = href;
				} else {
					if (href.startsWith("//")) {
						// No protocol, add protocol to url
						link = protocol + href;
					} else if (href.startsWith("/")) {
						// No domain, add domain
						link = domain + href;
					}
				}
			}

			// Add '/' at the end if not exist
			// if (!link.endsWith("/") && !StringUtils.isEmpty(link)) {
			// link = link + "/";
			// }

			if (!StringUtils.isEmpty(link)) {
				// Url is not empty, filter socials and add to set
				if (!link.contains("facebook") && !link.contains("twitter") && !link.contains("linkedin")) {
					linkSet.add(link);
				}
			}
		}

		return linkSet;
	}

}
