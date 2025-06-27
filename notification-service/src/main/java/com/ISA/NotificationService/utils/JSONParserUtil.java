package com.ISA.Restaurant.utils;


import com.ISA.Restaurant.Entity.RiderLocation;
import org.json.JSONObject;

public class JSONParserUtil {
    public static RiderLocation parseLocation(String message) {
        JSONObject jsonObject = new JSONObject(message);
        RiderLocation location = new RiderLocation();
        location.setCustomerId(jsonObject.getString("customerId"));
        location.setLatitude(jsonObject.getDouble("latitude"));
        location.setLongitude(jsonObject.getDouble("longitude"));
        return location;
    }
}



