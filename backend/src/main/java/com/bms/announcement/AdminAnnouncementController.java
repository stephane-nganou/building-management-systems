package com.bms.announcement;

import java.util.List;
import java.util.UUID;

import com.bms.announcement.dto.AnnouncementRequest;
import com.bms.announcement.dto.AnnouncementResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Administrators only; {@code SecurityConfig} refuses everyone else the whole of /api/admin. */
@RestController
@RequestMapping("/api/admin/announcements")
public class AdminAnnouncementController {

    private final AnnouncementService announcements;

    public AdminAnnouncementController(AnnouncementService announcements) {
        this.announcements = announcements;
    }

    @GetMapping
    public List<AnnouncementResponse> list() {
        return announcements.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementResponse create(@Valid @RequestBody AnnouncementRequest request) {
        return announcements.create(request);
    }

    @PutMapping("/{id}")
    public AnnouncementResponse update(@PathVariable UUID id, @Valid @RequestBody AnnouncementRequest request) {
        return announcements.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        announcements.delete(id);
    }
}
