package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.regex.Pattern;


public class CrawlTask extends RecursiveTask {
    private static final Logger logger = Logger.getLogger(CrawlTask.class.getName());

    private final String url;
    private final Instant deadline;
    private ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();
    private final ConcurrentMap<String, AtomicInteger> wordCounts;
    private final int maxDepth;
    private final CrawlerConfiguration config;
    private final Clock clock;
    private final PageParserFactory parserFactory;

    // Private constructor to enforce the use of the Builder
    private CrawlTask(Builder builder) {
        this.url = builder.url;
        this.deadline = builder.deadline;
        this.visitedUrls = builder.visitedUrls;
        this.wordCounts = builder.wordCounts;
        this.maxDepth = builder.maxDepth;
        this.config = builder.config;
        this.clock = builder.clock;
        this.parserFactory = builder.parserFactory;
    }

    @Override
    protected Void compute() {
        // Check if the current time exceeds the deadline or max depth is reached
        if (clock.instant().isAfter(deadline) || maxDepth <= 0) {
            logger.info("Reached max depth or timeout for URL: " + url);
            return null;
        }

        // Load the ignored URLs from the configuration
        List<Pattern> ignoredUrls = config.getIgnoredUrls(); // Initialize ignoredUrls list here

        // Check if the URL matches any ignored pattern and skip if it does
        if (ignoredUrls != null && ignoredUrls.stream().anyMatch(pattern -> pattern.matcher(url).matches())) {
            logger.info("Ignoring URL due to matching ignored pattern: " + url);
            return null;
        }

        // Try to add the URL to visitedUrls; if it fails, it means this URL has already been visited
        if (!visitedUrls.add(url)) {
            logger.info("URL already visited: " + url);
            return null;
        }

        logger.info("Crawling URL: " + url);
        PageParser.Result result = parserFactory.get(url).parse();
        logger.info("Parsed result for URL: " + url + " with word counts: " + result.getWordCounts());

        // Update wordCounts concurrently
        result.getWordCounts().forEach((word, count) -> {
            String lowerCaseWord = word.toLowerCase();
            if (config.getIgnoredWords().stream().noneMatch(pattern -> pattern.matcher(lowerCaseWord).matches())) {
                wordCounts.compute(lowerCaseWord, (k, v) -> v == null ? new AtomicInteger(count) : v.addAndGet(count) == 0 ? v : v);  // old code: wordCounts.compute(lowerCaseWord, (k, v) -> (v == null) ? new AtomicInteger(count) : new AtomicInteger(v.get() + count));
                logger.info("Updated word count for: " + lowerCaseWord + " to " + wordCounts.get(lowerCaseWord).get());
            } else {
                logger.info("Ignoring word due to ignored word pattern: " + lowerCaseWord);
            }
        });

        // Create subtasks for each link found on the page and fork them
        List<CrawlTask> tasks = result.getLinks().stream()
                .sorted() // Sort links to ensure consistent ordering for test predictability
                .map(link -> new CrawlTask.Builder()
                        .setUrl(link)
                        .setDeadline(deadline)
                        .setVisitedUrls(visitedUrls)
                        .setWordCounts(wordCounts)
                        .setMaxDepth(maxDepth - 1)
                        .setConfig(config)
                        .setClock(clock)
                        .setParserFactory(parserFactory)
                        .build())
                .collect(Collectors.toList());

        invokeAll(tasks);

        return null;
    }


    // Static Builder class
    public static class Builder {
        private String url;
        private Instant deadline;
        ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>(); // Thread-safe visited URLs
        private ConcurrentMap<String, AtomicInteger> wordCounts = new ConcurrentSkipListMap<>(); // Consistent ordering
        private int maxDepth;
        private CrawlerConfiguration config;
        private Clock clock;
        private PageParserFactory parserFactory;

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setDeadline(Instant deadline) {
            this.deadline = deadline;
            return this;
        }

        public Builder setVisitedUrls(ConcurrentSkipListSet<String> visitedUrls) {
            this.visitedUrls = visitedUrls;
            return this;
        }

        public Builder setWordCounts(ConcurrentMap<String, AtomicInteger> wordCounts) {
            this.wordCounts = wordCounts;
            return this;
        }

        public Builder setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder setConfig(CrawlerConfiguration config) {
            this.config = config;
            return this;
        }

        public Builder setClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder setParserFactory(PageParserFactory parserFactory) {
            this.parserFactory = parserFactory;
            return this;
        }

        public CrawlTask build() {
            return new CrawlTask(this);
        }
    }
}
