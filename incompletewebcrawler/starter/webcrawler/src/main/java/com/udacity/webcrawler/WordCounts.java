package com.udacity.webcrawler;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Utility class that sorts the map of word counts.
 *
 * <p>Reimplemented the sort() method using the Stream API and lambdas and/or method references.</p>
 */
final class WordCounts {

    /**
     * Given an unsorted map of word counts, returns a new map whose word counts are sorted according
     * to the provided criteria, and includes only the top {@param popularWordCount} words and counts.
     *
     * @param wordCounts       the unsorted map of word counts (with AtomicInteger values).
     * @param popularWordCount the number of popular words to include in the result map.
     * @return a map containing the top {@param popularWordCount} words and counts in the right order.
     */
    static Map<String, AtomicInteger> sort(Map<String, AtomicInteger> wordCounts, int popularWordCount) {
        // Ensure thread-safe access by using ConcurrentHashMap
        Map<String, AtomicInteger> concurrentWordCounts = new ConcurrentHashMap<>(wordCounts);

        return concurrentWordCounts.entrySet().stream()
            .sorted((entry1, entry2) -> {
                // Sort by word count in descending order
                int countComparison = Integer.compare(entry2.getValue().get(), entry1.getValue().get());
                if (countComparison != 0) {
                    return countComparison;
                }
                // Sort by word length in descending order if counts are equal
                int lengthComparison = Integer.compare(entry2.getKey().length(), entry1.getKey().length());
                if (lengthComparison != 0) {
                    return lengthComparison;
                }
                // Sort alphabetically if counts and lengths are equal
                return entry1.getKey().compareTo(entry2.getKey());
            })
            .limit(popularWordCount)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new // Maintain insertion order
            ));
    }

    /**
     * A {@link Comparator} that sorts word count pairs correctly:
     *
     * <ol>
     *   <li>First sorting by word count, ranking more frequent words higher.</li>
     *   <li>Then sorting by word length, ranking longer words higher.</li>
     *   <li>Finally, breaking ties using alphabetical order.</li>
     * </ol>
     */
    private static final class WordCountComparator implements Comparator<Map.Entry<String, AtomicInteger>> {
        @Override
        public int compare(Map.Entry<String, AtomicInteger> a, Map.Entry<String, AtomicInteger> b) {
            int countComparison = Integer.compare(b.getValue().get(), a.getValue().get());
            if (countComparison != 0) {
                return countComparison;
            }
            int lengthComparison = Integer.compare(b.getKey().length(), a.getKey().length());
            if (lengthComparison != 0) {
                return lengthComparison;
            }
            return a.getKey().compareTo(b.getKey());
        }
    }

    private WordCounts() {
        // This class cannot be instantiated
    }
}
