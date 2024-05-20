package com.task11.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.task11.pojo.Reservations;
import com.task11.pojo.Tables;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.util.*;
import java.util.stream.Collectors;

public class ReservationsHandler {
    private final DynamoDbTable<Reservations> reservationsDynamoDbTable;
    private final DynamoDbTable<Tables> tablesDynamoDbTable;

    public ReservationsHandler(DynamoDbTable<Reservations> reservationsDynamoDbTable, DynamoDbTable<Tables> tablesDynamoDbTable) {
        this.reservationsDynamoDbTable = reservationsDynamoDbTable;
        this.tablesDynamoDbTable = tablesDynamoDbTable;
    }

    public APIGatewayProxyResponseEvent postReservations(APIGatewayProxyRequestEvent request) {
        System.out.println("Post Reservations");
        Gson gson = new Gson();
        Reservations reservationItem = gson.fromJson(request.getBody(), Reservations.class);
        System.out.println(reservationItem);
        int tableNumber = reservationItem.getTableNumber();

        long countTableNumber = tablesDynamoDbTable.scan().items().stream()
                .filter(x -> x.getNumber() == tableNumber)
                .count();

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        if (countTableNumber ==  0) {
            response.setStatusCode(400);
            return response;
        }

        Comparator<String> c = Comparator.comparing(String::toString);

        long count = reservationsDynamoDbTable.scan().items().stream()
                .filter(x -> x.getDate().equals(reservationItem.getDate()))
                .filter(x -> x.getTableNumber() == reservationItem.getTableNumber())
                .filter(x-> c.compare(x.getSlotTimeStart(), reservationItem.getSlotTimeStart()) <= 0 &&
                        c.compare(reservationItem.getSlotTimeStart(), x.getSlotTimeEnd()) <= 0).count();
        System.out.println("Count : " + count);

        if (count > 0) {
            response.setStatusCode(400);
            return response;
        }

        String uuid = UUID.randomUUID().toString();
        reservationItem.setId(uuid);
        System.out.println("UUID : " + uuid);
        reservationsDynamoDbTable.putItem(reservationItem);

        Map<String, String> responseStruct = new HashMap<>();
        responseStruct.put("reservationId", reservationItem.getId());
        response.setStatusCode(200);
        response.setBody(new Gson().toJson(responseStruct));
        return response;
    }

    public APIGatewayProxyResponseEvent getAllReservations() {

        List<Reservations> reservations = reservationsDynamoDbTable.scan().items().stream().collect(Collectors.toList());

        Map<String, List<Reservations>> responseStruct = new HashMap<>();
        responseStruct.put("reservations", reservations);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setBody(new Gson().toJson(responseStruct));
        return response;
    }
}
