package com.udacity.webcrawler;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.google.inject.multibindings.Multibinder;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.parser.ParserModule;
import com.udacity.webcrawler.profiler.Profiler;
import java.util.logging.Logger;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Guice dependency injection module that installs all the required dependencies to run the web
 * crawler application. Callers should use it like this:
 *
 * <pre>{@code
 *   CrawlerConfiguration config = ...;
 *   WebCrawler crawler =
 *       Guice.createInjector(new WebCrawlerModule(config))
 *           .getInstance(WebCrawler.class);
 * }</pre>
 */
public final class WebCrawlerModule extends AbstractModule {

  private final CrawlerConfiguration config;

  private static final Logger logger = Logger.getLogger(WebCrawlerModule.class.getName());

  public WebCrawlerModule(CrawlerConfiguration config) {
    this.config = Objects.requireNonNull(config);
  }

  @Override
  protected void configure() {
    Multibinder<WebCrawler> multibinder =
        Multibinder.newSetBinder(binder(), WebCrawler.class, Internal.class);
    multibinder.addBinding().to(SequentialWebCrawler.class);
    multibinder.addBinding().to(ParallelWebCrawler.class);

    bind(Clock.class).toInstance(Clock.systemUTC());
    bind(CrawlerConfiguration.class).toInstance(config);  // Binding CrawlerConfiguration
    bind(Key.get(Integer.class, MaxDepth.class)).toInstance(config.getMaxDepth());
    bind(Key.get(Integer.class, PopularWordCount.class)).toInstance(config.getPopularWordCount());
    bind(Key.get(Duration.class, Timeout.class)).toInstance(config.getTimeout());
    bind(new Key<List<Pattern>>(IgnoredUrls.class) {}).toInstance(config.getIgnoredUrls());

    install(
        new ParserModule.Builder()
            .setTimeout(config.getTimeout())
            .setIgnoredWords(config.getIgnoredWords())
            .build());
  }

  @Provides
  @Singleton
  @Internal
  WebCrawler provideRawWebCrawler(
      @Internal Set<WebCrawler> implementations,
      @TargetParallelism int targetParallelism) {
    String override = config.getImplementationOverride();
    if (!override.isEmpty()) {
      return implementations
          .stream()
          .filter(impl -> impl.getClass().getName().equals(override))
          .findFirst()
          .orElseThrow(() -> new ProvisionException("Implementation not found: " + override));
    }
    return implementations
        .stream()
        .filter(impl -> targetParallelism <= impl.getMaxParallelism())
        .findFirst()
        .orElseThrow(
            () -> new ProvisionException(
                "No implementation able to handle parallelism = \"" +
                    config.getParallelism() + "\"."));
  }

  @Provides
  @Singleton
  @TargetParallelism
  int provideTargetParallelism() {
      int parallelism = config.getParallelism();
      logger.info("Parallelism from config: " + parallelism);
      if (parallelism >= 0) {
          return parallelism;
      }
      int processors = Runtime.getRuntime().availableProcessors();
      logger.info("Using available processors: " + processors);
      return processors;  
  }

  @Provides
  @Singleton
  WebCrawler provideWebCrawlerProxy(Profiler wrapper, @Internal WebCrawler delegate) {
    return wrapper.wrap(WebCrawler.class, delegate);
  }

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  private @interface Internal {
  }
}