package com.example.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.*;

@SpringBootApplication
@RestController
public class UrlshortenerApplication {

	private final Map<String, String> keyMap = new HashMap<>();
	private final Map<String, String> valueMap = new HashMap<>();
	private final Map<String, Integer> domainCount = new HashMap<>();
	private final String domain = "http://tinyurl.com";
	private final char[] myChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
	private final Random myRand = new Random();
	private final int keyLength = 6;

	public static void main(String[] args) {
		SpringApplication.run(UrlshortenerApplication.class, args);
	}

	@PostMapping("/shorten")
	public Map<String, String> shorten(@RequestBody Map<String, String> request) {
		String longURL = request.get("url");
		if (longURL == null || longURL.isEmpty())
			return Map.of("error", "URL is required");
		longURL = sanitizeURL(longURL);
		String code = valueMap.computeIfAbsent(longURL, this::generateKey);
		keyMap.putIfAbsent(code, longURL);
		try {
			String dom = URI.create(longURL).getHost().replace("www.", "");
			domainCount.put(dom, domainCount.getOrDefault(dom, 0) + 1);
		} catch (Exception ignored) {
		}
		return Map.of("short_url", domain + "/" + code);
	}

	@GetMapping("/{code}")
	public void redirect(@PathVariable String code, HttpServletResponse response) throws IOException {
		String longURL = keyMap.get(code);
		if (longURL == null) {
			response.setStatus(404);
			response.getWriter().write("URL not found");
			return;
		}
		response.sendRedirect(longURL);
	}

	@GetMapping("/metrics")
	public Map<String, Integer> metrics() {
		return domainCount.entrySet()
				.stream()
				.sorted((a, b) -> b.getValue() - a.getValue())
				.limit(3)
				.collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
	}

	private String generateKey(String url) {
		String key;
		do {
			key = "";
			for (int i = 0; i < keyLength; i++)
				key += myChars[myRand.nextInt(62)];
		} while (keyMap.containsKey(key));
		return key;
	}

	private String sanitizeURL(String url) {
		if (url.startsWith("http://"))
			url = url.substring(7);
		if (url.startsWith("https://"))
			url = url.substring(8);
		if (url.endsWith("/"))
			url = url.substring(0, url.length() - 1);
		return url;
	}
}
