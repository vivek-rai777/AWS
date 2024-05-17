package com.task10.handler;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.task10.pojo.Tables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TablesHandler {

    private final String tableName = "cmtr-57544369-Tables-test";

    public APIGatewayProxyResponseEvent getAllTables() {
        System.out.println("Get All Tables");
        AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.defaultClient();

        ScanRequest scanRequest = new ScanRequest()
                .withTableName(tableName);
        ScanResult result = ddb.scan(scanRequest);

        List<Tables> tables = new ArrayList<>();
        for (Map<String, AttributeValue> item : result.getItems()) {
            System.out.println(item);
            Tables table = new Tables();
            table.setId(Integer.parseInt(item.get("id").getN()));
            table.setNumber(Integer.parseInt(item.get("number").getN()));
            table.setPlaces(Integer.parseInt(item.get("places").getN()));

            System.out.println("Getting the vip attribute" + item.get("vip"));
            System.out.println("Getting the ISVIP attribute" + item.get("isVip"));
            System.out.println(item.get("vip") + "alsdfasdf");

            table.setVip(Boolean.valueOf(String.valueOf(item.get("vip").getN())));
            if (item.containsKey("minOrder")) {
                table.setMinOrder(Integer.parseInt(item.get("minOrder").getN()));
            }
            System.out.println(tables.add(table));
        }
        System.out.println(tables);
        Map<String, List<Tables>> map = new HashMap<>();
        map.put("tables", tables);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setBody(new Gson().toJson(map));
        return response;
    }

    public APIGatewayProxyResponseEvent postTables(APIGatewayProxyRequestEvent request) {
        System.out.println("Post Tables");
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
        DynamoDBMapper mapper = new DynamoDBMapper(client);
        System.out.println("Mapper created successfully");
        Gson gson = new Gson();
        Tables tableItem = gson.fromJson(request.getBody(), Tables.class);
        mapper.save(tableItem);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setBody("{\"id\": " + tableItem.getId() + "}");
        return response;
    }

    public APIGatewayProxyResponseEvent getTables(APIGatewayProxyRequestEvent event) {
        String tableId = event.getPathParameters().get("tableId");
        System.out.println("Getting Table with Id" + tableId);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.defaultClient();
            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(tableName);
            ScanResult result = ddb.scan(scanRequest);
            System.out.println("~~~~~~~~~~~~~~~~ RESULT : " + result);
            for (Map<String, AttributeValue> item : result.getItems()) {
                System.out.println("ITEM : " + item);
                if (item.get("id").getN().equals(tableId)) {
                    Tables table = new Tables();
                    table.setId(Integer.parseInt(item.get("id").getN()));
                    table.setNumber(Integer.parseInt(item.get("number").getN()));
                    table.setPlaces(Integer.parseInt(item.get("places").getN()));

                    table.setVip(Boolean.valueOf(String.valueOf(item.get("vip").getN())));
                    if (item.containsKey("minOrder")) {
                        table.setMinOrder(Integer.parseInt(item.get("minOrder").getN()));
                    }
                    response.setStatusCode(200);
                    response.setBody(new Gson().toJson(table));
                    System.out.println("RESPONSE : " + response);
                    return response;
                }
            }
        } catch (Exception e) {
            response.setStatusCode(400);
            System.out.println(e);
        }
        return response;
    }
}
