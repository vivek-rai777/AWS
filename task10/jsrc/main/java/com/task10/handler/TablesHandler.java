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
import com.task10.pojo.Tables;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class TablesHandler {
    public APIGatewayProxyResponseEvent getAllTables() {
        System.out.println("Get All Tables");
        AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.defaultClient();
        String tableName = "cmtr-57544369-Tables-test";
        ScanRequest scanRequest = new ScanRequest()
                .withTableName(tableName);
        ScanResult result = ddb.scan(scanRequest);
        List<Tables> tables = new ArrayList<>();
        for (Map<String, AttributeValue> item : result.getItems()){
            System.out.println(item);
            Tables table = new Tables();
            table.setId(Integer.parseInt(item.get("id").getN()));
            System.out.println(item.get("id"));
            table.setNumber(Integer.parseInt(item.get("number").getN()));
            System.out.println(item.get("number"));
            table.setPlaces(Integer.parseInt(item.get("places").getN()));
            System.out.println(item.get("places"));
            System.out.println("Getting the vip attribute" + item.get("vip"));
            System.out.println("Getting the ISVIP attribute" + item.get("isVip"));
            System.out.println(item.get("vip")+"alsdfasdf");
            table.setVip(Boolean.valueOf(String.valueOf(item.get("vip").getN())));
            System.out.println(item.get("vip"));
            if (item.containsKey("minOrder")) {
                table.setMinOrder(Integer.parseInt(item.get("minOrder").getN()));
            }
            System.out.println(tables.add(table));
        }
        System.out.println(tables);
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setBody(new Gson().toJson(tables));
        System.out.println("RETURNING:"+ response);
        return response;
    }
    public APIGatewayProxyResponseEvent postTables(APIGatewayProxyRequestEvent request) {
        System.out.println("Post Tables");
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
        DynamoDBMapper mapper = new DynamoDBMapper(client);
        System.out.println("Mapper created successfully");
        Gson gson = new Gson();
        Tables tableItem = gson.fromJson(request.getBody(), Tables.class);
        System.out.println(tableItem.toString());
        System.out.println("Saving Tables");
        mapper.save(tableItem);
        System.out.println("Returning");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setBody("{\"id\": " + tableItem.getId() + "}");
        return response;
    }
}
