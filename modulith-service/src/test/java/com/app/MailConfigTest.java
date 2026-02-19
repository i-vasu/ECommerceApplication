package com.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class MailConfigTest {

    @Autowired
    private JavaMailSenderImpl mailSender;

    @Test
    public void testMailConfig() {
        assertThat(mailSender.getHost()).isEqualTo("smtp.gmail.com");
        assertThat(mailSender.getUsername()).isEqualTo("noreply@example.com");
        // Check if password is null or "default_password"
        System.out.println("[DEBUG_LOG] Mail Password: " + mailSender.getPassword());
    }
}
