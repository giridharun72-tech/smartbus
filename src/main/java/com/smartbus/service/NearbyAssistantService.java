package com.smartbus.service;

import com.smartbus.dto.NearbyAssistantRequest;
import com.smartbus.dto.NearbyAssistantResponse;
import com.smartbus.dto.NearbyBusOption;
import com.smartbus.dto.NearbyStopResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
public class NearbyAssistantService {
    private static final Logger logger = LoggerFactory.getLogger(NearbyAssistantService.class);
    private static final Pattern YEAR = Pattern.compile("\\b20\\d{2}\\b");
    private static final Pattern CHIEF_MINISTER_ABBREVIATION =
            Pattern.compile("(?i)^\\s*(?:who is\\s+)?(?:the\\s+)?cm\\s+of\\s+([^?]+?)\\??\\s*$");
    private static final List<String> LIVE_SEARCH_PHRASES = List.of(
            "current", "currently", "latest", "recent", "right now", "as of",
            "today", "yesterday", "this week", "this month", "this year", "now",
            "live", "breaking news", "news today", "weather", "forecast", "stock price",
            "exchange rate", "current affairs", "trending", "election result", "election results",
            "who is cm", "who is the cm", "cm of", "chief minister", "prime minister",
            "president of", "governor of", "mayor of", "who is the president", "who leads",
            "which party governs", "ruling party", "which party is in power");
    private static final Set<String> GENERAL_WORDS = Set.of(
            "a", "an", "and", "are", "best", "bus", "buses", "can", "find", "for", "get",
            "go", "i", "me", "my", "near", "nearby", "please", "recommend", "show", "take",
            "the", "there", "to", "want", "which", "with", "what", "where", "trip", "option",
            "fastest", "fast", "quick", "route", "time", "today", "available"
    );

    private final SearchService searchService;
    private final OllamaChatService ollamaChatService;
    private final OllamaWebSearchService ollamaWebSearchService;
    private final AssistantSafetyGuard safetyGuard;

    public NearbyAssistantService(SearchService searchService,
                                  OllamaChatService ollamaChatService,
                                  OllamaWebSearchService ollamaWebSearchService,
                                  AssistantSafetyGuard safetyGuard) {
        this.searchService = searchService;
        this.ollamaChatService = ollamaChatService;
        this.ollamaWebSearchService = ollamaWebSearchService;
        this.safetyGuard = safetyGuard;
    }

