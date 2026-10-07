package com.bms.announcement.dto;

import java.time.Instant;

import com.bms.announcement.AnnouncementKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Shown from {@code startsAt} until just before {@code endsAt}; a blank French or German text falls back to English. */
public record AnnouncementRequest(
        @NotNull AnnouncementKind kind,
        @NotBlank @Size(max = 1000) String messageEn,
        @Size(max = 1000) String messageFr,
        @Size(max = 1000) String messageDe,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt) {
}
