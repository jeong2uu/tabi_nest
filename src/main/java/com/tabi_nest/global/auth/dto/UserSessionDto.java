package com.tabi_nest.global.auth.dto;

import lombok.Data;

@Data
public class UserSessionDto {
    private Long id;
    private String email;
    private String name;
    private String role;

    public UserSessionDto() {}

    public UserSessionDto(Long id, String email, String name, String role) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
    }

}
