package com.task09;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.TracingMode;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;

@LambdaHandler(lambdaName = "processor",
        roleName = "processor-role",
        isPublishVersion = false,
		tracingMode = TracingMode.Active,
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)

@LambdaUrlConfig(authType = AuthType.NONE, invokeMode = InvokeMode.BUFFERED)
@DynamoDbTriggerEventSource(batchSize = 10, targetTable = "Weather")
public class Processor implements RequestHandler<Object, Map<String, Object>> {

    public Map<String, Object> handleRequest(Object request, Context context) {
        OpenMeteoAPI openMeteoObj = new OpenMeteoAPI();
        String weather = null;

        weather = openMeteoObj.getWeatherForecast();

        AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClientBuilder.standard().build();
        DynamoDB dynamoDB = new DynamoDB(dynamoDBClient);

        Table table = dynamoDB.getTable("cmtr-57544369-Weather-test");

        String uuid = UUID.randomUUID().toString();
        Item item = new Item().withPrimaryKey("id", uuid).withJSON("forecast", weather);
        table.putItem(item);

        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put("statusCode", 200);
        resultMap.put("body", weather);
        return resultMap;
    }
}