package com.bms.metrics.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the administrator's metrics screen shows, for the days from
 * {@code from} to {@code to} inclusive. Every per day list has one entry for
 * each of those days, zeros included, in order.
 */
public record MetricsResponse(int days, LocalDate from, LocalDate to,
                              Activity activity, Customers customers, Features features, Traffic traffic) {

    public record DayCount(LocalDate day, long count) {
    }

    public record Split(long owners, long assistants) {
    }

    public record ActiveDay(LocalDate day, long owners, long assistants) {
    }

    /** Active users today, over the last 7 days and over the last 30, whatever the range. */
    public record Activity(List<DayCount> signIns, List<ActiveDay> activeUsers, Split dau, Split wau, Split mau) {
    }

    /** Owners by where their subscription stands today, and how they arrived in the range. */
    public record Customers(long trial, long active, long expired, long suspended,
                            List<DayCount> newOwners, long trialsStarted, long trialsConverted) {
    }

    public record Features(List<DayCount> buildings, List<DayCount> apartments, List<DayCount> tenants,
                           List<DayCount> expenses, List<DayCount> rentInvoices,
                           List<DayCount> coldWaterInvoices, List<DayCount> pdfDownloads) {
    }

    public record TrafficDay(LocalDate day, long requests, long clientErrors, long serverErrors) {
    }

    public record Endpoint(String method, String route, long requests, long clientErrors, long serverErrors,
                           double averageMs, long maxMs) {
    }

    public record Traffic(long requests, long clientErrors, long serverErrors, List<TrafficDay> perDay,
                          List<Endpoint> busiest, List<Endpoint> slowest) {
    }
}
