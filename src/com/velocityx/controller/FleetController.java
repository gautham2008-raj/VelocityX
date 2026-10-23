package com.velocityx.controller;

import com.sun.net.httpserver.HttpExchange;
import com.velocityx.model.Category;
import com.velocityx.model.InventoryUnit;
import com.velocityx.model.VehicleCardDTO;
import com.velocityx.repository.VehicleRepository;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FleetController extends BaseApiHandler {
    private final VehicleRepository vehicleRepo = new VehicleRepository();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        try {
            if ("GET".equals(exchange.getRequestMethod()) && path.equals("/api/fleet")) {
                Category cat = null;
                if (query != null && query.contains("category=")) {
                    String catStr = query.split("category=")[1].split("&")[0].toUpperCase();
                    try {
                        cat = Category.valueOf(catStr);
                    } catch (IllegalArgumentException ignored) {}
                }
                List<VehicleCardDTO> fleet = vehicleRepo.getFleetCards(cat);
                sendJsonResponse(exchange, 200, fleet);

            } else if ("GET".equals(exchange.getRequestMethod()) && path.startsWith("/api/fleet/units/")) {
                String modelIdStr = path.substring("/api/fleet/units/".length());
                long modelId = Long.parseLong(modelIdStr);
                List<InventoryUnit> units = vehicleRepo.getUnitsForModel(modelId);
                sendJsonResponse(exchange, 200, units);

            } else {
                sendError(exchange, 404, "Fleet endpoint not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Fleet retrieval error: " + e.getMessage());
        }
    }
}