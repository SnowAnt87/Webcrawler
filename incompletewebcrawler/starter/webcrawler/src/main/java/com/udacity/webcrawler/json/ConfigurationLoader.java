package com.udacity.webcrawler.json;

import java.io.Reader;
import java.nio.file.Path;
import java.util.Objects;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser.Feature;


/**
 * A static utility class that loads a JSON configuration file.
 */
public final class ConfigurationLoader {

  private final Path path;

  /**
   * Create a {@link ConfigurationLoader} that loads configuration from the given {@link Path}.
   */
  public ConfigurationLoader(Path path) {
    this.path = Objects.requireNonNull(path);
  }

  /**
   * Loads configuration from this {@link ConfigurationLoader}'s path
   *
   * @return the loaded {@link CrawlerConfiguration}.
   */
   public CrawlerConfiguration load() {
       Objects.requireNonNull(path, "Path must not be null");
    
       try (Reader reader = java.nio.file.Files.newBufferedReader(path)) {
           return read(reader);
       } catch (IOException e) {
           throw new RuntimeException("Failed to load configuration from path: " + path, e);
       }
   }


  /**
   * Loads crawler configuration from the given reader.
   *
   * @param reader a Reader pointing to a JSON string that contains crawler configuration.
   * @return a crawler configuration
   */
  
   public static CrawlerConfiguration read(Reader reader) {
       // Ensure that the reader is not null to avoid null pointer exceptions
       Objects.requireNonNull(reader);

       try {
           ObjectMapper objectMapper = new ObjectMapper();
           // Disable the AUTO_CLOSE_SOURCE feature to prevent the reader from being automatically closed
           objectMapper.configure(Feature.AUTO_CLOSE_SOURCE, false);

           return objectMapper.readValue(reader, CrawlerConfiguration.Builder.class).build();
       } catch (IOException e) {
           throw new RuntimeException("Failed to read configuration", e);
       }
   }

}
