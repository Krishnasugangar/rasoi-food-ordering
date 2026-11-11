package com.fooddelivery.repository;

import com.fooddelivery.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    long countByActiveTrue();

    List<Restaurant> findByActiveTrueOrderByNameAsc();

    List<Restaurant> findAllByOrderByNameAsc();
}
