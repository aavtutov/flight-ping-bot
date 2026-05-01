package com.avtutov.FlightPing.service.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class TimeUtils {

    private final DateTimeFormatter UNIVERSAL_FORMATTER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .optionalStart().appendLiteral('T').optionalEnd()
            .optionalStart().appendLiteral(' ').optionalEnd()
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .optionalStart().appendOffsetId().optionalEnd()
            .toFormatter();

    @Named("toInstant")
    public Instant parseToInstant(String utcStr) {
        if (utcStr == null || utcStr.isBlank()) return null;
        try {
            TemporalAccessor temporal = UNIVERSAL_FORMATTER.parseBest(utcStr.trim(), 
                    Instant::from, OffsetDateTime::from, ZonedDateTime::from);
            
            return Instant.from(temporal);
        } catch (Exception e) {
            return null;
        }
    }

    @Named("toLocalDateTime")
    public LocalDateTime parseToLocal(String localStr) {
        if (localStr == null || localStr.isBlank()) return null;
        try {
            TemporalAccessor temporal = UNIVERSAL_FORMATTER.parseBest(localStr.trim(), 
                    OffsetDateTime::from, LocalDateTime::from, LocalDate::from);

            if (temporal instanceof OffsetDateTime odt) return odt.toLocalDateTime();
            if (temporal instanceof LocalDateTime ldt) return ldt;
            if (temporal instanceof LocalDate ld) return ld.atStartOfDay();
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    @Named("toLocalDate")
    public LocalDate toLocalDate(String localStr) {
        if (localStr == null || localStr.isBlank()) return null;
        try {
            return LocalDate.parse(localStr.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }
}
