package com.bms.metrics;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.bms.user.AppUser;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Notes who used the service on which day, and each browser sign in.
 *
 * <p>A session outlives a sign in by days, so "active" means any request that
 * day, not a sign in. That would be a write per request; remembering who has
 * already been seen today makes it one write per user and day.
 */
@Component
public class ActivityRecorder {

    private final JdbcClient jdbc;
    private final Clock clock;
    private final Map<UUID, LocalDate> seen = new ConcurrentHashMap<>();

    public ActivityRecorder(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public void touch(AppUser user) {
        LocalDate today = Metrics.today(clock);
        if (today.equals(seen.put(user.getId(), today))) {
            return;
        }
        jdbc.sql("""
                        insert into user_activity (user_id, day, role) values (:user, :day, :role)
                        on conflict do nothing""")
                .param("user", user.getId())
                .param("day", today)
                .param("role", user.getRole().name())
                .update();
    }

    public void signedIn(AppUser user) {
        LocalDate today = Metrics.today(clock);
        seen.put(user.getId(), today);
        jdbc.sql("""
                        insert into user_activity (user_id, day, role, sign_ins) values (:user, :day, :role, 1)
                        on conflict (user_id, day) do update set sign_ins = user_activity.sign_ins + 1""")
                .param("user", user.getId())
                .param("day", today)
                .param("role", user.getRole().name())
                .update();
    }
}
