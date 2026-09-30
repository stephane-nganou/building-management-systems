package com.bms.admin.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Both days are included. */
public record PeriodRequest(
        @NotNull LocalDate startsOn,
        @NotNull LocalDate endsOn,
        @Size(max = 500) String note) {
}
