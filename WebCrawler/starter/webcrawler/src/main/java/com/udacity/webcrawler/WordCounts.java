package com.udacity.webcrawler;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class that sorts the map of word counts.
 *
 */
final class WordCounts {

  /**
   * Given an unsorted map of word counts, returns a new map whose word counts are sorted according
   * to the provided comparator, and includes only the top {@param popularWordCount} words and counts.
   *
   * Reimplemented using only the Stream API and lambdas.
   *
   * @param wordCounts       the unsorted map of word counts.
   * @param popularWordCount the number of popular words to include in the result map.
   * @return a map containing the top {@param popularWordCount} words and counts in the right order.
   */
  static Map<String, Integer> sort(Map<String, Integer> wordCounts, int popularWordCount) {
    return wordCounts.entrySet()
        .stream()
        .sorted(Comparator.comparing(Map.Entry<String, Integer>::getValue).reversed() // Sort by word count in descending order
            .thenComparing(entry -> entry.getKey().length(), Comparator.reverseOrder()) // Sort by word length in descending order
            .thenComparing(Map.Entry::getKey)) // Sort alphabetically
        .limit(popularWordCount) // Limit to the top popularWordCount entries
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (e1, e2) -> e1, // In case of conflict, keep the first one (this won't happen in this case)
            LinkedHashMap::new // Maintain insertion order
        ));
  }

  private WordCounts() {
    // This class cannot be instantiated
  }
}
