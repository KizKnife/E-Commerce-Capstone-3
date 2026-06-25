package org.yearup.service;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.models.*;
import org.yearup.repository.OrderLineItemRepository;
import org.yearup.repository.OrderRepository;

import java.time.LocalDate;

@Service
public class OrderService
{
    private final OrderRepository orderRepository;
    private final OrderLineItemRepository orderLineItemRepository;
    private final ShoppingCartService shoppingCartService;
    private final ProfileService profileService;

    public OrderService(OrderRepository orderRepository, OrderLineItemRepository orderLineItemRepository, ShoppingCartService shoppingCartService, ProfileService profileService) {
        this.orderRepository = orderRepository;
        this.orderLineItemRepository = orderLineItemRepository;
        this.shoppingCartService = shoppingCartService;
        this.profileService = profileService;
    }

    @Transactional
    public Order checkout(int userId)
    {
        ShoppingCart cart = shoppingCartService.getByUserId(userId);

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty())
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        Profile profile = profileService.getByUserId(userId);

        Order order = new Order();

        order.setUserId(userId);
        order.setDate(LocalDate.now().toString());

        order.setAddress(profile.getAddress());
        order.setCity(profile.getCity());
        order.setState(profile.getState());
        order.setZip(profile.getZip());

        order.setShippingAmount(0);

        order = orderRepository.save(order);

        for (ShoppingCartItem item : cart.getItems().values())
        {
            OrderLineItem line = new OrderLineItem();

            line.setOrderId(order.getOrderId());
            line.setProductId(item.getProductId());
            line.setSalesPrice(item.getProduct().getPrice());
            line.setQuantity(item.getQuantity());
            line.setDiscount(item.getDiscountPercent());

            orderLineItemRepository.save(line);
        }

        shoppingCartService.clearCart(userId);

        return order;
    }
}
