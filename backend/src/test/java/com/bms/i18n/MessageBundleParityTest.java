package com.bms.i18n;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every language says everything. A key missing from a bundle would not fail:
 * Spring would quietly answer in English instead, half a page at a time.
 */
class MessageBundleParityTest {

    @Test
    void everyLanguageHasExactlyTheEnglishKeys() throws Exception {
        Path english = Path.of(getClass().getResource("/messages.properties").toURI());
        List<Path> languages;
        try (Stream<Path> files = Files.list(english.getParent())) {
            languages = files.filter(file -> file.getFileName().toString().matches("messages_\\w+\\.properties"))
                    .toList();
        }

        assertThat(languages).extracting(file -> file.getFileName().toString())
                .contains("messages_fr.properties", "messages_de.properties");
        for (Path language : languages) {
            assertThat(keys(language)).as(language.getFileName().toString())
                    .containsExactlyInAnyOrderElementsOf(keys(english));
        }
    }

    private static List<String> keys(Path bundle) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(bundle, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties.stringPropertyNames().stream().toList();
    }
}
