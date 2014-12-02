package com.hrant.utils;

import java.util.regex.Pattern;

/*
 * Constants
 * Author: Hrant Vardanyan
 */
public class Constants {

	public static final String BROWSER = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.107 Safari/537.36";
	public static final int TIMEOUT = 10_000;
	public static volatile boolean flag = false;
	public static Pattern REGEX_DOMAIN = Pattern.compile("((http://|https://).*?)/");
	

}
