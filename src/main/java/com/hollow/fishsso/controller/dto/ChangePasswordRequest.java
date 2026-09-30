package com.hollow.fishsso.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 登录后修改密码的请求。 */
public record ChangePasswordRequest(@JsonProperty("current_password") String currentPassword,
                                    @JsonProperty("new_password") String newPassword) {
}
