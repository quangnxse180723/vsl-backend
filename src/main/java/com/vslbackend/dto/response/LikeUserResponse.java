package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Mot nguoi da thich bai blog (dung cho admin xem chi tiet). */
@Data
@Builder
public class LikeUserResponse {
    private Long userId;
    private String userName;
    private String userAvatar;
    private LocalDateTime createdAt;
}
