package edu.sjsu.wildstore;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

public class ClientTest {

    @Test
    void testGetWebClient() {
        WebClient client = Client.getWebClient("http://localhost:8080");
        Assertions.assertNotNull(client);

        WebClient clientWithToken = Client.getWebClient("http://localhost:8080", "test-token");
        Assertions.assertNotNull(clientWithToken);
    }
}