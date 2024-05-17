package com.task10;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.syndicate.deployment.annotations.environment.EnvironmentVariable;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.RetentionSetting;
import com.task10.handler.ReservationsHandler;
import com.task10.handler.TablesHandler;
import com.task10.pojo.Reservations;
import com.task10.pojo.Tables;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@LambdaHandler(lambdaName = "api_handler",
        roleName = "api_handler-role",
        isPublishVersion = false,
        logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@EnvironmentVariable(key = "TABLES_DYNAMODB_TABLE", value = "${tables_table}")
@EnvironmentVariable(key = "RESERVATION_DYNAMODB_TABLE", value = "${reservations_table}")
@EnvironmentVariable(key = "BOOKING_USERPOOL", value = "${booking_userpool}")
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final CognitoIdentityProviderClient cognitoClient;
    private static final String USER_POOL_ID = getUserPoolId();
    private static final String APP_CLIENT_ID = getClientId();
    private final String reservationTable = System.getenv("RESERVATION_DYNAMODB_TABLE");
    private final String tablesTable = System.getenv("TABLES_DYNAMODB_TABLE");
    DynamoDbEnhancedClient dynamoDbEnhancedClient = DynamoDbEnhancedClient.create();
    DynamoDbTable<Reservations> reservationsDynamoDbTable = dynamoDbEnhancedClient.table(reservationTable, TableSchema.fromBean(Reservations.class));
    DynamoDbTable<Tables> tablesDynamoDbTable = dynamoDbEnhancedClient.table(tablesTable, TableSchema.fromBean(Tables.class));

    public ApiHandler() {
        cognitoClient = CognitoIdentityProviderClient.create();
    }

    public static String getUserPoolId() {

        CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.create();

        ListUserPoolsRequest listUserPoolsRequest = ListUserPoolsRequest.builder().maxResults(10).build();
        ListUserPoolsResponse listUserPoolsResponse = cognitoClient.listUserPools(listUserPoolsRequest);

        String userPoolId = listUserPoolsResponse.userPools().get(0).id();

        for (UserPoolDescriptionType userPool : listUserPoolsResponse.userPools()) {
            String poolName = System.getenv("BOOKING_USERPOOL");
            if (userPool.name().equals(poolName)) {
                userPoolId = userPool.id();
                System.out.println("User Pool Id is: " + userPoolId);
                break;
            }
        }
        return userPoolId;
    }


    public static String getClientId() {

        CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.create();

        ListUserPoolClientsRequest listUserPoolClientsRequest = ListUserPoolClientsRequest.builder()
                .userPoolId(USER_POOL_ID).maxResults(10).build();
        ListUserPoolClientsResponse listUserPoolClientsResponse = cognitoClient
                .listUserPoolClients(listUserPoolClientsRequest);

        String clientId = "";
        for (UserPoolClientDescription userPoolClient : listUserPoolClientsResponse.userPoolClients()) {
            if (userPoolClient.clientName().equals("client-app")) {
                clientId = userPoolClient.clientId();
                System.out.println("Client Id is: " + clientId);
                break;
            }
        }
        return clientId;
    }

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            switch (event.getResource()) {
                case "/signup":
                    return handleSignUp(event);
                case "/signin":
                    return handleSignIn(event);
                case "/tables":
                    return handleTables(event);
                case "/reservations":
                    return handleReservations(event);
                default:
                    return handleTables(event);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return new APIGatewayProxyResponseEvent().withStatusCode(400);
    }

    public APIGatewayProxyResponseEvent handleSignUp(APIGatewayProxyRequestEvent event) {
        System.out.println("Entering try");
        try {
            System.out.println("Parsing request");
            Map<String, String> input = new Gson().fromJson(event.getBody(), HashMap.class);
            System.out.println("request body is parsed");
            String firstName = input.get("firstName");
            String lastName = input.get("lastName");
            String email = input.get("email");
            String password = input.get("password");
            // Validate the inputs here! (Check the email format and password rules)
            System.out.println("Creating user_request");
            AdminCreateUserRequest user_request = AdminCreateUserRequest.builder()
                    .userPoolId(USER_POOL_ID)
                    .username(email)
                    .userAttributes(
                            AttributeType.builder()
                                    .name("email")
                                    .value(email)
                                    .build())
                    .messageAction("SUPPRESS")
                    .build();

            System.out.println("Cognito-Client creating user_request");
            cognitoClient.adminCreateUser(user_request);
            System.out.println("Setting password");
            //Set a user's password in Cognito User Pool
            AdminSetUserPasswordRequest passwordRequest = AdminSetUserPasswordRequest.builder()
                    .username(email)
                    .password(password)
                    .userPoolId(USER_POOL_ID)
                    .permanent(true)
                    .build();
            cognitoClient.adminSetUserPassword(passwordRequest);
            System.out.println("Returning");
            return new APIGatewayProxyResponseEvent().withStatusCode(200);
        } catch (Exception e) {
            System.out.println(e);
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Failed to sign up!");
        }
    }

    public APIGatewayProxyResponseEvent handleSignIn(APIGatewayProxyRequestEvent request) {
        Map<String, String> input = new Gson().fromJson(request.getBody(), HashMap.class);
        System.out.println("Handling sign in");
        CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder().region(Region.EU_CENTRAL_1).build();
        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", input.get("email"));
        authParameters.put("PASSWORD", input.get("password"));
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        System.out.println(authParameters);
        System.out.println("Preparing Authrequest");
        InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .authParameters(authParameters)
                .clientId(APP_CLIENT_ID)
                .build();
        try {
            System.out.println("Initiating Authrequest");
            System.out.println(authRequest);
            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);
            System.out.println(authResponse);
            response.setBody("{ \"accessToken\": \"" + authResponse.authenticationResult().accessToken() + "\"}");
            response.setStatusCode(200);
        } catch (NotAuthorizedException e) {
            System.out.println(e);
            response.setStatusCode(400);
        } catch (UserNotFoundException e) {
            System.out.println(e);
            response.setStatusCode(400);
        } catch (Exception e) {
            System.out.println(e);
            response.setStatusCode(400);
        }
        System.out.println("Returning " + response);
        return response;
    }

    private APIGatewayProxyResponseEvent handleReservations(APIGatewayProxyRequestEvent event) {
        if (event.getHttpMethod().equalsIgnoreCase("GET")) {
            return new ReservationsHandler(reservationsDynamoDbTable, tablesDynamoDbTable).getAllReservations();
        } else if (event.getHttpMethod().equalsIgnoreCase("POST")) {
            return new ReservationsHandler(reservationsDynamoDbTable, tablesDynamoDbTable).postReservations(event);
        }
        return new APIGatewayProxyResponseEvent().withStatusCode(400);
    }

    private APIGatewayProxyResponseEvent handleTables(APIGatewayProxyRequestEvent event) {
        System.out.println(event);
        System.out.println(event.getPathParameters());
        if (Objects.nonNull(event.getPathParameters())) {
            System.out.println("Inside First If API Handler");
            return new TablesHandler(tablesDynamoDbTable).getTables(event);
        } else if (event.getHttpMethod().equalsIgnoreCase("GET")) {
            return new TablesHandler(tablesDynamoDbTable).getAllTables();
        } else if (event.getHttpMethod().equalsIgnoreCase("POST")) {
            return new TablesHandler(tablesDynamoDbTable).postTables(event);
        }
        return new APIGatewayProxyResponseEvent().withStatusCode(400);
    }
}

