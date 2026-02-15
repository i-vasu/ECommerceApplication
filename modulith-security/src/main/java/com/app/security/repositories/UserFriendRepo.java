package com.app.security.repositories;

import com.app.security.entities.UserFriend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFriendRepo extends JpaRepository<UserFriend, Long> {
    
    @Query("SELECT uf FROM UserFriend uf WHERE uf.user.userId = ?1 AND uf.friend.userId = ?2")
    Optional<UserFriend> findByUserIdAndFriendId(Long userId, Long friendId);
    
    @Query("SELECT uf FROM UserFriend uf WHERE uf.user.userId = ?1 AND uf.status = 'ACCEPTED'")
    List<UserFriend> findAcceptedFriendsByUserId(Long userId);
    
    @Query("SELECT uf FROM UserFriend uf WHERE uf.friend.userId = ?1 AND uf.status = 'PENDING'")
    List<UserFriend> findPendingFriendRequestsForUser(Long userId);
    
    @Query("SELECT COUNT(uf) > 0 FROM UserFriend uf WHERE " +
           "((uf.user.userId = ?1 AND uf.friend.userId = ?2) OR " +
           "(uf.user.userId = ?2 AND uf.friend.userId = ?1)) AND " +
           "uf.status = 'ACCEPTED'")
    boolean areFriends(Long userId1, Long userId2);
}
