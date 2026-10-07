package com.bms.announcement;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    List<Announcement> findAllByOrderByStartsAtDesc();

    /** Started at or before {@code now}, and not yet ended: an announcement ends just before {@code endsAt}. */
    @Query("""
            select a from Announcement a
            where a.startsAt <= :now and a.endsAt > :now
            order by a.startsAt""")
    List<Announcement> findShowingAt(Instant now);
}
