package com.app.catalog.listeners;

import com.app.catalog.repositories.WishlistRepo;
import com.app.core.events.user.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final WishlistRepo wishlistRepo;

    @EventListener
    @Transactional
    public void handleUserDeleted(UserDeletedEvent event) {
        wishlistRepo.deleteByUserId(event.getUserId());
    }
}
