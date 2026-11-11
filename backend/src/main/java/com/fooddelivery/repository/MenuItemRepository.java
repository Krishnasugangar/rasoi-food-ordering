package com.fooddelivery.repository;

import com.fooddelivery.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    @Query("select count(m) from MenuItem m where m.available = true and m.restaurant.active = true")
    long countAvailableInActiveRestaurants();

    List<MenuItem> findByRestaurantIdOrderByCategoryAscNameAsc(Long restaurantId);

    List<MenuItem> findByRestaurantIdAndAvailableTrueOrderByCategoryAscNameAsc(Long restaurantId);
}
