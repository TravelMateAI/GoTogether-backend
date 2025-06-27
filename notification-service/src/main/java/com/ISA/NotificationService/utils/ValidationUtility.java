package com.ISA.Restaurant.utils;


import com.ISA.Restaurant.enums.OrderStatus;
import com.ISA.Restaurant.exception.InvalidOrderStateTransitionException;
import com.ISA.Restaurant.exception.SameOrderStateException;

public class ValidationUtility {

    public static void validateStateTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == null || currentStatus.equals(newStatus)) {
            throw new SameOrderStateException("Order already in the same state.");
        }
//        if (!currentStatus.canTransitionTo(newStatus)) {
//            throw new InvalidOrderStateTransitionException(
//                    String.format("Invalid transition from %s to %s", currentStatus, newStatus));
//        }
    }
}