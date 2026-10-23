package com.velocityx.controller;

import com.sun.net.httpserver.HttpExchange;
import com.velocityx.model.UnitStatus;
import com.velocityx.repository.BookingRepository;
import com.velocityx.repository.UserRepository;
import com.velocityx.repository.VehicleRepository;
import com.velocityx.security.SecurityContext;
import com.velocityx.util.JsonUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminController extends BaseApiHandler {
    private final VehicleRepository vehicleRepo = new VehicleRepository();
    private final BookingRepository bookingRepo = new BookingRepository();
    private final UserRepository userRepo = new UserRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SecurityContext ctx = getSecurityContext(exchange);
        if (ctx == null || !ctx.isAdmin()) {
            sendError(exchange, 403, "Access Denied: Root Operations Administrator clearance required.");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if ("GET".equals(method) && path.equals("/api/admin/metrics")) {
                handleMetrics(exchange);
            } else if ("GET".equals(method) && path.equals("/api/admin/bookings")) {
                handleAllBookings(exchange);
            } else if ("POST".equals(method) && path.equals("/api/admin/inventory/add")) {
                handleAddUnit(exchange);
            } else if ("POST".equals(method) && path.equals("/api/admin/inventory/bulk-add")) {
                handleBulkAdd(exchange);
            } else {
                sendError(exchange, 404, "Admin endpoint not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Admin operations failed: " + e.getMessage());
        }
    }

    private void handleMetrics(HttpExchange exchange) throws Exception {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalUnits", vehicleRepo.countTotalUnits());
        metrics.put("availableUnits", vehicleRepo.countUnitsByStatus(UnitStatus.AVAILABLE));
        metrics.put("rentedUnits", vehicleRepo.countUnitsByStatus(UnitStatus.RENTED));
        metrics.put("maintenanceUnits", vehicleRepo.countUnitsByStatus(UnitStatus.MAINTENANCE));
        metrics.put("activeRentals", bookingRepo.countActiveRentals());
        metrics.put("totalUsers", userRepo.countTotalUsers());
        metrics.put("totalRevenueINR", bookingRepo.calculateTotalRevenue());

        sendJsonResponse(exchange, 200, metrics);
    }

    private void handleAllBookings(HttpExchange exchange) throws Exception {
        List<Map<String, Object>> bookings = bookingRepo.getAllBookingsForAdmin();
        sendJsonResponse(exchange, 200, bookings);
    }

    private void handleAddUnit(HttpExchange exchange) throws Exception {
        String body = readRequestBody(exchange);
        Map<String, String> data = JsonUtil.parseSimpleJson(body);

        long modelId = Long.parseLong(data.get("modelId"));
        String reg = data.get("registrationNumber");
        BigDecimal rate = new BigDecimal(data.get("dailyRate"));
        int odo = Integer.parseInt(data.getOrDefault("odometerKm", "0"));

        vehicleRepo.addInventoryUnit(modelId, reg, rate, odo);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Unit " + reg + " successfully provisioned.");
        sendJsonResponse(exchange, 201, resp);
    }

    private void handleBulkAdd(HttpExchange exchange) throws Exception {
        String body = readRequestBody(exchange);
        Map<String, String> data = JsonUtil.parseSimpleJson(body);

        long modelId = Long.parseLong(data.get("modelId"));
        String prefix = data.get("regPrefix");
        int count = Integer.parseInt(data.get("count"));
        BigDecimal rate = new BigDecimal(data.get("dailyRate"));

        vehicleRepo.bulkAddInventoryUnits(modelId, prefix, count, rate);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", count + " physical units provisioned under prefix " + prefix);
        sendJsonResponse(exchange, 201, resp);
    }
}