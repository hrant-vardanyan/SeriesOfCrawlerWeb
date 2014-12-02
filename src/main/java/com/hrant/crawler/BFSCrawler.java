package com.hrant.crawler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
public class BFSCrawler {

	private static final Logger LOGGER = Logger.getLogger(BFSCrawler.class);

	/*
	 * Simple testing
	 */
	public static void main(String[] args) throws IOException {

		BFSCrawler bfsCrawler = new BFSCrawler();

		LinkedList<String> queue = new LinkedList<>();
		queue.add("http://ceng.gazi.edu.tr/~ozdemir/");
		bfsCrawler.bfsLogic(queue);

	}

	/*
	 * BFS main logic
	 */
	public void bfsLogic(LinkedList<String> queue) {
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
				LOGGER.info("Child: " + allLinksInPage.size());
				// For every child url
				for (String child : allLinksInPage) {
					// Check for child being unique
					// add method of HashSet class returns false, if add fails
					if (used.add(child)) {
						// Child is unique, add it at tail of queue
						queue.addLast(child);

						// Save entry in database
						UrlEntry urlEntry = new UrlEntry();
						urlEntry.setChildUrl(child);
						urlEntry.setParentUrl(currUrl);
						try {
							OlxDAO.getInstance().addIfNotExist(urlEntry);
						} catch (Exception e) {
							LOGGER.error("error with storing db", e);
						}
					}
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
				if (!link.contains("facebook") && !link.contains("twitter")) {
					linkSet.add(link);
				}
			}
		}

		return linkSet;
	}

	/*
	 * Read initial urls from text file
	 */
	public static List<String> readLinksInTXT(Path inputPath) {
		File file = new File(inputPath.toString());
		List<String> linkList = new ArrayList<>();

		try {
			FileReader reader = new FileReader(file);
			char[] chars = new char[(int) file.length()];
			reader.read(chars);
			String content = new String(chars);
			String linksArray[] = content.split("\\r?\\n");
			List<String> linkListWithEmptyLines = Arrays.asList(linksArray);

			for (String link : linkListWithEmptyLines) {
				if (!StringUtils.isEmpty(link)) {
					link = java.net.URLDecoder.decode(link, "UTF-8");
					linkList.add(link);
				}
			}

			reader.close();
		} catch (IOException e) {
			LOGGER.error("Exception getting data from  " + inputPath, e);
		}

		return linkList;
	}

}
