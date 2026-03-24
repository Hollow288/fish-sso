package com.hollow.fishsso.controller.dto;

/**
 * 重定向响应DTO
 * @param redirect_url 重定向URL
 */
public record RedirectResponse(String redirect_url) {
}