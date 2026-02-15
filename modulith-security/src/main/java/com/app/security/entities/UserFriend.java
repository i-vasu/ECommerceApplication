package com.app.security.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User friendship/connection entity for social features.
 * Supports bidirectional friendships.
 */
@Entity
@Table(name = "user_friends", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "friend_id"}),
       indexes = {
           @Index(name = "idx_user_friends_user", columnList = "user_id"),
           @Index(name = "idx_user_friends_friend", columnList = "friend_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFriend {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id", nullable = false)
    private User friend;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private FriendshipStatus status = FriendshipStatus.PENDING;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;
    
    public enum FriendshipStatus {
        PENDING,
        ACCEPTED,
        BLOCKED
    }

    // Manual Getters and Setters for compatibility with Java 25 issues
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getFriend() {
        return friend;
    }

    public void setFriend(User friend) {
        this.friend = friend;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public void setStatus(FriendshipStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
}
