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
      // Inject dependencies
      Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

      // Crawl and write results
      CrawlResult result = crawler.crawl(config.getStartPages());
      CrawlResultWriter resultWriter = new CrawlResultWriter(result);
      if (config.getResultPath() != null && !config.getResultPath().isEmpty()) {
          // Write the result to the specified file path
          resultWriter.write(Path.of(config.getResultPath()));
      } else {
          // Write the result to System.out
          Writer writer = new OutputStreamWriter(System.out);
          resultWriter.write(writer);
      }

      // Write profile data
      if (config.getProfileOutputPath() != null && !config.getProfileOutputPath().isEmpty()) {
          // Write profile data to the specified file path
          profiler.writeData(Path.of(config.getProfileOutputPath()));
      } else {
          // Write profile data to System.out
          Writer writer = new OutputStreamWriter(System.out);
        profiler.writeData(writer);
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
