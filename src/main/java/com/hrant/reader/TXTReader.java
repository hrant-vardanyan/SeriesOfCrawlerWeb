package com.hrant.reader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class TXTReader {
	
	private static final Logger LOGGER = Logger.getLogger(TXTReader.class);

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
