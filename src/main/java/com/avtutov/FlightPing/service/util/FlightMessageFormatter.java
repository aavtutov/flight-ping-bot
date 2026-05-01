package com.avtutov.FlightPing.service.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.avtutov.FlightPing.model.Flight;
import com.avtutov.FlightPing.model.Subscription;

@Component
public class FlightMessageFormatter {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    public String formatWelcomeMessage(String firstName) {
        return String.format(
            "👋 <b>Hello, %s!</b>\n\nTo subscribe, send flight and date.\nExample: <code>LH155, 29.03</code>",
            firstName
        );
    }

    public String formatSuccessSubscription(Flight flight) {
        LocalDateTime depSched = flight.getDepartureScheduledTimeLocal();
        LocalDateTime depRevis = flight.getDepartureRevisedTimeLocal();

        String departureTimeStr = formatTime(depSched);

        if (depRevis != null && !depRevis.equals(depSched)) {
            long diff = Math.abs(Duration.between(depSched, depRevis).toMinutes());
            if (diff > 5) {
                departureTimeStr = String.format("<s>%s</s> <b>%s</b>", formatTime(depSched), formatTime(depRevis));
            }
        }

        return String.format(
            "🎉 <b>Subscribed!</b>\n\n" +
            "<code>%s - %s (%s)</code>\n\n" +
            "---------------\n\n" +
            "<code>Status:</code> <b>%s</b>\n\n" + 
            "<code>Time:</code> <b>%s</b> \n" +
            "<code>Gate:</code> <b>%s</b>\n" +
            "<code>Terminal:</code> <b>%s</b>\n" +
            "<code>Check-in Desks:</code> <b>%s</b>\n\n" +
            "<code>Aircraft:</code> <b>%s</b>\n" +
            "<code>Arriving time:</code> <b>%s</b>\n\n" +
            "---------------\n\n" +
            "We’ll keep you posted with any changes.",
            getCity(flight.getDepartureAirportCity()),
            getCity(flight.getArrivalAirportCity()),
            formatDate(depSched),
            flight.getStatus() != null ? flight.getStatus().getDescription() : "Unknown",
            departureTimeStr,
            getValueOrTbd(flight.getGate()),
            getValueOrTbd(flight.getTerminal()),
            getValueOrTbd(flight.getCheckInDesk()),
            getValueOrTbd(flight.getAircraftModel()),
            formatTime(flight.getArrivalScheduledTimeLocal())
        );
    }

    public String formatSubscriptionList(List<Subscription> subscriptions) {
        if (subscriptions.isEmpty()) {
            return "📭 <b>You don't have any active subscriptions.</b>\n" +
                   "\nTo subscribe, send me a flight number and date:\n<code>kl1521, 22.04</code>";
        }

        StringBuilder sb = new StringBuilder("<b>Your Active Subscriptions:</b>\n\n");
        for (int i = 0; i < subscriptions.size(); i++) {
            Flight f = subscriptions.get(i).getFlight();
            sb.append(String.format("%d. ✈️ <b>%s ➔ %s</b> (%s %s) \n\n",
                i + 1,
                f.getDepartureAirportIata(),
                f.getArrivalAirportIata(),
                formatDate(f.getDepartureScheduledTimeLocal()),
                formatTime(f.getDepartureScheduledTimeLocal())
            ));
        }
        sb.append("Use /cancel to cancel subscription.");
        return sb.toString();
    }

    public String formatSuggestedFlights(List<Flight> flights) {
        StringBuilder sb = new StringBuilder("We've found a few flights on selected date:\n\n");
        for (int i = 0; i < flights.size(); i++) {
            Flight f = flights.get(i);
            sb.append(String.format("%d. ✈️ <b>%s ➔ %s</b> %s (/subscribe_%d) \n\n",
                i + 1,
                f.getDepartureAirportIata(),
                f.getArrivalAirportIata(),
                formatTime(f.getDepartureScheduledTimeLocal()),
                f.getId()
            ));
        }
        sb.append("Tap <code>/subscribe_...</code> to select one.");
        return sb.toString();
    }

    public String formatFlightUpdate(String flightNumber, String summary) {
        return String.format("📣 <b>Flight to %s:</b>\n\n%s", flightNumber, summary);
    }

    private String formatDate(LocalDateTime ldt) {
        return ldt != null ? ldt.format(DATE_FORMATTER) : "TBD";
    }

    private String formatTime(LocalDateTime ldt) {
        return ldt != null ? ldt.format(TIME_FORMATTER) : "TBD";
    }

    private String getCity(String city) {
        return city != null ? city.toUpperCase() : "N/A";
    }

    private String getValueOrTbd(String value) {
        return (value != null && !value.isBlank()) ? value : "TBD";
    }
}
