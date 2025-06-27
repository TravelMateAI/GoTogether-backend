//package com.ISA.Restaurant.utils;
//
//import com.ISA.Restaurant.enums.OrderStatus;
//import com.fasterxml.jackson.core.JsonParser;
//import com.fasterxml.jackson.databind.DeserializationContext;
//import com.fasterxml.jackson.databind.JsonDeserializer;
//
//import java.io.IOException;
//
//public class OrderStatusDeserializer extends JsonDeserializer<OrderStatus> {
//
//    @Override
//    public OrderStatus deserialize(JsonParser parser, DeserializationContext context) throws IOException {
//        String value = parser.readValueAs().get("value").asText(); // Access the "value" field
//        return OrderStatus.fromValue(value);
//    }
//}
