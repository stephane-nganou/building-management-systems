package com.bms.common.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Reads the wording out of `messages.properties` in the language the caller
 * asked for. The `Accept-Language` header on the request is what decides, and
 * Spring has already narrowed it to one we support by the time this runs.
 */
@Component
public class Messages {

    private final MessageSource messageSource;

    public Messages(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String get(String code, Object... args) {
        return get(LocaleContextHolder.getLocale(), code, args);
    }

    public String get(Locale locale, String code, Object... args) {
        return messageSource.getMessage(code, args, locale);
    }
}
