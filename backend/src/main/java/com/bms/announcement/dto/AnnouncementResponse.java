package com.bms.announcement.dto;

import java.time.Instant;
import java.util.UUID;

import com.bms.announcement.Announcement;
import com.bms.announcement.AnnouncementKind;

/**
 * Everything about an announcement. {@code updatedAt} changes with every edit,
 * which is how a browser that dismissed one knows to show it again.
 */
public record AnnouncementResponse(
        UUID id,
        AnnouncementKind kind,
        String messageEn,
        String messageFr,
        String messageDe,
        Instant startsAt,
        Instant endsAt,
        Instant updatedAt) {

    public static AnnouncementResponse from(Announcement announcement) {
        return new AnnouncementResponse(announcement.getId(), announcement.getKind(), announcement.getMessageEn(),
                announcement.getMessageFr(), announcement.getMessageDe(), announcement.getStartsAt(),
                announcement.getEndsAt(), announcement.getUpdatedAt());
    }
}
