package com.udacity.webcrawler;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class WordCounter {
    private static volatile WordCounter instance;
    private final Map<String, AtomicInteger> wordCounts;

    // Regular expression to ignore words with 3 or fewer characters
    private static final Pattern IGNORED_WORDS_PATTERN = Pattern.compile("^.{1,3}$");

    private WordCounter() {
        wordCounts = new ConcurrentHashMap<>();
    }

    // Thread-safe singleton pattern implementation
    public static WordCounter getInstance() {
        if (instance == null) {
            synchronized (WordCounter.class) {
                if (instance == null) {
                    instance = new WordCounter();
                }
            }
        }
        return instance;
    }

    // Adds a word count (or increments an existing one)
    public void addWord(String word, int count) {
        // Ignore words that match the regular expression (3 or fewer characters)
        if (IGNORED_WORDS_PATTERN.matcher(word).matches()) {
            System.out.println("Ignoring word: " + word); // Debugging log
            return;
        }

        wordCounts.compute(word, (k, v) -> {
            if (v == null) {
                return new AtomicInteger(count);
            } else {
                v.addAndGet(count);
                return v;
            }
        });
    }

    // Returns the word counts map with AtomicInteger values
    public Map<String, AtomicInteger> getWordCounts() {
        return wordCounts;
    }

    // Returns the word counts map as Integer values
    public Map<String, Integer> getWordCountsAsIntegers() {
        return wordCounts.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().get()  // Convert AtomicInteger to Integer
            ));
    }

    public void reset() {
        wordCounts.clear();
    }
}
