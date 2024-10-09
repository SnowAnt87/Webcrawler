package com.udacity.webcrawler;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.truth.Truth.assertWithMessage;

public final class WordCountsTest {

    @Test
    public void testBasicOrder() {
        // Create a map with AtomicInteger values
        Map<String, AtomicInteger> unsortedCounts = new HashMap<>();
        unsortedCounts.put("the", new AtomicInteger(2));
        unsortedCounts.put("quick", new AtomicInteger(1));
        unsortedCounts.put("brown", new AtomicInteger(1));
        unsortedCounts.put("fox", new AtomicInteger(1));
        unsortedCounts.put("jumped", new AtomicInteger(1));
        unsortedCounts.put("over", new AtomicInteger(1));
        unsortedCounts.put("lazy", new AtomicInteger(1));
        unsortedCounts.put("dog", new AtomicInteger(1));

        // Sort using the modified WordCounts.sort() that handles AtomicInteger
        Map<String, AtomicInteger> result = WordCounts.sort(unsortedCounts, 4);

        assertWithMessage("Returned the wrong number of popular words")
            .that(result)
            .hasSize(4);

        assertWithMessage("Returned the correct number of popular words, but the wrong words or counts")
            .that(result)
            .containsEntry("the", new AtomicInteger(2));
        assertWithMessage("Returned the correct number of popular words, but the wrong words or counts")
            .that(result)
            .containsEntry("jumped", new AtomicInteger(1));
        assertWithMessage("Returned the correct number of popular words, but the wrong words or counts")
            .that(result)
            .containsEntry("brown", new AtomicInteger(1));
        assertWithMessage("Returned the correct number of popular words, but the wrong words or counts")
            .that(result)
            .containsEntry("quick", new AtomicInteger(1));

        // Convert to Map.Entry and check the order
        assertWithMessage("Returned the correct words, but they are in the wrong order")
            .that(result.entrySet())
            .containsExactly(
                Map.entry("the", new AtomicInteger(2)),
                Map.entry("jumped", new AtomicInteger(1)),
                Map.entry("brown", new AtomicInteger(1)),
                Map.entry("quick", new AtomicInteger(1))
            )
            .inOrder();
    }

    @Test
    public void testNotEnoughWords() {
        // Create a map with AtomicInteger values
        Map<String, AtomicInteger> unsortedCounts = new HashMap<>();
        unsortedCounts.put("the", new AtomicInteger(2));
        unsortedCounts.put("quick", new AtomicInteger(1));
        unsortedCounts.put("brown", new AtomicInteger(1));
        unsortedCounts.put("fox", new AtomicInteger(1));

        // Sort using the modified WordCounts.sort() that handles AtomicInteger
        Map<String, AtomicInteger> result = WordCounts.sort(unsortedCounts, 5);

        assertWithMessage("Returned the wrong number of popular words")
            .that(result)
            .hasSize(4);

        assertWithMessage("Returned the correct number of popular words, but the wrong words or counts")
            .that(result)
            .containsEntry("the", new AtomicInteger(2));
        assertWithMessage("Returned the correct number of popular words, but the wrong words or counts")
            .that(result)
            .containsEntry("brown", new AtomicInteger(1));
        assertWithMessage("Returned the correct number of popular words, but the wrong words or counts")
            .that(result)
            .containsEntry("quick", new AtomicInteger(1));
        assertWithMessage("Returned the correct number of popular words, but the wrong words or counts")
            .that(result)
            .containsEntry("fox", new AtomicInteger(1));

        // Convert to Map.Entry and check the order
        assertWithMessage("Returned the correct words, but they are in the wrong order")
            .that(result.entrySet())
            .containsExactly(
                Map.entry("the", new AtomicInteger(2)),
                Map.entry("brown", new AtomicInteger(1)),
                Map.entry("quick", new AtomicInteger(1)),
                Map.entry("fox", new AtomicInteger(1))
            )
            .inOrder();
    }
}
