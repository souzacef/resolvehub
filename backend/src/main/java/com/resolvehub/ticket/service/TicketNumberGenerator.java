package com.resolvehub.ticket.service;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TicketNumberGenerator {

    private static final long INITIAL_TICKET_NUMBER = 1001L;

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong fallbackCounter = new AtomicLong(-1L);

    public TicketNumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String nextTicketNumber() {
        Long sequenceValue = nextSequenceValue();
        if (sequenceValue != null) {
            return format(sequenceValue);
        }

        return format(nextFallbackValue());
    }

    private Long nextSequenceValue() {
        try {
            return jdbcTemplate.queryForObject("select nextval('ticket_number_seq')", Long.class);
        } catch (DataAccessException exception) {
            return null;
        }
    }

    private long nextFallbackValue() {
        long currentValue = fallbackCounter.get();
        if (currentValue >= INITIAL_TICKET_NUMBER) {
            return fallbackCounter.incrementAndGet();
        }

        synchronized (fallbackCounter) {
            currentValue = fallbackCounter.get();
            if (currentValue < INITIAL_TICKET_NUMBER) {
                long seededValue = Math.max(fetchNextValueFromTable(), INITIAL_TICKET_NUMBER);
                fallbackCounter.set(seededValue);
                return seededValue;
            }

            return fallbackCounter.incrementAndGet();
        }
    }

    private long fetchNextValueFromTable() {
        try {
            Long nextValue = jdbcTemplate.queryForObject(
                    "select coalesce(max(cast(substring(ticket_number, 4) as bigint)), 1000) + 1 from tickets",
                    Long.class
            );
            return nextValue == null ? INITIAL_TICKET_NUMBER : nextValue;
        } catch (DataAccessException exception) {
            return INITIAL_TICKET_NUMBER;
        }
    }

    private String format(long numericValue) {
        return "RH-" + numericValue;
    }
}
