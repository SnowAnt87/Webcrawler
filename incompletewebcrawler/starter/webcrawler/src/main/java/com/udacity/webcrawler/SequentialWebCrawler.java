package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

/**
 * A {@link WebCrawler} that downloads and processes one page at a time.
 */
final class SequentialWebCrawler implements WebCrawler {

  private final Clock clock;
  private final PageParserFactory parserFactory;
  private final Duration timeout;
  private final int popularWordCount;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;

  @Inject
  SequentialWebCrawler(
      Clock clock,
      PageParserFactory parserFactory,
      @Timeout Duration timeout,
      @PopularWordCount int popularWordCount,
      @MaxDepth int maxDepth,
      @IgnoredUrls List<Pattern> ignoredUrls) {
    this.clock = clock;
    this.parserFactory = parserFactory;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    Instant deadline = clock.instant().plus(timeout);
    Set<String> visitedUrls = new HashSet<>();

    // Reset the word counter to ensure a fresh start for each crawl.
    WordCounter wordCounter = WordCounter.getInstance();
    wordCounter.reset();  // Resetting any previous counts

    for (String url : startingUrls) {
      crawlInternal(url, deadline, maxDepth, visitedUrls);
    }

    // Get the word counts from WordCounter
    Map<String, AtomicInteger> counts = wordCounter.getWordCounts();

    if (counts.isEmpty()) {
      return new CrawlResult.Builder()
          .setWordCounts(counts.entrySet().stream()
              .collect(Collectors.toMap(
                  Map.Entry::getKey,
                  entry -> entry.getValue().get(),
                  (e1, e2) -> e1,
                  LinkedHashMap::new
              ))
          )
          .setUrlsVisited(visitedUrls.size())
          .build();
    }

    // Sort the map of counts and limit to popularWordCount entries
    Map<String, Integer> sortedCounts = counts.entrySet()
        .stream()
        .sorted((entry1, entry2) -> {
          // Sorting by count in descending order
          int countComparison = Integer.compare(entry2.getValue().get(), entry1.getValue().get());
          if (countComparison != 0) {
            return countComparison;
          }
          // Sorting alphabetically if counts are equal
          return entry1.getKey().compareTo(entry2.getKey());
        })
        .limit(popularWordCount)
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().get(),
            (e1, e2) -> e1, // Merge function
            LinkedHashMap::new // Keep insertion order
        ));

    return new CrawlResult.Builder()
        .setWordCounts(sortedCounts)
        .setUrlsVisited(visitedUrls.size())
        .build();
  }

  private void crawlInternal(
      String url,
      Instant deadline,
      int maxDepth,
      Set<String> visitedUrls) {
    if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
      return;
    }
    for (Pattern pattern : ignoredUrls) {
      if (pattern.matcher(url).matches()) {
        return;
      }
    }
    if (visitedUrls.contains(url)) {
      return;
    }
    visitedUrls.add(url);

    PageParser.Result result = parserFactory.get(url).parse();

    // Add words to the WordCounter instance
    WordCounter wordCounter = WordCounter.getInstance();
    for (Map.Entry<String, Integer> entry : result.getWordCounts().entrySet()) {
      wordCounter.addWord(entry.getKey(), entry.getValue());
    }

    for (String link : result.getLinks()) {
      crawlInternal(link, deadline, maxDepth - 1, visitedUrls);
    }
  }
}
