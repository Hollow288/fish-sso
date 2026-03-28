package com.hollow.fishsso.controller;

import com.hollow.fishsso.controller.dto.ResetPasswordRequest;
import com.hollow.fishsso.controller.dto.SendResetCodeRequest;
import com.hollow.fishsso.service.PasswordResetService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 密码管理控制器
 */
@RestController
@RequestMapping("/sso/password")
public class PasswordController {

    private final PasswordResetService passwordResetService;

    /**
     * 构造函数
     * @param passwordResetService 密码重置服务
     */
    public PasswordController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /**
     * 发送密码重置验证码
     * @param request 请求体（用户名、邮箱）
     * @return 统一成功响应
     */
    @PostMapping("/reset-code")
    public ResponseEntity<Map<String, String>> sendResetCode(@RequestBody SendResetCodeRequest request) {
        passwordResetService.sendResetCode(request.username(), request.email());
        return ResponseEntity.ok(Map.of("message", "如果用户名和邮箱匹配，验证码已发送至您的邮箱"));
    }

    /**
     * 重置密码
     * @param request 请求体（用户名、新密码、验证码）
     * @return 统一成功响应
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.username(), request.newPassword(), request.code());
        return ResponseEntity.ok(Map.of("message", "密码重置成功"));
    }
}
