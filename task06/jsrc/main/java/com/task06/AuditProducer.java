package com.task06;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.StreamRecord;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;

import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

import java.time.LocalDateTime;
import java.util.*;

@LambdaHandler(lambdaName = "audit_producer",
	roleName = "audit_producer-role",
	isPublishVersion = false,
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@DynamoDbTriggerEventSource(batchSize = 3,targetTable = "Configuration")
public class AuditProducer implements RequestHandler<DynamodbEvent, Void> {

	private final DynamoDB dynamoDB;
	private final Table auditTable;

	public AuditProducer() {
		this.dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());
		this.auditTable = dynamoDB.getTable("cmtr-57544369-Audit");
	}

	@Override
	public Void handleRequest(DynamodbEvent event, Context context) {
		for (DynamodbEvent.DynamodbStreamRecord record : event.getRecords()) {
			com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord streamRecord = record.getDynamodb();
			if ("INSERT".equals(record.getEventName())) {
				handleInsertEvent(streamRecord);
			} else {
				handleModifyEvent(streamRecord);
			}
		}
		return null;
	}

	private void handleInsertEvent(com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord streamRecord) {
		String itemId = streamRecord.getKeys().get("key").getS();
		Map<String, Object> newValue = getNewValue(streamRecord);
		String modificationTime = LocalDateTime.now().toString();

		createAuditEntry(itemId, modificationTime, newValue);
	}

	private void handleModifyEvent(com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord streamRecord) {
		System.out.println("Entering modify Event");
		String itemId = streamRecord.getKeys().get("key").getS();
		Map<String, Object> oldValue = getOldValue(streamRecord);
		Map<String, Object> newValue = getNewValue(streamRecord);
		String modificationTime = LocalDateTime.now().toString();

		/*// Extract updated attributes
		Set<String> updatedAttributes = new HashSet<>(newValue.keySet());
		updatedAttributes.removeAll(oldValue.keySet());

		// Handle each updated attribute, it supposes that only one field would be updated each time,
		// please revise according to your own logic
		System.out.println("Invoking AuditEntry");
		for (String updatedAttribute: updatedAttributes) {
			createAuditEntry(itemId, modificationTime, oldValue, newValue, updatedAttribute);
		}*/
		// Extract updated attributes
		Set<String> updatedAttributes = new HashSet<>();
		for (Map.Entry<String, Object> entry: newValue.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (!oldValue.containsKey(key) || !oldValue.get(key).equals(value)) {
				updatedAttributes.add(key);
			}
		}

		// Handle each updated attribute
		System.out.println("Invoking AuditEntry");
		for (String updatedAttribute: updatedAttributes) {
			createAuditEntry(itemId, modificationTime, oldValue, newValue, updatedAttribute);
		}
	}


	/*private void handleModifyEvent(com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord streamRecord) {
		String itemId = streamRecord.getKeys().get("key").getS();
		Map<String, Object> oldValue = getOldValue(streamRecord);
		Map<String, Object> newValue = getNewValue(streamRecord);
		String modificationTime = LocalDateTime.now().toString();

		createAuditEntry(itemId, modificationTime, oldValue, newValue);
	}*/

	private Map<String, Object> getOldValue(com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord streamRecord) {
		Map<String, com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue> imageMap = streamRecord.getOldImage();
		Map<String, Object> oldValues = new HashMap<>();

		imageMap.forEach((key, attrValue) -> {
			if (attrValue.getS() != null) {
				oldValues.put(key, attrValue.getS());
			} else if (attrValue.getN() != null) {
				try {
					oldValues.put(key, Integer.parseInt(attrValue.getN()));
				} catch (NumberFormatException e) {
					oldValues.put(key, Double.parseDouble(attrValue.getN()));
				}
			}
		});

		return oldValues;
	}

	private Map<String, Object> getNewValue(com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord streamRecord) {
		Map<String, com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue> imageMap = streamRecord.getNewImage();
		Map<String, Object> newValues = new HashMap<>();

		imageMap.forEach((key, attrValue) -> {
			if (attrValue.getS() != null) {
				newValues.put(key, attrValue.getS());
			} else if (attrValue.getN() != null) {
				try {
					newValues.put(key, Integer.parseInt(attrValue.getN()));
				} catch (NumberFormatException e) {
					newValues.put(key, Double.parseDouble(attrValue.getN()));
				}
			}
		});

		return newValues;
	}

	private void createAuditEntry(String itemId, String modificationTime, Map<String, Object> newValue) {
		Item auditEntry = new Item()
				.withString("id", UUID.randomUUID().toString())
				.withString("itemKey", itemId)
				.withString("modificationTime", modificationTime)
				.withMap("newValue", newValue);

		PutItemSpec putItemSpec = new PutItemSpec().withItem(auditEntry);
		auditTable.putItem(putItemSpec);
	}

	/*private void createAuditEntry(String itemId, String modificationTime, Map<String, Object> oldValue, Map<String, Object> newValue) {
		Map<String, Object> auditEntryMap = new HashMap<>();
		auditEntryMap.put("id", UUID.randomUUID().toString());
		auditEntryMap.put("itemKey", itemId);
		auditEntryMap.put("modificationTime", modificationTime);
		auditEntryMap.put("oldValue", oldValue);
		auditEntryMap.put("newValue", newValue);

		Item auditEntry = Item.fromMap(auditEntryMap);
		PutItemSpec putItemSpec = new PutItemSpec().withItem(auditEntry);
		auditTable.putItem(putItemSpec);
	}*/

	private void createAuditEntry(String itemId, String modificationTime, Map<String, Object> oldValue, Map<String, Object> newValue, String updatedAttribute) {
		System.out.println("Entering createAudit");
		Map<String, Object> auditEntryMap = new HashMap<>();
		auditEntryMap.put("id", UUID.randomUUID().toString());
		auditEntryMap.put("itemKey", itemId);
		auditEntryMap.put("modificationTime", modificationTime);
		auditEntryMap.put("oldValue", oldValue.get(updatedAttribute));
		auditEntryMap.put("newValue", newValue.get(updatedAttribute));
		auditEntryMap.put("updatedAttribute", updatedAttribute);

		System.out.println("Creating a Item");
		Item auditEntry = Item.fromMap(auditEntryMap);
		PutItemSpec putItemSpec = new PutItemSpec().withItem(auditEntry);
		try {
			System.out.println("Put Item");
			auditTable.putItem(putItemSpec);
		} catch (Exception e) {
			//Handle Exception
			System.out.println(e.getMessage().toString());
			e.printStackTrace();

		}
	}
}
