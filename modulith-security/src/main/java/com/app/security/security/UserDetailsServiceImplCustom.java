package com.app.security.security;

import com.app.security.repositories.UserRepo;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImplCustom implements UserDetailsService {

	private final UserRepo userRepo;

	public UserDetailsServiceImplCustom(UserRepo userRepo) {
		this.userRepo = userRepo;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return userRepo.findByEmail(username)
				.map(UserInfoConfig::new)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
	}
}