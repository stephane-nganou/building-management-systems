package com.bms.user;

import com.bms.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Accounts are looked up by email, ignoring case, so two rows sharing one would
 * make every such lookup fail. The database refuses the second row.
 */
class AppUserEmailIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AppUserRepository users;

    @Test
    void anEmailDifferingOnlyInCaseIsRefused() {
        users.saveAndFlush(new AppUser("kc-first", "same@example.com", "First", "One", false));

        assertThatThrownBy(() -> users.saveAndFlush(
                new AppUser("kc-second", "SAME@example.com", "Second", "Two", false)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
