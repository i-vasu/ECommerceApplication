package com.app.identity.security;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.app.identity.entities.User;

public class UserInfoConfig implements UserDetails {

	private static final long serialVersionUID = 1L;

	private String email;
	private String password;
	private List<GrantedAuthority> authorities;

	public UserInfoConfig() {
	}

	public UserInfoConfig(String email, String password, List<GrantedAuthority> authorities) {
		this.email = email;
		this.password = password;
		this.authorities = authorities;
	}

	public UserInfoConfig(User user) {
		this.email = user.getEmail();
		this.password = user.getPassword();
		this.authorities = user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getRoleName()))
				.collect(Collectors.toList());
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setAuthorities(List<GrantedAuthority> authorities) {
		this.authorities = authorities;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
