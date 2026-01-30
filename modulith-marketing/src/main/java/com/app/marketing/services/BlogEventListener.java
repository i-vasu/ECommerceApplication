package com.app.marketing.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;
import com.app.security.repositories.UserRepo;
import com.app.security.entities.User;

@Service
public class BlogEventListener implements StreamListener<String, MapRecord<String, String, String>> {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private MarketingService marketingService;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        Map<String, String> value = message.getValue();
        String blogTitle = value.get("title");
        String blogContent = value.get("content");

        System.out.println("Processing Blog Broadcast Event: " + blogTitle);

        // Broadcast to all users
        // Note: In a real system, you might paginate this or push to a separate email
        // worker queue to avoid memory issues with large datasets.
        List<User> users = userRepo.findAll();

        for (User user : users) {
            try {
                Map<String, Object> vars = new HashMap<>();
                vars.put("firstName", user.getFirstName());
                vars.put("blogTitle", blogTitle);
                vars.put("blogContent", blogContent);

                marketingService.sendCampaignEmail("New Blog: " + blogTitle, user.getEmail(), "New Post: " + blogTitle,
                        "blog-newsletter", vars);
            } catch (Exception e) {
                System.err.println("Failed to send blog email to: " + user.getEmail());
            }
        }
    }
}
