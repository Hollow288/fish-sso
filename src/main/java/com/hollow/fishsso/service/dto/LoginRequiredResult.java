package com.hollow.fishsso.service.dto;

/**
 * 需要登录结果DTO
 * @param loginUrl 登录URL
 */
public record LoginRequiredResult(String loginUrl) {
}