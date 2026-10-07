package com.bms.announcement;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import com.bms.announcement.dto.AnnouncementRequest;
import com.bms.announcement.dto.AnnouncementResponse;
import com.bms.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The administrator's messages to every signed in user, and which of them are showing now. */
@Service
public class AnnouncementService {

    private final AnnouncementRepository announcements;
    private final Clock clock;

    public AnnouncementService(AnnouncementRepository announcements, Clock clock) {
        this.announcements = announcements;
        this.clock = clock;
    }

    /** What every signed in user is shown right now. */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> showing() {
        return announcements.findShowingAt(clock.instant()).stream().map(AnnouncementResponse::from).toList();
    }

    /** Every announcement, past, present and scheduled, the latest to start first. */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> list() {
        return announcements.findAllByOrderByStartsAtDesc().stream().map(AnnouncementResponse::from).toList();
    }

    @Transactional
    public AnnouncementResponse create(AnnouncementRequest request) {
        Announcement announcement = new Announcement(request.kind(), request.messageEn(), request.messageFr(),
                request.messageDe(), request.startsAt(), request.endsAt());
        return AnnouncementResponse.from(announcements.saveAndFlush(announcement));
    }

    @Transactional
    public AnnouncementResponse update(UUID id, AnnouncementRequest request) {
        Announcement announcement = require(id);
        announcement.change(request.kind(), request.messageEn(), request.messageFr(), request.messageDe(),
                request.startsAt(), request.endsAt());
        // Flushed so the response carries the new updatedAt, which dismissals key on.
        return AnnouncementResponse.from(announcements.saveAndFlush(announcement));
    }

    @Transactional
    public void delete(UUID id) {
        announcements.delete(require(id));
    }

    private Announcement require(UUID id) {
        return announcements.findById(id).orElseThrow(() -> NotFoundException.of("error.notFound.announcement", id));
    }
}
