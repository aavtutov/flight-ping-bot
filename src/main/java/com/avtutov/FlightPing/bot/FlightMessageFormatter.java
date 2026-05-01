package com.avtutov.FlightPing.bot;

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
    
    public final String ALREADY_SUBSCRIBED = "⚠️ You are already subscribed to this flight.";
    
    public final String NO_ACTIVE_SUBS = "📭 <b>You don't have any active subscriptions.</b>\n" +
            "\nTo subscribe for flight updates, send me the flight number and date."
            + "\n\nExample:\n\n"
            + "<code>ba435, 29/04</code>";
    
    public final String CANCEL_FORMAT = "⚠️ Please, specify number of subscription or cancel all of them at once.\n\n" +
            "<code>/cancel 1</code>\nor\n<code>/cancel all</code>\n\n";
    
    public final String FLIGHT_NOT_FOUND = "🚫 Flight not found in system for this date.";
    
    public final String WRONG_FLIGHT_DATA = "⚠️ I don't recognize this format.\nTry: <code>ba435, 29/04</code>";
    
    public final String ALL_SUBS_CANCELLED = "✅ All subscriptions were cancelled.";
    
    public final String API_LIMIT = "⚠️ API requests limit. Try again later.";
    
    public final String INVALID_FLIGHT_SELECTION = "⚠️ Invalid flight selection.";
    
    public final String SUB_CREATEING_ERROR = "⚠️ API requests limit. Try again later.";
    
    public final String WRONG_NUMBER_TO_CANCEL = "⚠️ Wrong number.\n"
    		+ "Use /list to see current list of active subscriptions.";

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
            if (diff >= 5) {
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
    
    public String formatUpdatedFlightSummary(String arrivalCity, String summary, Flight flight) {
    	String updatedFlight = formatUpdatedFlight(flight);
        return String.format("📣 <b>Flight to %s:</b>\n\n%s\n\n%s", arrivalCity, summary, updatedFlight);
    }
    
    public String formatUpdatedFlight(Flight flight) {
        LocalDateTime depSched = flight.getDepartureScheduledTimeLocal();
        LocalDateTime depRevis = flight.getDepartureRevisedTimeLocal();

        String departureTimeStr = formatDepartureTime(depSched, depRevis);

        return String.format(
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
            return NO_ACTIVE_SUBS;
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
    
    public String formatLimitReached(int max) {
        return String.format("🚫 <b>Limit reached!</b>\n"
        		+ "You can only have %d active subscriptions at a time.\n"
        		+ "Use /list to manage them.", max);
    }
    
    public String formatSubscriptionCancelled(String city) {
        return String.format(
        		"✅ Subscription to <b>%s</b> flight  cancelled.\n"
        		+ "Use /list to see your active subscriptions.", city);
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
    
    private String formatDepartureTime(LocalDateTime depScheduled, LocalDateTime depRevised) {
    	
    	if (depRevised != null && !depRevised.equals(depScheduled)) {
            long diff = Math.abs(Duration.between(depScheduled, depRevised).toMinutes());
            if (diff >= 5) {
                return String.format("<s>%s</s> <b>%s</b>", formatTime(depScheduled), formatTime(depRevised));
            }
        }
    	return formatTime(depScheduled);
    }
}
