package com.task05;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

@LambdaHandler(lambdaName = "api_handler", roleName = "api_handler-role", isPublishVersion = false, logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED)

public class ApiHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

	private final Table eventsTable;

	public ApiHandler() {
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
		DynamoDB dynamoDB = new DynamoDB(client);
		eventsTable = dynamoDB.getTable("cmtr-57544369-Events-test");
	}

	public Map<String, Object> handleRequest(Map<String, Object> request, Context context) {
		try {
			int principalId = (int) request.get("principalId");
			Map<String, String> content = (Map<String, String>) request.get("content");

			Item eventItem = new Item().withPrimaryKey("id", UUID.randomUUID().toString())
					.withInt("principalId", principalId).withString("createdAt", LocalDateTime.now().toString())
					.withMap("body", content);

			eventsTable.putItem(eventItem);

			Map<String, Object> response = new HashMap<>();
			response.put("statusCode", 201);
			response.put("event", eventItem.asMap());

			return response;
		} catch (Exception e) {
			System.out.println(e.toString());
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("statusCode", 500);
			errorResponse.put("error", "Internal Server Error");
			return errorResponse;
		}
	}
}
