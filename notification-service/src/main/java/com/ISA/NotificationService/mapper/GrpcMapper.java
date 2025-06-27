package com.ISA.Restaurant.mapper;

import com.ISA.Restaurant.Entity.Menu;
import com.ISA.Restaurant.Entity.MenuItem;
import restaurant.Restaurant;

import java.util.List;
import java.util.stream.Collectors;

public class GrpcMapper {

    public static Restaurant.Menu mapMenuToGrpc(Menu menu, List<MenuItem> menuItems) {
        List<Restaurant.MenuItem> grpcMenuItems = menuItems.stream()
                .filter(MenuItem::getActive)
                .map(GrpcMapper::mapMenuItemToGrpc)
                .collect(Collectors.toList());

        return Restaurant.Menu.newBuilder()
                .setMenuId(menu.getMenuId())
                .setMenuName(menu.getMenuName())
                .setMenuDescription(menu.getMenuDescription())
                .addAllMenuItems(grpcMenuItems)
                .build();
    }

    public static Restaurant.MenuItem mapMenuItemToGrpc(MenuItem menuItem) {
        return Restaurant.MenuItem.newBuilder()
                .setMenuItemId(menuItem.getMenuItemId())
                .setMenuItemName(menuItem.getMenuItemName())
                .setMenuItemDescription(menuItem.getMenuItemDescription())
                .setMenuItemPrice(menuItem.getMenuItemPrice())
                .addAllImageUrls(menuItem.getImageUrls() != null ? menuItem.getImageUrls() : List.of())
                .build();
    }

    public static Restaurant.RestaurantSummary mapRestaurantToSummary(com.ISA.Restaurant.Entity.Restaurant restaurant) {
        return Restaurant.RestaurantSummary.newBuilder()
                .setRestaurantId(restaurant.getRestaurantId())
                .setRestaurantName(restaurant.getRestaurantName())
                .setRestaurantCity(restaurant.getRestaurantCity())
                .setCoverImageUrl(restaurant.getCoverImageUrl() != null ? restaurant.getCoverImageUrl() : "")
                .build();
    }
}
