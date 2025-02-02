package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;


import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.nio.file.Files;
import java.util.Objects;

public final class WebCrawlerMain {

  private final CrawlerConfiguration config;

  private WebCrawlerMain(CrawlerConfiguration config) {
    this.config = Objects.requireNonNull(config);
  }

  @Inject
  private WebCrawler crawler;

  @Inject
  private Profiler profiler;

  private void run() throws Exception {
    // Inject dependencies using Guice
    Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

    // Perform the web crawling
    CrawlResult result = crawler.crawl(config.getStartPages());
    CrawlResultWriter resultWriter = new CrawlResultWriter(result);

    // Write crawl results to JSON file or System.out if result path is empty
    String resultPath = config.getResultPath();
    if (!resultPath.isEmpty()) {
      Path path = Paths.get(resultPath);
      resultWriter.write(path);
    } else {
      Writer outputWriter = new OutputStreamWriter(System.out);
      resultWriter.write(outputWriter);
    }

    // Write profiler data to a text file or System.out if profiler output path is empty
    String profileOutputPath = config.getProfileOutputPath();
    if (!profileOutputPath.isEmpty()) {
      Path path = Paths.get(profileOutputPath);
      profiler.writeData(path);
    } else {
      try (Writer writer = new OutputStreamWriter(System.out)) {
        profiler.writeData(writer);
      }
    }
  }


  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("Usage: WebCrawlerMain [starting-url]");
      return;
    }

    CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
    new WebCrawlerMain(config).run();
  }
}
