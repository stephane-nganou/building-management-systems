package com.bms.config;

import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * The language a request is answered in, taken from its `Accept-Language`
 * header. The list is closed on purpose: a header asking for anything else
 * falls back to English rather than to the server's own default, which would
 * otherwise make the answer depend on where the container happens to run.
 */
@Configuration
public class LocaleConfig {

    /** The languages the application speaks, which the sign in page is offered in too. */
    static final List<Locale> SUPPORTED = List.of(Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN);

    @Bean
    LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(SUPPORTED);
        resolver.setDefaultLocale(Locale.ENGLISH);
        return resolver;
    }
}
