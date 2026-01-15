package com.store.user.dto;

import com.store.user.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String password;
    private Role role;
    private Boolean active;
    private LocalDateTime createdAt;
}
