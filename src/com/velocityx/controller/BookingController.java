package com.velocityx.controller;

import com.sun.net.httpserver.HttpExchange;
import com.velocityx.model.Invoice;
import com.velocityx.model.RentalQuote;
import com.velocityx.repository.BookingRepository;
import com.velocityx.security.SecurityContext;
import com.velocityx.service.RentalService;
import com.velocityx.util.JsonUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingController extends BaseApiHandler {
    private final RentalService rentalService = new RentalService();
    private final BookingRepository bookingRepo = new BookingRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if ("POST".equals(method) && path.equals("/api/bookings/quote")) {
                handleQuote(exchange);
            } else if ("POST".equals(method) && path.equals("/api/bookings/book")) {
                handleBook(exchange);
            } else if ("GET".equals(method) && path.equals("/api/bookings/my-rentals")) {
                handleMyRentals(exchange);
            } else if ("POST".equals(method) && path.equals("/api/bookings/return")) {
                handleReturn(exchange);
            } else {
                sendError(exchange, 404, "Endpoint not found");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Booking process failed: " + e.getMessage());
        }
    }

    private void handleQuote(HttpExchange exchange) throws Exception {
        String body = readRequestBody(exchange);
        Map<String, String> data = JsonUtil.parseSimpleJson(body);

        long unitId = Long.parseLong(data.get("unitId"));
        LocalDate start = LocalDate.parse(data.get("startDate"));
        LocalDate end = LocalDate.parse(data.get("endDate"));

        RentalQuote quote = rentalService.calculateQuote(unitId, start, end);
        sendJsonResponse(exchange, 200, quote);
    }

    private void handleBook(HttpExchange exchange) throws Exception {
        SecurityContext ctx = getSecurityContext(exchange);
        if (ctx == null) {
            sendError(exchange, 401, "Authentication required to book a vehicle.");
            return;
        }

        String body = readRequestBody(exchange);
        Map<String, String> data = JsonUtil.parseSimpleJson(body);

        long unitId = Long.parseLong(data.get("unitId"));
        LocalDate start = LocalDate.parse(data.get("startDate"));
        LocalDate end = LocalDate.parse(data.get("endDate"));

        Invoice invoice = rentalService.executeBooking(ctx.getUserId(), unitId, start, end);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Reservation confirmed and invoice generated.");
        resp.put("invoice", invoice);
        sendJsonResponse(exchange, 201, resp);
    }

    private void handleMyRentals(HttpExchange exchange) throws Exception {
        SecurityContext ctx = getSecurityContext(exchange);
        if (ctx == null) {
            sendError(exchange, 401, "Authentication required.");
            return;
        }

        List<Map<String, Object>> rentals = bookingRepo.getUserBookings(ctx.getUserId());
        sendJsonResponse(exchange, 200, rentals);
    }

    private void handleReturn(HttpExchange exchange) throws Exception {
        SecurityContext ctx = getSecurityContext(exchange);
        if (ctx == null) {
            sendError(exchange, 401, "Authentication required.");
            return;
        }

        String body = readRequestBody(exchange);
        Map<String, String> data = JsonUtil.parseSimpleJson(body);

        long bookingId = Long.parseLong(data.get("bookingId"));
        int addedKm = Integer.parseInt(data.getOrDefault("addedKm", "0"));

        rentalService.returnVehicle(bookingId, addedKm);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Vehicle successfully returned. Inventory unit status updated to AVAILABLE.");
        sendJsonResponse(exchange, 200, resp);
    }
}