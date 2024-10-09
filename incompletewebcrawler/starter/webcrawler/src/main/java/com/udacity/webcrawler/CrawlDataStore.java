package com.udacity.webcrawler;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CrawlDataStore {
    // Singleton instance
    private static final CrawlDataStore INSTANCE = new CrawlDataStore();

    private final Set<String> visitedUrls;
    private final ConcurrentHashMap<String, AtomicInteger> wordCounts;

    // Private constructor to prevent instantiation from other classes
    private CrawlDataStore() {
        this.visitedUrls = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.wordCounts = new ConcurrentHashMap<>();
    }

    // Public method to get the single instance of CrawlDataStore
    public static CrawlDataStore getInstance() {
        return INSTANCE;
    }

    // Getter for visitedUrls
    public Set<String> getVisitedUrls() {
        return visitedUrls;
    }

    // Getter for wordCounts
    public ConcurrentHashMap<String, AtomicInteger> getWordCounts() {
        return wordCounts;
    }
}