    public NearbyAssistantResponse reply(NearbyAssistantRequest request, String role) {
        if (safetyGuard.isUnsafeRequest(request.getMessage())) {
            return new NearbyAssistantResponse(safetyGuard.refusal(), List.of());
        }

        String predefinedReply = predefinedReply(request.getMessage(), role);
        if (predefinedReply != null) {
            return new NearbyAssistantResponse(predefinedReply, List.of());
        }

        if (!isTripQuestion(request.getMessage())) {
            if (needsLiveSearch(request.getMessage())) {
                String liveQuestion = expandChiefMinisterAbbreviation(request.getMessage());
                try {
                    List<OllamaWebSearchService.SearchResult> sources =
                            ollamaWebSearchService.search(liveQuestion);
                    String answer = ollamaChatService.answer(liveQuestion, role, sources);
                    return new NearbyAssistantResponse(answer, List.of(),
                            sources.stream().map(OllamaWebSearchService.SearchResult::source).toList());
                } catch (ResponseStatusException exception) {
                    logger.warn("Live search unavailable; using the local model without current-source verification: {}",
                            exception.getReason());
                    String answer = ollamaChatService.answerWithoutLiveSearch(liveQuestion, role);
                    return new NearbyAssistantResponse(answer, List.of());
                }
            }
            return new NearbyAssistantResponse(ollamaChatService.answer(request.getMessage(), role), List.of());
        }

        List<NearbyStopResult> stops = searchService.findNearbyStops(
                request.getLatitude(), request.getLongitude(), 10.0);
        List<NearbyAssistantResponse.TripSuggestion> allTrips = stops.stream()
                .flatMap(stop -> stop.getBusOptions().stream()
                        .map(bus -> toSuggestion(stop, bus)))
                .filter(suggestion -> !"CANCELLED".equalsIgnoreCase(suggestion.status()))
                .sorted(Comparator.comparingInt(NearbyAssistantResponse.TripSuggestion::totalEtaMinutes)
                        .thenComparingInt(NearbyAssistantResponse.TripSuggestion::walkingMinutes))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                suggestion -> suggestion.busId() + "|" + suggestion.departureTime(),
                                suggestion -> suggestion,
                                (first, ignored) -> first,
                                LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())));

        Set<String> terms = Arrays.stream(request.getMessage().toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9 ]", " ")
                        .split("\\s+"))
                .filter(term -> term.length() > 1 && !GENERAL_WORDS.contains(term))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<NearbyAssistantResponse.TripSuggestion> matches = terms.isEmpty()
                ? allTrips
                : allTrips.stream()
                        .filter(trip -> matchScore(trip, terms) > 0)
                        .sorted(Comparator.comparingInt(
                                        (NearbyAssistantResponse.TripSuggestion trip) -> matchScore(trip, terms))
                                .reversed()
                                .thenComparingInt(NearbyAssistantResponse.TripSuggestion::totalEtaMinutes))
                        .toList();
        List<NearbyAssistantResponse.TripSuggestion> suggestions = matches.stream().limit(3).toList();

        if (suggestions.isEmpty()) {
            String destinations = allTrips.stream()
                    .map(NearbyAssistantResponse.TripSuggestion::destination)
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(5)
                    .collect(Collectors.joining(", "));
            String hint = destinations.isBlank()
                    ? "I couldn't find active scheduled buses at nearby stops right now."
                    : "Nearby services currently show destinations such as " + destinations + ".";
            return new NearbyAssistantResponse(
                    hint + " Try a destination, route name, bus number, or bus type. For road closures or other help, message the BCU/Admin team here.",
                    List.of());
        }

        NearbyAssistantResponse.TripSuggestion best = suggestions.get(0);
        String reply = "Best live-data match: Bus " + best.busNumber() + " toward " + best.destination()
                + " from " + best.stopName() + " (" + best.distanceDisplay() + " away, about "
                + best.walkingMinutes() + " min walk). Its scheduled departure is " + best.departureTime()
                + "; estimated total wait and walk is about " + best.totalEtaMinutes() + " min. "
                + statusSummary(best)
                + (suggestions.size() > 1 ? " I’ve included " + (suggestions.size() - 1) + " other option(s) to compare." : "")
                + " These trip suggestions use SmartBus route data. Ask a general question for a local model answer, or message BCU/Admin for help.";
        return new NearbyAssistantResponse(reply, suggestions);
    }

    private String expandChiefMinisterAbbreviation(String question) {
        var matcher = CHIEF_MINISTER_ABBREVIATION.matcher(question);
        if (!matcher.matches()) {
            return question;
        }
        return "Who is the chief minister of " + matcher.group(1).strip() + "?";
    }

    private boolean isTripQuestion(String message) {
        String normalized = message.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return containsAny(normalized, "nearby bus", "nearby buses", "buses nearby", "nearest bus",
                "find bus", "find a bus", "find buses", "bus to", "buses to", "bus route",
                "bus number", "bus type", "express bus", "bus stop", "bus stops",
                "bus schedule", "bus timetable", "bus timings", "bus from", "bus service",
                "trip by bus", "track my bus", "track the bus", "buses near me");
    }

    private boolean needsLiveSearch(String message) {
        String normalized = message.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        String padded = " " + normalized + " ";
        return YEAR.matcher(normalized).find()
                || LIVE_SEARCH_PHRASES.stream().anyMatch(phrase -> padded.contains(" " + phrase + " "));
    }

    private String predefinedReply(String message, String role) {
        String normalized = message.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.matches("^(hi|hello|hey|good morning|good afternoon|good evening|howdy)$")) {
            return switch (role) {
                case "ADMIN" -> "Hello, Admin! I can explain fleet management, routes, bookings, revenue reports, and account help. I provide guidance only and do not read or change private records.";
                case "BCU" -> "Hello, BCU controller! I can explain fleet monitoring, GPS signal loss, route diversions, incidents, and passenger support.";
                case "DRIVER" -> "Hello, Driver! I can explain location sharing, ticket verification, manual location updates, and reporting an incident.";
                case "USER" -> "Hello! I can help with public bus routes, nearby trips, ticket booking, tracking, and passenger support.";
                default -> "Hello! I can help with public bus routes, nearby trips, ticket booking steps, and tracking. Sign in for passenger services.";
            };
        }

        if (matchesAny(normalized, "thank you", "thanks", "thx", "thanks a lot", "thank you so much")) {
            return "You’re welcome! Ask a question about the services available to your account.";
        }

        if (containsAny(normalized, "owner of neuronex", "owners of neuronex",
                "ownership of neuronex", "who owns neuronex", "neuronex owner", "neuronex ownership")) {
            return "Neuronex is owned by Giridharun, Kavin, and Kaniskhar. The three of us are an SIH team and winners of the 2026 ESEC hackathon in the Hardware Track. We are students in the Electronics and Communication Engineering (ECE) department at MKCE.";
        }

        if (containsAny(normalized, "who are you", "what are you", "are you ai", "is this ai", "are you a bot")) {
            return "I’m Neuronex, SmartBus’s AI assistant powered by a locally hosted large language model. I can answer general questions, provide role-specific guidance, and search public trip information. I have no access to private passenger, driver, financial, or operational records.";
        }

        if (matchesAny(normalized, "help", "what can you do", "what do you do",
                "how can you help", "your features")) {
            return helpReply(role);
        }

        if (containsAny(normalized, "total collection", "revenue report", "revenue reports",
                "view revenue", "check revenue", "earnings report", "income report",
                "collection report", "collection reports")) {
            return switch (role) {
                case "ADMIN" -> "Revenue and collection reports are available from the Admin dashboard under Earnings & Charts or Reports. This assistant does not disclose account totals or financial records.";
                case "BCU" -> "Financial collection reports are restricted to Admin. For BCU operations, use the control dashboard, fleet map, and incident tools.";
                case "DRIVER" -> "Financial collection reports are not available in the Driver portal. Use the driver console for your assigned bus, location sharing, ticket verification, and incident reporting.";
                default -> "Financial collection reports are staff-only. I can help you find public routes, fares shown for a trip, and passenger booking steps.";
            };
        }

        if (containsAny(normalized, "bus emergency", "driver emergency", "bus breakdown",
                "bus delay", "report a breakdown", "report breakdown", "report an incident",
                "report incident", "report a delay", "report delay", "report an emergency",
                "report emergency", "road block", "roadblock", "route diversion",
                "service disruption", "bus cancellation", "bus cancelled", "bus canceled",
                "bus stuck", "operational incident")) {
            return switch (role) {
                case "ADMIN" -> "For an operational incident or route diversion, review the BCU dashboard and incident tools. Admin fleet and route management pages are available from the Admin dashboard. I provide instructions but do not change operations.";
                case "BCU" -> "Review the BCU Incidents page and Signal Monitor. Use Bus & Driver Setup to apply an approved route change or replacement bus, then communicate service updates to passengers.";
                case "DRIVER" -> "Use the incident buttons in your Driver Console to report a delay, breakdown, or emergency to BCU. For a road closure or route change, contact BCU; only BCU can authorize route changes or manual location mode.";
                case "USER" -> "For a road block, delay, cancellation, or safety issue, contact the BCU/Admin team through the passenger support chat on the Nearby page. Check trip status before boarding.";
                default -> "For a service issue, check the public bus status or sign in as a passenger to contact BCU/Admin through support chat. For an emergency, contact local emergency services.";
            };
        }

        if (containsAny(normalized, "book a ticket", "book ticket", "booking a ticket",
                "ticket booking", "book my ticket", "bus ticket", "reserve a seat",
                "reserve seats", "seat reservation", "bus reservation", "verify a ticket",
                "verify ticket", "ticket verification", "check a ticket", "check ticket",
                "scan a ticket", "scan ticket", "ticket qr", "qr code ticket",
                "ticket number", "my ticket", "my booking", "my reservation", "passenger id")) {
            return switch (role) {
                case "ADMIN" -> "Use Admin Bookings to review booking records. For passenger boarding checks, authorized staff should use the BCU verification tools. I cannot reveal private passenger or ticket records.";
                case "BCU" -> "Use Passenger Verification in the BCU portal to verify a passenger ID or ticket and review authorized travel information. This assistant does not retrieve passenger records.";
                case "DRIVER" -> "Open Verify Passenger Tickets in your Driver Console, then scan the QR code or enter the ticket number. Check its validity before marking boarding as used; do not rely on this assistant to validate a ticket.";
                case "USER" -> "To book, sign in as a passenger, search your route, choose an available bus and date, select seats, then confirm. Your ticket is available from your account after booking.";
                default -> "You can search public trips as a guest. Sign in or register as a passenger to book a ticket and view your tickets.";
            };
        }

        if (containsAny(normalized, "location sharing", "share location", "share my location",
                "manual location", "signal lost", "signal loss", "gps signal", "bus gps",
                "driver gps", "driver location", "bus location", "where is my bus",
                "where is the bus", "allow location access", "enable location",
                "track my bus", "bus tracking")) {
            return switch (role) {
                case "ADMIN" -> "Use Admin Phone GPS to share a selected bus location. For fleet-wide signal health, open the BCU Signal Monitor. Location updates and vehicle assignments are managed in the authorized dashboards.";
                case "BCU" -> "Use the Signal Monitor to check GPS health. If a driver loses signal, review the last known location and authorize Manual Location Mode when appropriate; the driver can then submit a manual fix.";
                case "DRIVER" -> "Open your Driver Console and choose Enable Location to share your phone GPS for your assigned bus. If GPS is unavailable, contact BCU; manual coordinates are accepted only after BCU enables Manual Location Mode.";
                case "USER" -> "Use the Nearby map to find stops, or open a bus tracking page to see its latest reported location. Allow location access if you want to search near your current position.";
                default -> "Open Buses Near Me to find nearby stops. You can allow browser location access for nearby results, or search from the default map area.";
            };
        }

        if (containsAny(normalized, "bus route", "bus routes", "route for my bus",
                "route change", "change the route", "change a route", "bus schedule",
                "bus timetable", "bus time table", "bus timings", "driver details",
                "bus details", "bus fleet", "fleet management", "bus setup",
                "replacement bus")) {
            return switch (role) {
                case "ADMIN" -> "Use Bus Fleet, route, schedule, and driver management pages from the Admin dashboard. Changes must be made there by authorized staff; this assistant only explains where to go.";
                case "BCU" -> "Use Bus & Driver Setup to manage buses, driver assignments, route stops, and service changes. Use the BCU dashboard and Fleet Map to monitor operations. This assistant does not edit records.";
                case "DRIVER" -> "Your assigned bus and route are shown in the Driver Console. Contact BCU to request a route, schedule, driver-detail, or replacement-bus change.";
                case "USER" -> "Enter your origin and destination in Search Buses to see available routes and schedules. Ask for a destination or bus type to get nearby trip suggestions.";
                default -> "Search public routes and schedules from Search Buses, or ask me for nearby services to a destination.";
            };
        }

        if (containsAny(normalized, "bus fare", "ticket fare", "fare for my trip",
                "fare for the bus", "bus ticket price", "ticket price", "ticket cost",
                "booking cost", "fare amount", "pay for my ticket", "pay for booking",
                "payment for my booking", "how much is the fare")) {
            return switch (role) {
                case "USER", "GUEST" -> "Search for a route and select a trip to see its fare before booking. The assistant does not estimate fares or collect payments.";
                case "DRIVER" -> "For a passenger fare question, refer to the ticket details or contact BCU. Do not collect or confirm a payment based on this assistant’s reply.";
                case "BCU", "ADMIN" -> "Use the authorized booking and reporting pages for fare or payment records. This assistant does not read financial or passenger records.";
                default -> null;
            };
        }

        if (containsAny(normalized, "log in", "sign in", "login page", "login help",
                "account login", "register for smartbus", "create an account",
                "sign up for smartbus", "forgot password", "password reset", "reset password")) {
            return switch (role) {
                case "DRIVER" -> "Use the Driver Login page with your assigned driver credentials. If access fails or your assignment is incorrect, contact BCU or Admin; never share your password in chat.";
                case "BCU", "ADMIN" -> "Sign in with your staff account to access your authorized dashboard. Contact the system administrator if access or role assignment is incorrect; never share credentials in chat.";
                case "USER" -> "Use Sign in for your passenger account, or Register to create one. Never share your password in chat.";
                default -> "Choose Sign in for an existing account or Register to create a passenger account. Never share your password in chat.";
            };
        }

        return null;
    }

    private String helpReply(String role) {
        return switch (role) {
            case "ADMIN" -> "I can explain where Admin manages buses, drivers, routes, schedules, bookings, and revenue reports. I provide guidance only; use the Admin dashboard to view or change records.";
            case "BCU" -> "I can explain fleet monitoring, GPS signal loss, manual-location authorization, incidents, passenger verification, route diversions, and support chat. I do not read or change live records.";
            case "DRIVER" -> "I can explain how to share your assigned bus location, verify a ticket, submit an authorized manual location, and report a delay, breakdown, or emergency.";
            case "USER" -> "I can help find public nearby trips, explain booking and tracking, answer fare and account questions, and tell you how to contact BCU/Admin support.";
            default -> "I can help find public bus trips and explain passenger booking, tracking, and account basics. Sign in to access passenger services.";
        };
    }

    private boolean containsAny(String message, String... phrases) {
        return Arrays.stream(phrases).anyMatch(message::contains);
    }

    private boolean matchesAny(String message, String... phrases) {
        return Arrays.stream(phrases).anyMatch(message::equals);
    }

    private NearbyAssistantResponse.TripSuggestion toSuggestion(NearbyStopResult stop, NearbyBusOption bus) {
        return new NearbyAssistantResponse.TripSuggestion(
                bus.getBusId(), bus.getBusNumber(), bus.getRouteName(), bus.getDestination(),
                bus.getBusType(), bus.getStatus(), bus.getIssue(), stop.getStopName(), stop.getDistanceDisplay(),
                stop.getWalkingMinutes(), bus.getDepartureTime(), bus.getBusEtaMinutes(),
                bus.getTotalEtaMinutes(), bus.isBookable());
    }

    private int matchScore(NearbyAssistantResponse.TripSuggestion trip, Set<String> terms) {
        String searchable = String.join(" ",
                safe(trip.destination()), safe(trip.routeName()), safe(trip.busNumber()),
                safe(trip.stopName()), safe(trip.busType()), safe(trip.status())).toLowerCase(Locale.ROOT);
        return (int) terms.stream().filter(searchable::contains).count();
    }

    private String statusSummary(NearbyAssistantResponse.TripSuggestion trip) {
        if (trip.issue() != null && !trip.issue().isBlank()) {
            return "SmartBus reports an active service issue: " + trip.issue() + ".";
        }
        return "Current service status: " + safe(trip.status()).replace('_', ' ') + ".";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
