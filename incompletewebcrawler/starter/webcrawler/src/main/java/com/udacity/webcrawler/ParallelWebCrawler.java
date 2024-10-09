package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

final class ParallelWebCrawler implements WebCrawler {
    private final Clock clock;
    private final Duration timeout;
    private final int popularWordCount;
    private final ForkJoinPool pool;
    private final PageParserFactory parserFactory;
    private final CrawlerConfiguration config;
    private static final Logger logger = Logger.getLogger(ParallelWebCrawler.class.getName());

    @Inject
    ParallelWebCrawler(
            Clock clock,
            @Timeout Duration timeout,
            @PopularWordCount int popularWordCount,
            @TargetParallelism int threadCount,
            PageParserFactory parserFactory,
            CrawlerConfiguration config) {
        this.clock = clock;
        this.timeout = timeout;
        this.popularWordCount = popularWordCount;
        this.parserFactory = parserFactory;
        this.config = config;

        // Ensure the thread count is greater than 1 to allow proper parallelism.
        logger.info("Thread count set to: " + threadCount);
        this.pool = new ForkJoinPool(threadCount);
    }

   @Override
   public CrawlResult crawl(List<String> startingUrls) {
       Instant deadline = clock.instant().plus(timeout);

       ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();
       ConcurrentMap<String, AtomicInteger> wordCounts = new ConcurrentHashMap<>();

       // Create and fork tasks for each starting URL
       List<CrawlTask> tasks = startingUrls.stream()
           .map(url -> new CrawlTask.Builder()
               .setUrl(url)
               .setDeadline(deadline)
               .setVisitedUrls(visitedUrls)
               .setWordCounts(wordCounts)
               .setMaxDepth(config.getMaxDepth())
               .setConfig(config)
               .setClock(clock)
               .setParserFactory(parserFactory)
               .build())
           .collect(Collectors.toList());

        // Fork each task
        tasks.forEach(ForkJoinTask::fork);

        // Join each task to ensure proper synchronization
        tasks.forEach(ForkJoinTask::join);

        // Convert AtomicInteger values in wordCounts to Integer and sort results
        Map<String, Integer> sortedWordCounts = wordCounts.entrySet().stream()
                .sorted((e1, e2) -> {
                    int cmp = Integer.compare(e2.getValue().get(), e1.getValue().get());
                    return (cmp != 0) ? cmp : e1.getKey().compareTo(e2.getKey());
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get(),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        // Collect the top `popularWordCount` words
        Map<String, Integer> topWords = sortedWordCounts.entrySet().stream()
                .limit(popularWordCount)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        // Log final results for verification
        logger.info("Final URLs Visited: " + visitedUrls);
        logger.info("Final URLs Visited Count: " + visitedUrls.size());
        logger.info("Final Word Counts: " + topWords);

        // Build and return the CrawlResult
        return new CrawlResult.Builder()
                .setWordCounts(topWords)
                .setUrlsVisited(visitedUrls.size())
                .build();
    }

    @Override
    public int getMaxParallelism() {
        return Math.max(Runtime.getRuntime().availableProcessors(), 2);
    }
}
