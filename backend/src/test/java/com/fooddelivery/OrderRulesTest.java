package com.fooddelivery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.model.MenuCategory;
import com.fooddelivery.model.MenuItem;
import com.fooddelivery.model.Order;
import com.fooddelivery.model.OrderStatus;
import com.fooddelivery.model.PaymentMethod;
import com.fooddelivery.model.PaymentStatus;
import com.fooddelivery.model.Restaurant;
import com.fooddelivery.model.Role;
import com.fooddelivery.model.User;
import com.fooddelivery.repository.MenuItemRepository;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderRulesTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private RestaurantRepository restaurants;

    @Autowired
    private MenuItemRepository menuItems;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void checkoutTotalComesFromStoredMenuPrices() throws Exception {
        Kitchen kitchen = kitchen("priced");
        String token = register("priced.customer@fooddelivery.com");

        mvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "restaurantId": %d,
                                  "customerName": "Rahul Mehta",
                                  "customerPhone": "9876501234",
                                  "deliveryAddress": "12 Lake Road, Hyderabad",
                                  "paymentMethod": "COD",
                                  "items": [
                                    {"menuItemId": %d, "quantity": 2},
                                    {"menuItemId": %d, "quantity": 1}
                                  ]
                                }
                                """.formatted(kitchen.restaurantId, kitchen.dosaId, kitchen.chaiId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(241.00));
    }

    @Test
    void rejectsDishesFromAnotherRestaurant() throws Exception {
        Kitchen first = kitchen("first");
        Kitchen second = kitchen("second");
        String token = register("mixed.customer@fooddelivery.com");
        long before = orders.count();

        mvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "restaurantId": %d,
                                  "customerName": "Rahul Mehta",
                                  "customerPhone": "9876501234",
                                  "deliveryAddress": "12 Lake Road, Hyderabad",
                                  "paymentMethod": "COD",
                                  "items": [
                                    {"menuItemId": %d, "quantity": 1},
                                    {"menuItemId": %d, "quantity": 1}
                                  ]
                                }
                                """.formatted(first.restaurantId, first.dosaId, second.dosaId)))
                .andExpect(status().isBadRequest());

        assertThat(orders.count()).isEqualTo(before);
    }

    @Test
    void rejectsStripeOrdersAndClientPaymentReferences() throws Exception {
        Kitchen kitchen = kitchen("pay");
        String token = register("pay.customer@fooddelivery.com");
        String order = """
                {
                  "restaurantId": %d,
                  "customerName": "Rahul Mehta",
                  "customerPhone": "9876501234",
                  "deliveryAddress": "12 Lake Road, Hyderabad",
                  "paymentMethod": "%s",
                  "stripePaymentId": %s,
                  "items": [{"menuItemId": %d, "quantity": 1}]
                }
                """;
        long before = orders.count();

        mvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order.formatted(kitchen.restaurantId, "STRIPE", "\"pi_fake\"", kitchen.dosaId)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order.formatted(kitchen.restaurantId, "COD", "\"pi_fake\"", kitchen.dosaId)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "restaurantId": %d,
                                  "customerName": "Rahul Mehta",
                                  "customerPhone": "9876501234",
                                  "deliveryAddress": "12 Lake Road, Hyderabad",
                                  "paymentMethod": "COD",
                                  "items": []
                                }
                                """.formatted(kitchen.restaurantId)))
                .andExpect(status().isBadRequest());

        assertThat(orders.count()).isEqualTo(before);
    }

    @Test
    void invalidStatusChangeLeavesOrderAndPaymentUntouched() throws Exception {
        Kitchen kitchen = kitchen("status");
        String customer = register("status.customer@fooddelivery.com");
        String admin = admin("status.admin@fooddelivery.com");
        long orderId = place(customer, kitchen);
        BigDecimal revenueBefore = orders.sumRevenue();

        mvc.perform(patch("/api/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));

        mvc.perform(patch("/api/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PLACED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));

        assertThat(orders.sumRevenue()).isEqualByComparingTo(revenueBefore);
    }

    @Test
    void codIsPaidOnlyOnDeliveryAndOnlyThatRevenueIsCounted() throws Exception {
        Kitchen kitchen = kitchen("revenue");
        String customer = register("revenue.customer@fooddelivery.com");
        String admin = admin("revenue.admin@fooddelivery.com");
        saveHistoricalStripeOrder(customer, kitchen);
        BigDecimal before = orders.sumRevenue();
        long orderId = place(customer, kitchen);

        for (String status : new String[]{"CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY"}) {
            mvc.perform(patch("/api/orders/" + orderId + "/status")
                            .header("Authorization", "Bearer " + admin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"" + status + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentStatus").value("PENDING"));
        }

        mvc.perform(patch("/api/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));

        assertThat(orders.sumRevenue().subtract(before)).isEqualByComparingTo("100.50");

        mvc.perform(patch("/api/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void customerCannotReadSomeoneElsesOrder() throws Exception {
        Kitchen kitchen = kitchen("access");
        String owner = register("owner.customer@fooddelivery.com");
        String other = register("other.customer@fooddelivery.com");
        long orderId = place(owner, kitchen);

        mvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk());
    }

    private void saveHistoricalStripeOrder(String customerToken, Kitchen kitchen) throws Exception {
        String email = json.readTree(mvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andReturn().getResponse().getContentAsString()).get("email").asText();
        User customer = users.findByEmail(email).orElseThrow();
        Restaurant restaurant = restaurants.findById(kitchen.restaurantId).orElseThrow();
        Order historical = new Order();
        historical.setUser(customer);
        historical.setRestaurant(restaurant);
        historical.setCustomerName("Archive");
        historical.setCustomerPhone("9876501234");
        historical.setDeliveryAddress("Old address");
        historical.setPaymentMethod(PaymentMethod.STRIPE);
        historical.setPaymentStatus(PaymentStatus.PAID);
        historical.setStatus(OrderStatus.DELIVERED);
        historical.setStripePaymentId("pi_historical");
        historical.setTotalAmount(new BigDecimal("999.00"));
        orders.save(historical);
    }

    private long place(String token, Kitchen kitchen) throws Exception {
        String body = mvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "restaurantId": %d,
                                  "customerName": "Rahul Mehta",
                                  "customerPhone": "9876501234",
                                  "deliveryAddress": "12 Lake Road, Hyderabad",
                                  "paymentMethod": "COD",
                                  "items": [{"menuItemId": %d, "quantity": 1}]
                                }
                                """.formatted(kitchen.restaurantId, kitchen.dosaId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private String register(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Rahul Mehta","email":"%s","password":"secret1","phone":"9876501234"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private String admin(String email) throws Exception {
        User admin = new User();
        admin.setName("Admin Rao");
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode("Admin@123"));
        admin.setPhone("9876543210");
        admin.setRole(Role.ADMIN);
        users.save(admin);
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Admin@123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private Kitchen kitchen(String name) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(name + " kitchen");
        restaurant.setCuisineType("Test");
        restaurant.setAddress("Test lane");
        restaurant.setPhone("9800000000");
        restaurant.setRating(new BigDecimal("4.5"));
        restaurant.setActive(true);
        restaurants.save(restaurant);

        MenuItem dosa = dish(restaurant, "Dosa", "100.50");
        MenuItem chai = dish(restaurant, "Chai", "40.00");
        return new Kitchen(restaurant.getId(), dosa.getId(), chai.getId());
    }

    private MenuItem dish(Restaurant restaurant, String name, String price) {
        MenuItem item = new MenuItem();
        item.setRestaurant(restaurant);
        item.setName(name);
        item.setDescription(name);
        item.setPrice(new BigDecimal(price));
        item.setCategory(MenuCategory.MAIN);
        item.setVeg(true);
        item.setAvailable(true);
        return menuItems.save(item);
    }

    private record Kitchen(long restaurantId, long dosaId, long chaiId) {
    }
}
