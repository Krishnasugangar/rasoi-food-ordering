package com.fooddelivery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.model.MenuCategory;
import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.repository.MenuItemRepository;
import com.fooddelivery.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogAndPasswordTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private RestaurantRepository restaurants;

    @Autowired
    private MenuItemRepository menuItems;

    @Test
    void homepageCountsFollowActiveKitchensAndAvailableDishes() throws Exception {
        long kitchensBefore = summary("kitchens");
        long dishesBefore = summary("dishes");
        Restaurant kitchen = kitchen("count-kitchen");
        MenuItem first = dish(kitchen, "Dosa", true);
        dish(kitchen, "Chai", true);

        mvc.perform(get("/api/restaurants/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kitchens").value(kitchensBefore + 1))
                .andExpect(jsonPath("$.dishes").value(dishesBefore + 2));

        kitchen.setActive(false);
        restaurants.save(kitchen);
        mvc.perform(get("/api/restaurants/summary"))
                .andExpect(jsonPath("$.kitchens").value(kitchensBefore))
                .andExpect(jsonPath("$.dishes").value(dishesBefore));

        kitchen.setActive(true);
        restaurants.save(kitchen);
        first.setAvailable(false);
        menuItems.save(first);
        mvc.perform(get("/api/restaurants/summary"))
                .andExpect(jsonPath("$.kitchens").value(kitchensBefore + 1))
                .andExpect(jsonPath("$.dishes").value(dishesBefore + 1));
    }

    @Test
    void passwordChangeKeepsTheOldPasswordUntilTheNewOneIsSaved() throws Exception {
        String email = "password.change@fooddelivery.com";
        String token = register(email, "secret1");

        mvc.perform(post("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"secret2\"}"))
                .andExpect(status().isBadRequest());
        login(email, "secret1");

        mvc.perform(post("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"secret1\",\"newPassword\":\"secret1\"}"))
                .andExpect(status().isBadRequest());
        login(email, "secret1");

        mvc.perform(post("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"secret1\",\"newPassword\":\"secret2\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"secret1\"}"))
                .andExpect(status().isUnauthorized());
        login(email, "secret2");
    }

    private long summary(String field) throws Exception {
        String body = mvc.perform(get("/api/restaurants/summary"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get(field).asLong();
    }

    private String register(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Password Tester","email":"%s","password":"%s","phone":"9876501234"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private void login(String email, String password) throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());
    }

    private Restaurant kitchen(String name) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(name);
        restaurant.setCuisineType("Test");
        restaurant.setAddress("Test lane");
        restaurant.setPhone("9800000000");
        restaurant.setRating(new BigDecimal("4.0"));
        restaurant.setActive(true);
        return restaurants.save(restaurant);
    }

    private MenuItem dish(Restaurant restaurant, String name, boolean available) {
        MenuItem item = new MenuItem();
        item.setRestaurant(restaurant);
        item.setName(name);
        item.setPrice(new BigDecimal("80.00"));
        item.setCategory(MenuCategory.MAIN);
        item.setVeg(true);
        item.setAvailable(available);
        return menuItems.save(item);
    }
}
