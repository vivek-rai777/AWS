package com.task07;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.json.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.syndicate.deployment.annotations.events.RuleEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

@LambdaHandler(lambdaName = "uuid_generator", roleName = "uuid_generator-role", isPublishVersion = false, logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED)

@RuleEventSource(targetRule = "uuid_trigger")
public class UuidGenerator implements RequestHandler<Object, String> {

	private static final String BUCKET_NAME = "cmtr-57544369-uuid-storage-test";

	@Override
	public String handleRequest(Object input, Context context) {
		List<String> uuids = generateUUIDs(10);

		String executionTime = LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

		JSONObject jsonContent = new JSONObject();
		jsonContent.put("ids", uuids);

		String fileName = executionTime + ".json";

		writeToS3(fileName, jsonContent.toString());

		return "UUIDs generated and stored in S3";
	}

	private List<String> generateUUIDs(int count) {
		List<String> uuids = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			uuids.add(UUID.randomUUID().toString());
		}
		return uuids;
	}

	private void writeToS3(String fileName, String content) {
		InputStream inputStream = new ByteArrayInputStream(content.getBytes());

		AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentType("application/json");

		s3Client.putObject(BUCKET_NAME, fileName, inputStream, metadata);
	}
}
