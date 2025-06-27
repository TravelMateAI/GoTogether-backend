package com.ISA.Restaurant.mapper;

import com.ISA.Restaurant.Dto.Event.CustomerOrderDto;
import com.ISA.Restaurant.Dto.Event.OrderItemDto;
import com.ISA.Restaurant.Dto.Event.RiderRequestDto;
import com.ISA.Restaurant.Dto.Response.OrderDto;
import com.ISA.Restaurant.Dto.Response.OrderItemDTO;
import com.ISA.Restaurant.Dto.RestaurantDto;
import com.ISA.Restaurant.Entity.CustomerLocation;
import com.ISA.Restaurant.Entity.Order;
import com.ISA.Restaurant.Entity.OrderItem;
import com.ISA.Restaurant.Entity.RestaurantLocation;
import com.ISA.Restaurant.enums.OrderStatus;
import com.ISA.Restaurant.repo.MenuItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OrderMapper {

    private static MenuItemRepository menuItemRepository;

    // Setter method for static repository
    public OrderMapper(MenuItemRepository menuItemRepository) {
        OrderMapper.menuItemRepository = menuItemRepository;
    }

    public static OrderDto mapToOrderDto(Order order) {
        return new OrderDto(
                order.getOrderId(),
                order.getRestaurantId(),
                order.getRestaurantLocationList(),
                order.getCustomerLocationsList(),
                mapToOrderItemDtos(order.getOrderItems()),
                order.getRestaurantName(),
                order.getRestaurantAddress(),
                order.getRestaurantPhone(),
                order.getCustomerName(),
                order.getCustomerAddress(),
                order.getCustomerPhone(),
                order.getStatus(),
                order.getPaymentAmount(),
                order.getCreatedDate(),
                order.getLastUpdated()
        );
    }

    public static List<OrderItemDTO> mapToOrderItemDtos(List<OrderItem> orderItems) {
        if (orderItems == null) return Collections.emptyList();
        return orderItems.stream()
                .map(orderItem -> new OrderItemDTO(
                        orderItem.getMenuItemId(),
                        orderItem.getQuantity()
                ))
                .collect(Collectors.toList());
    }

    public static Order toEntity(CustomerOrderDto dto) {
        Order order = new Order();
        order.setOrderId(dto.getOrderId());
        order.setCustomerLocations(toCustomerLocation(dto.getCustomerLocation(), order));
        order.setRestaurantId(dto.getRestaurantId());
        order.setCustomerName(dto.getCustomerName());
        order.setCustomerAddress(dto.getCustomerAddress());
        order.setCustomerPhone(dto.getCustomerPhone());
        order.setOrderItems(mapOrderItems(dto.getOrderItems(), order));
        order.setPaymentAmount(calculateTotalPrice(dto.getOrderItems(), dto.getRestaurantId()));
        return order;
    }

    public static Order createOrderEntity(CustomerOrderDto orderDto, RestaurantDto restaurantDetails) {
        Order order = toEntity(orderDto);
        order.setRestaurantId(orderDto.getRestaurantId());
        order.setRestaurantName(restaurantDetails.getRestaurantName());
        order.setRestaurantAddress(restaurantDetails.getRestaurantAddress());
        order.setRestaurantPhone(restaurantDetails.getRestaurantPhone());
        order.setRestaurantLocation(toRestaurantLocation(restaurantDetails.getRestaurantLocation(), order));
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedDate(LocalDateTime.now());
        order.setLastUpdated(LocalDateTime.now());
        return order;
    }

    public static RiderRequestDto toRiderRequestDto(Order order) {
        return new RiderRequestDto(
                order.getOrderId(),
                order.getRestaurantLocationList(),
                order.getCustomerLocationsList(),
                order.getRestaurantName(),
                order.getRestaurantAddress(),
                order.getRestaurantPhone(),
                order.getCustomerName(),
                order.getCustomerAddress(),
                order.getCustomerPhone(),
                order.getPaymentAmount()
        );
    }

    private static RestaurantLocation toRestaurantLocation(String locationString, Order order) {
        List<Double> coordinates = convertStringToList(locationString);
        if (coordinates.size() < 2) return null;
        return new RestaurantLocation(null, coordinates.get(0), coordinates.get(1), order);
    }

    private static CustomerLocation toCustomerLocation(List<Double> coordinates, Order order) {
        if (coordinates == null || coordinates.size() < 2) return null;
        return new CustomerLocation(null, coordinates.get(0), coordinates.get(1), order);
    }

    private static List<Double> convertStringToList(String locationString) {
        try {
            if (locationString == null || locationString.isEmpty()) return Collections.emptyList();
            return Arrays.stream(locationString.split(","))
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.error("Invalid location format: {}", locationString, e);
            throw new IllegalArgumentException("Invalid location format: " + locationString, e);
        }
    }

    private static List<OrderItem> mapOrderItems(List<OrderItemDto> orderItemDtos, Order order) {
        if (orderItemDtos == null) return List.of();
        return orderItemDtos.stream()
                .map(dto -> new OrderItem(null, dto.getMenuItemId(), dto.getQuantity(), order))
                .collect(Collectors.toList());
    }

    private static Double calculateTotalPrice(List<OrderItemDto> orderItems, String restaurantId) {
        if (orderItems == null || orderItems.isEmpty()) {
            log.info("No order items provided. Total price is 0.0");
            return 0.0;
        }

        try {
            Long parsedRestaurantId = Long.parseLong(restaurantId);
            return orderItems.stream()
                    .mapToDouble(item -> {
                        try {
                            Long menuItemId = Long.parseLong(item.getMenuItemId());
                            var menuItem = menuItemRepository.findByMenuItemId(menuItemId);
                            if (menuItem == null) {
                                log.error("MenuItem not found: {} for restaurantId: {}", item.getMenuItemId(), restaurantId);
                                return 0.0;
                            }
                            return menuItem.getMenuItemPrice() * item.getQuantity();
                        } catch (NumberFormatException e) {
                            log.error("Invalid menuItemId: {}", item.getMenuItemId(), e);
                            return 0.0;
                        }
                    })
                    .sum();
        } catch (NumberFormatException e) {
            log.error("Invalid restaurantId: {}", restaurantId, e);
            throw new IllegalArgumentException("Invalid restaurantId: " + restaurantId, e);
        }
    }
}
