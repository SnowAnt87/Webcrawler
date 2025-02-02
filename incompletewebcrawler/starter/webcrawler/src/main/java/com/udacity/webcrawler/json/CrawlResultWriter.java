package com.udacity.webcrawler.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Utility class to write a {@link CrawlResult} to file.
 */
public final class CrawlResultWriter {
    private final CrawlResult result;

    /**
     * Creates a new {@link CrawlResultWriter} that will write the given {@link CrawlResult}.
     */
    public CrawlResultWriter(CrawlResult result) {
        this.result = Objects.requireNonNull(result);
    }

    /**
     * Formats the {@link CrawlResult} as JSON and writes it to the given {@link Path}.
     *
     * <p>If a file already exists at the path, the existing file should not be deleted; new data
     * should be appended to it.
     *
     * @param path the file path where the crawl result data should be written.
     */
    public void write(Path path) {
        Objects.requireNonNull(path);
        ObjectMapper objectMapper = new ObjectMapper().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path)) {
            write(bufferedWriter);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write crawl result to file", e);
        }
    }

    /**
     * Formats the {@link CrawlResult} as JSON and writes it to the given {@link Writer}.
     *
     * <p> Note: The caller is responsible for managing the lifecycle of the {@link Writer}.
     * This method does not close the writer.
     *
     * @param writer the destination where the crawl result data should be written.
     */
     public void write(Writer writer) {
         Objects.requireNonNull(writer);
         ObjectMapper objectMapper = new ObjectMapper().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

         try {
             objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, result);
         } catch (IOException e) {
             throw new RuntimeException("Failed to write crawl result to writer", e);
         }
     }

}
