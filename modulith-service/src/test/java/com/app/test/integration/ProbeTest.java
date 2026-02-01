package com.app.test.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProbeTest {
    @Autowired
    private WebApplicationContext context;

    @Test
    void test() {
        RestTestClient client = RestTestClient.bindToApplicationContext(context).build();
        System.out.println("RestTestClient created: " + client);
    }
}
