package com.hollow.fishsso.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    /**
     * 构造函数
     * @param mailSender 邮件发送器
     */
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 发送密码重置验证码邮件
     * @param to 收件人邮箱
     * @param code 验证码
     */
    public void sendResetCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Fish SSO - 密码重置验证码");
        message.setText("您的密码重置验证码是: " + code + "\n\n该验证码5分钟内有效，请勿将验证码告知他人。\n\n如果这不是您本人的操作，请忽略此邮件。");
        mailSender.send(message);
        log.info("密码重置验证码邮件已发送至: {}", to);
    }
}
