package com.task10.handler;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
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
    private final String reservationTable = "cmtr-57544369-Reservations-test";
    private final String tablesTable = "cmtr-57544369-Tables-test";

    public APIGatewayProxyResponseEvent postReservations(APIGatewayProxyRequestEvent request) {
        System.out.println("Post Reservations");

        Gson gson = new Gson();
        Reservations reservationItem = gson.fromJson(request.getBody(), Reservations.class);
        int tableId = reservationItem.getTableNumber();
        System.out.println("TABLE ID : " + tableId);

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
        DynamoDB dynamoDB = new DynamoDB(client);
        System.out.println("DYNAMO DB : " + dynamoDB);

        Table table = dynamoDB.getTable(tablesTable);
        System.out.println("TABLE : " + table);
        Item item = table.getItem("id", tableId);
        System.out.println("ITEM : " + item);

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        if (item == null) {
            response.setStatusCode(400);
            return response;
        }

        DynamoDBMapper mapper = new DynamoDBMapper(client);
        String uuid = UUID.randomUUID().toString();
        reservationItem.setId(uuid);
        System.out.println("Saving Reservation ITEM");
        mapper.save(reservationItem);
        System.out.println("SAVED Reservation ITEM");
        Map<String, String> responseStruct = new HashMap<>();
        responseStruct.put("reservationsId", reservationItem.getId());
        response.setStatusCode(200);
        response.setBody(new Gson().toJson(responseStruct));
        return response;
    }

    public APIGatewayProxyResponseEvent getAllReservations() {
        AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.defaultClient();

        ScanRequest scanRequest = new ScanRequest()
                .withTableName(reservationTable);
        ScanResult result = ddb.scan(scanRequest);
        List<Reservations> reservations = new ArrayList<>();
        for (Map<String, AttributeValue> item : result.getItems()) {
            Reservations reservation = new Reservations();
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
