package com.isec.platform.modules.notifications.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.modules.notifications.config.AfricasTalkingProperties;
import com.isec.platform.modules.notifications.dto.AfricasTalkingSmsResponse;
import com.isec.platform.modules.notifications.dto.SmsSendResult;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AfricasTalkingSmsClientTest {

    private MockWebServer mockWebServer;
    private AfricasTalkingSmsClient smsClient;
    private AfricasTalkingProperties properties;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        properties = new AfricasTalkingProperties();
        properties.setUsername("test-user");
        properties.setApiKey("test-api-key");
        properties.setFrom("12345");
        properties.setBaseUrl(mockWebServer.url("/").toString());

        smsClient = new AfricasTalkingSmsClient(properties, WebClient.builder());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void sendSms_Success() throws InterruptedException, IOException {
        // Prepare raw JSON response to verify @JsonProperty mapping
        String rawJson = "{" +
                "  \"SMSMessageData\": {" +
                "    \"Message\": \"Sent to 1/1\"," +
                "    \"Recipients\": [" +
                "      {" +
                "        \"cost\": \"KES 0.8000\"," +
                "        \"messageId\": \"ATXid_123\"," +
                "        \"number\": \"+254719531872\"," +
                "        \"status\": \"Success\"," +
                "        \"statusCode\": 101" +
                "      }" +
                "    ]" +
                "  }" +
                "}";

        mockWebServer.enqueue(new MockResponse()
                .setBody(rawJson)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // Execute
        StepVerifier.create(smsClient.sendSms("+254719531872", "Hello Jambo"))
                .assertNext(result -> {
                    assertTrue(result.isOverallSuccess());
                    assertEquals("Sent to 1/1", result.getSummaryMessage());
                    assertEquals(1, result.getRecipients().size());
                    assertEquals("ATXid_123", result.getRecipients().get(0).getMessageId());
                })
                .verifyComplete();

        // Verify request
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("test-api-key", recordedRequest.getHeader("apiKey"));
        assertEquals(MediaType.APPLICATION_FORM_URLENCODED_VALUE + ";charset=UTF-8", recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE));
        
        String body = recordedRequest.getBody().readUtf8();
        assertTrue(body.contains("username=test-user"));
        assertTrue(body.contains("to=%2B254719531872"));
        assertTrue(body.contains("message=Hello+Jambo"));
        assertTrue(body.contains("from=12345"));
    }

    @Test
    void sendSms_Failure_StatusCode403() throws InterruptedException, IOException {
        // Prepare mock response with error status code
        AfricasTalkingSmsResponse.Recipient recipient = new AfricasTalkingSmsResponse.Recipient(
                "+254719531872", "KES 0.0000", "InvalidPhoneNumber", 403, "None");
        AfricasTalkingSmsResponse.SMSMessageData smsData = new AfricasTalkingSmsResponse.SMSMessageData(
                "Sent to 0/1", List.of(recipient));
        AfricasTalkingSmsResponse mockResponse = new AfricasTalkingSmsResponse(smsData);

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(mockResponse))
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // Execute
        StepVerifier.create(smsClient.sendSms("+254719531872", "Hello Jambo"))
                .assertNext(result -> {
                    assertFalse(result.isOverallSuccess());
                    assertEquals(403, result.getRecipients().get(0).getStatusCode());
                })
                .verifyComplete();
    }
}
