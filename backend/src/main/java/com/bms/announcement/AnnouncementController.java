package com.bms.announcement;

import java.util.List;

import com.bms.announcement.dto.AnnouncementResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * What the administrator is telling everybody right now. Any signed in user
 * may read it. All three texts are sent, so the app can switch language
 * without asking again.
 */
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcements;

    public AnnouncementController(AnnouncementService announcements) {
        this.announcements = announcements;
    }

    @GetMapping
    public List<AnnouncementResponse> showing() {
        return announcements.showing();
    }
}
