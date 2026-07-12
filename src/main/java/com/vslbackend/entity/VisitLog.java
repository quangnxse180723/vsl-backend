package com.vslbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Mot luot truy cap he thong. User null = khach (guest) chua dang nhap.
 * Ghi 1 lan moi phien trinh duyet (frontend gui beacon khi mo app).
 */
@Entity
@Table(name = "visit_logs", indexes = {
        @Index(name = "idx_visit_visited_at", columnList = "visited_at"),
        @Index(name = "idx_visit_session_id", columnList = "session_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null = khach vang lai (guest). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Dinh danh 1 phien tab trinh duyet (FE sinh, luu sessionStorage). Dung de
     * "nang cap" dung luot cua phien nay tu GUEST -> USER khi khach dang nhap,
     * thay vi ghi them dong moi (tranh dem trung 1 nguoi). Null = log cu/khong co.
     */
    @Column(name = "session_id", length = 64)
    private String sessionId;

    /** Chuoi mo ta thiet bi da phan tich tu User-Agent, vd "Windows · Chrome". */
    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /** Vi tri suy ra tu IP (best-effort), vd "Ho Chi Minh, Vietnam". */
    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "visited_at", nullable = false)
    private LocalDateTime visitedAt;
}
