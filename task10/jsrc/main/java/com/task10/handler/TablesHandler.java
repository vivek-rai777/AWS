package com.task10.handler;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.task10.pojo.Tables;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.util.*;
import java.util.stream.Collectors;

public class TablesHandler {
    private final DynamoDbTable<Tables> tablesDynamoDbTable;

    public TablesHandler(DynamoDbTable<Tables> tablesDynamoDbTable) {
        this.tablesDynamoDbTable = tablesDynamoDbTable;
    }

    public APIGatewayProxyResponseEvent getAllTables() {

        List<Tables> tables = tablesDynamoDbTable.scan().items().stream().collect(Collectors.toList());

        Map<String, List<Tables>> map = new HashMap<>();
        map.put("tables", tables);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setBody(new Gson().toJson(map));
        return response;
    }

    public APIGatewayProxyResponseEvent postTables(APIGatewayProxyRequestEvent request) {

        Gson gson = new Gson();
        Tables tableItem = gson.fromJson(request.getBody(), Tables.class);

        tablesDynamoDbTable.putItem(tableItem);

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setBody("{\"id\": " + tableItem.getId() + "}");
        return response;
    }

    public APIGatewayProxyResponseEvent getTables(APIGatewayProxyRequestEvent event) {
        int tableId = Integer.parseInt(event.getPathParameters().get("tableId"));
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        Optional<Tables> table = tablesDynamoDbTable.scan().items().stream().filter(x -> x.getId()==tableId).findFirst();

        if(table.isPresent()) {
            response.setStatusCode(200);
            response.setBody(new Gson().toJson(table));
        }
        else {
            response.setStatusCode(400);
        }
        return response;
    }
}
