package com.fooddelivery.service;

import com.fooddelivery.dto.OrderItemRequest;
import com.fooddelivery.dto.OrderItemResponse;
import com.fooddelivery.dto.OrderRequest;
import com.fooddelivery.dto.OrderResponse;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.exception.ConflictException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.exception.UnauthorizedException;
import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderItem;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.model.PaymentMethod;
import com.fooddelivery.model.PaymentStatus;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.Role;
import com.fooddelivery.model.User;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PLACED, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.PREPARING, EnumSet.of(OrderStatus.OUT_FOR_DELIVERY),
            OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class)
    );

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RestaurantService restaurantService;
    private final MenuItemService menuItemService;

    public OrderService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            RestaurantService restaurantService,
            MenuItemService menuItemService
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.restaurantService = restaurantService;
        this.menuItemService = menuItemService;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list() {
        User current = currentUser();
        List<Order> orders = current.getRole() == Role.ADMIN
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findByUserIdOrderByCreatedAtDesc(current.getId());
        return orders.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return toResponse(loadForCurrentUser(id));
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        rejectUnsupportedPayment(request);
        User current = currentUser();
        Restaurant restaurant = restaurantService.find(request.getRestaurantId());
        if (!restaurant.isActive()) {
            throw new BadRequestException("This restaurant is not accepting orders");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Add at least one dish");
        }

        Order order = new Order();
        order.setUser(current);
        order.setRestaurant(restaurant);
        order.setCustomerName(request.getCustomerName().trim());
        order.setCustomerPhone(request.getCustomerPhone().trim());
        order.setDeliveryAddress(request.getDeliveryAddress().trim());
        order.setPaymentMethod(PaymentMethod.COD);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setStripePaymentId(null);
        order.setStatus(OrderStatus.PLACED);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> lines = new ArrayList<>();
        for (OrderItemRequest line : request.getItems()) {
            if (line == null || line.getMenuItemId() == null || line.getQuantity() == null || line.getQuantity() < 1) {
                throw new BadRequestException("Each dish needs a menu item and a quantity of at least 1");
            }
            MenuItem menuItem = menuItemService.find(line.getMenuItemId());
            if (!menuItem.getRestaurant().isActive() || !menuItem.getRestaurant().getId().equals(restaurant.getId())) {
                throw new BadRequestException("Every dish must come from the selected restaurant");
            }
            if (!menuItem.isAvailable()) {
                throw new BadRequestException(menuItem.getName() + " is not available");
            }
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setMenuItem(menuItem);
            item.setQuantity(line.getQuantity());
            item.setPrice(menuItem.getPrice());
            lines.add(item);
            total = total.add(menuItem.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
        }
        order.setItems(lines);
        order.setTotalAmount(total);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus requested) {
        if (!SecurityUtils.isAdmin()) {
            throw new UnauthorizedException("Only an admin can update order status");
        }
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getStatus() == requested) {
            return toResponse(order);
        }
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), Set.of());
        if (!allowed.contains(requested)) {
            throw new ConflictException("That status change is not allowed");
        }
        order.setStatus(requested);
        if (requested == OrderStatus.DELIVERED && order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }
        return toResponse(orderRepository.save(order));
    }

    public OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUser().getId());
        response.setCustomerName(order.getCustomerName());
        response.setCustomerPhone(order.getCustomerPhone());
        response.setDeliveryAddress(order.getDeliveryAddress());
        response.setRestaurantId(order.getRestaurant().getId());
        response.setRestaurantName(order.getRestaurant().getName());
        response.setStatus(order.getStatus());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setStripePaymentId(order.getStripePaymentId());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setItems(order.getItems().stream().map(this::toItemResponse).toList());
        return response;
    }

    private void rejectUnsupportedPayment(OrderRequest request) {
        if (request.getPaymentMethod() != PaymentMethod.COD) {
            throw new BadRequestException("Only cash on delivery is accepted");
        }
        if (request.getStripePaymentId() != null && !request.getStripePaymentId().isBlank()) {
            throw new BadRequestException("Payment references are not accepted");
        }
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setMenuItemId(item.getMenuItem().getId());
        response.setName(item.getMenuItem().getName());
        response.setQuantity(item.getQuantity());
        response.setPrice(item.getPrice());
        response.setLineTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return response;
    }

    private Order loadForCurrentUser(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        User current = currentUser();
        if (current.getRole() != Role.ADMIN && !order.getUser().getId().equals(current.getId())) {
            throw new ResourceNotFoundException("Order not found");
        }
        return order;
    }

    private User currentUser() {
        User principal = SecurityUtils.currentUser();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("Login required"));
    }
}
