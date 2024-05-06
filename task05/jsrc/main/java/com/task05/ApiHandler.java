package com.task05;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

@LambdaHandler(
		lambdaName = "api_handler", 
		roleName = "api_handler-role", 
		isPublishVersion = false, 
		logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
	)

public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private final AmazonDynamoDB dynamoDBClient = AmazonDynamoDBClientBuilder.defaultClient();
	private final DynamoDB dynamoDB = new DynamoDB(dynamoDBClient);
	private final String tableName = "Events";

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
		APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			Map<String, Object> requestBody = objectMapper.readValue(input.getBody(), Map.class);

			int principalId = (int) requestBody.get("principalId");
			Map<String, String> content = (Map<String, String>) requestBody.get("content");

			String eventId = UUID.randomUUID().toString();
			String createdAt = Instant.now().toString();

			Table table = dynamoDB.getTable(tableName);
			Item item = new Item().withPrimaryKey("id", eventId).withInt("principalId", principalId)
					.withString("createdAt", createdAt).withMap("body", content);

			table.putItem(new PutItemSpec().withItem(item));

			response.setStatusCode(201);
			response.setBody(objectMapper.writeValueAsString(
					Map.of("id", eventId, "principalId", principalId, "createdAt", createdAt, "body", content)));
		} catch (JsonProcessingException e) {
			response.setStatusCode(400);
			response.setBody("Bad Request: Invalid JSON format");
		} catch (Exception e) {
			response.setStatusCode(500);
			response.setBody("Internal Server Error");
		}
		return response;
	}
}
