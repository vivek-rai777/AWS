package com.task07;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.gson.Gson;
import com.syndicate.deployment.annotations.events.RuleEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;

@LambdaHandler(lambdaName = "uuid_generator",
        roleName = "uuid_generator-role",
        isPublishVersion = false,
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@RuleEventSource(targetRule = "uuid_trigger")
public class UuidGenerator implements RequestHandler<Object, String> {
    private AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();

    public String handleRequest(Object request, Context context) {
        String bucket = "cmtr-57544369-uuid-storage";
        String key = Instant.now().toString();
        Map<String, Object> data = new HashMap<>();
        data.put("ids", IntStream.range(0, 10)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList()));
        Gson gson = new Gson();
        // Upload to S3
        InputStream stream = new ByteArrayInputStream(gson.toJson(data).getBytes(StandardCharsets.UTF_8));
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(gson.toJson(data).length());
        context.getLogger().log("Writing to: " + bucket + "/" + key);
        s3.putObject(new PutObjectRequest(bucket, key, stream, metadata));
        return "UUID written to S3 bucket";
    }
}