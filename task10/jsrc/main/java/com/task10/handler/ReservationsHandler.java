package com.task10.handler;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.task10.pojo.Reservations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
public class ReservationsHandler {
    public APIGatewayProxyResponseEvent postReservations(APIGatewayProxyRequestEvent request) {
        System.out.println("Post Reservations");
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
        DynamoDBMapper mapper = new DynamoDBMapper(client);

        System.out.println("Mapper created successfully");
        Gson gson = new Gson();
        Reservations reservationItem = gson.fromJson(request.getBody(), Reservations.class);
        String uuid = UUID.randomUUID().toString();
        reservationItem.setId(uuid);
        System.out.println(reservationItem);
        System.out.println("Saving Reservations");
        mapper.save(reservationItem);
        System.out.println("Returning");
        // Construct response
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setBody("{\"id\": " + reservationItem.getId() + "}");
        return response;
    }

    public APIGatewayProxyResponseEvent getAllReservations() {
        AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.defaultClient();
        String tableName = "cmtr-57544369-Reservations-test";
        ScanRequest scanRequest = new ScanRequest()
                .withTableName(tableName);
        ScanResult result = ddb.scan(scanRequest);
        List<Reservations> reservations = new ArrayList<>();
        for (Map<String, AttributeValue> item : result.getItems()){
            Reservations reservation = new Reservations(); // assuming you have a POJO class 'Table' with these fields and their setters & getters
            reservation.setId(item.get("id").getS());
            reservation.setTableNumber(Integer.parseInt(item.get("tableNumber").getN()));
            reservation.setClientName(item.get("clientName").getS());
            reservation.setPhoneNumber(item.get("phoneNumber").getS());
            reservation.setDate(item.get("date").getS());
            reservation.setSlotTimeStart(item.get("slotTimeStart").getS());
            reservation.setSlotTimeEnd(item.get("slotTimeEnd").getS());
            reservations.add(reservation);
        }
        Map<String, List<Reservations>> responseStruct = new HashMap<>();
        responseStruct.put("reservations", reservations);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setBody(new Gson().toJson(responseStruct));
        return response;
    }
}
 