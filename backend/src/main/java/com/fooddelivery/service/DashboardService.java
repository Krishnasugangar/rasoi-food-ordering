package com.fooddelivery.service;

import com.fooddelivery.dto.DashboardSummary;
import com.fooddelivery.dto.OrderResponse;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.repository.MenuItemRepository;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public DashboardService(
            RestaurantRepository restaurantRepository,
            MenuItemRepository menuItemRepository,
            OrderRepository orderRepository,
            OrderService orderService
    ) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary() {
        DashboardSummary summary = new DashboardSummary();
        summary.setTotalRestaurants(restaurantRepository.count());
        summary.setActiveRestaurants(restaurantRepository.findByActiveTrueOrderByNameAsc().size());
        summary.setTotalMenuItems(menuItemRepository.count());
        summary.setTotalOrders(orderRepository.count());
        summary.setPlacedOrders(orderRepository.countByStatus(OrderStatus.PLACED));
        summary.setRevenue(orderRepository.sumRevenue());

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            byStatus.put(status.name(), orderRepository.countByStatus(status));
        }
        summary.setOrdersByStatus(byStatus);

        List<OrderResponse> recent = orderRepository.findTop6ByOrderByCreatedAtDesc().stream()
                .map(orderService::toResponse)
                .toList();
        summary.setRecentOrders(recent);
        return summary;
    }
}
