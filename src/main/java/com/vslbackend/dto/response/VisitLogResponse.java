package com.vslbackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** Mot dong trong bang log truy cap chi tiet. */
@Data
@Builder
public class VisitLogResponse {
    private Long id;
    private String visitorType;   // "USER" (hoc vien) hoac "GUEST" (khach)
    private Long userId;          // null neu khach
    private String userName;      // null neu khach
    private String userAvatar;    // null neu khach
    private String deviceInfo;
    private String ipAddress;
    private String location;
    private LocalDateTime visitedAt;
}
