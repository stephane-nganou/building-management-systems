package com.bms.announcement;

import java.time.Instant;

import com.bms.common.BaseEntity;
import com.bms.common.exception.ValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A message from the administrator to every signed in user, shown from
 * {@code startsAt} until just before {@code endsAt}. English is required; the
 * French and German texts are optional and fall back to it.
 */
@Entity
@Table(name = "announcement")
public class Announcement extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private AnnouncementKind kind;

    @Column(name = "message_en", nullable = false, length = 1000)
    private String messageEn;

    @Column(name = "message_fr", length = 1000)
    private String messageFr;

    @Column(name = "message_de", length = 1000)
    private String messageDe;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    protected Announcement() {
        // for JPA
    }

    public Announcement(AnnouncementKind kind, String messageEn, String messageFr, String messageDe,
                        Instant startsAt, Instant endsAt) {
        change(kind, messageEn, messageFr, messageDe, startsAt, endsAt);
    }

    public void change(AnnouncementKind kind, String messageEn, String messageFr, String messageDe,
                       Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new ValidationException("error.announcement.range");
        }
        this.kind = kind;
        this.messageEn = messageEn.strip();
        this.messageFr = translation(messageFr);
        this.messageDe = translation(messageDe);
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    /** A translation left blank is no translation: the reader falls back to English. */
    private static String translation(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }

    public AnnouncementKind getKind() {
        return kind;
    }

    public String getMessageEn() {
        return messageEn;
    }

    public String getMessageFr() {
        return messageFr;
    }

    public String getMessageDe() {
        return messageDe;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }
}
