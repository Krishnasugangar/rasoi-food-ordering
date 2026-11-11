package com.fooddelivery.service;

import com.fooddelivery.dto.CatalogSummary;
import com.fooddelivery.dto.RestaurantRequest;
import com.fooddelivery.dto.RestaurantResponse;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.repository.MenuItemRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    public RestaurantService(RestaurantRepository restaurantRepository, MenuItemRepository menuItemRepository) {
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Transactional(readOnly = true)
    public CatalogSummary summary() {
        CatalogSummary summary = new CatalogSummary();
        summary.setKitchens(restaurantRepository.countByActiveTrue());
        summary.setDishes(menuItemRepository.countAvailableInActiveRestaurants());
        return summary;
    }

    @Transactional(readOnly = true)
    public List<RestaurantResponse> list(String search) {
        List<Restaurant> restaurants = SecurityUtils.isAdmin()
                ? restaurantRepository.findAllByOrderByNameAsc()
                : restaurantRepository.findByActiveTrueOrderByNameAsc();
        String needle = search == null ? "" : search.trim().toLowerCase();
        return restaurants.stream()
                .filter(restaurant -> needle.isEmpty() || matches(restaurant, needle))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RestaurantResponse get(Long id) {
        Restaurant restaurant = find(id);
        if (!SecurityUtils.isAdmin() && !restaurant.isActive()) {
            throw new ResourceNotFoundException("Restaurant not found");
        }
        return toResponse(restaurant);
    }

    @Transactional
    public RestaurantResponse create(RestaurantRequest request) {
        Restaurant restaurant = new Restaurant();
        apply(restaurant, request);
        return toResponse(restaurantRepository.save(restaurant));
    }

    @Transactional
    public RestaurantResponse update(Long id, RestaurantRequest request) {
        Restaurant restaurant = find(id);
        apply(restaurant, request);
        return toResponse(restaurantRepository.save(restaurant));
    }

    @Transactional
    public void delete(Long id) {
        Restaurant restaurant = find(id);
        restaurant.setActive(false);
        restaurantRepository.save(restaurant);
    }

    public Restaurant find(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
    }

    private boolean matches(Restaurant restaurant, String needle) {
        return contains(restaurant.getName(), needle) || contains(restaurant.getCuisineType(), needle)
                || contains(restaurant.getAddress(), needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    private void apply(Restaurant restaurant, RestaurantRequest request) {
        restaurant.setName(request.getName().trim());
        restaurant.setCuisineType(blankToNull(request.getCuisineType()));
        restaurant.setAddress(blankToNull(request.getAddress()));
        restaurant.setPhone(blankToNull(request.getPhone()));
        restaurant.setRating(request.getRating() == null ? BigDecimal.ZERO : request.getRating());
        if (request.getActive() != null) {
            restaurant.setActive(request.getActive());
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public RestaurantResponse toResponse(Restaurant restaurant) {
        RestaurantResponse response = new RestaurantResponse();
        response.setId(restaurant.getId());
        response.setName(restaurant.getName());
        response.setCuisineType(restaurant.getCuisineType());
        response.setAddress(restaurant.getAddress());
        response.setPhone(restaurant.getPhone());
        response.setRating(restaurant.getRating());
        response.setActive(restaurant.isActive());
        response.setCreatedAt(restaurant.getCreatedAt());
        return response;
    }
}
