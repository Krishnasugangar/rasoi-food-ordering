package com.fooddelivery.service;

import com.fooddelivery.dto.MenuItemRequest;
import com.fooddelivery.dto.MenuItemResponse;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.repository.MenuItemRepository;
import com.fooddelivery.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantService restaurantService;

    public MenuItemService(MenuItemRepository menuItemRepository, RestaurantService restaurantService) {
        this.menuItemRepository = menuItemRepository;
        this.restaurantService = restaurantService;
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> list(Long restaurantId) {
        if (restaurantId == null) {
            return menuItemRepository.findAll().stream()
                    .filter(this::visibleToCaller)
                    .map(this::toResponse)
                    .toList();
        }
        Restaurant restaurant = restaurantService.find(restaurantId);
        if (!SecurityUtils.isAdmin() && !restaurant.isActive()) {
            throw new ResourceNotFoundException("Restaurant not found");
        }
        List<MenuItem> items = SecurityUtils.isAdmin()
                ? menuItemRepository.findByRestaurantIdOrderByCategoryAscNameAsc(restaurantId)
                : menuItemRepository.findByRestaurantIdAndAvailableTrueOrderByCategoryAscNameAsc(restaurantId);
        return items.stream().filter(this::visibleToCaller).map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MenuItemResponse get(Long id) {
        MenuItem item = find(id);
        if (!visibleToCaller(item)) {
            throw new ResourceNotFoundException("Menu item not found");
        }
        return toResponse(item);
    }

    @Transactional
    public MenuItemResponse create(MenuItemRequest request) {
        MenuItem item = new MenuItem();
        apply(item, request);
        return toResponse(menuItemRepository.save(item));
    }

    @Transactional
    public MenuItemResponse update(Long id, MenuItemRequest request) {
        MenuItem item = find(id);
        apply(item, request);
        return toResponse(menuItemRepository.save(item));
    }

    @Transactional
    public void delete(Long id) {
        MenuItem item = find(id);
        item.setAvailable(false);
        menuItemRepository.save(item);
    }

    private boolean visibleToCaller(MenuItem item) {
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        return item.isAvailable() && item.getRestaurant() != null && item.getRestaurant().isActive();
    }

    public MenuItem find(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
    }

    private void apply(MenuItem item, MenuItemRequest request) {
        Restaurant restaurant = restaurantService.find(request.getRestaurantId());
        item.setRestaurant(restaurant);
        item.setName(request.getName().trim());
        item.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        item.setPrice(request.getPrice());
        item.setCategory(request.getCategory());
        if (request.getVeg() != null) {
            item.setVeg(request.getVeg());
        }
        if (request.getAvailable() != null) {
            item.setAvailable(request.getAvailable());
        }
    }

    public MenuItemResponse toResponse(MenuItem item) {
        MenuItemResponse response = new MenuItemResponse();
        response.setId(item.getId());
        response.setRestaurantId(item.getRestaurant().getId());
        response.setRestaurantName(item.getRestaurant().getName());
        response.setName(item.getName());
        response.setDescription(item.getDescription());
        response.setPrice(item.getPrice());
        response.setCategory(item.getCategory());
        response.setVeg(item.isVeg());
        response.setAvailable(item.isAvailable());
        response.setCreatedAt(item.getCreatedAt());
        return response;
    }
}
