package com.vslbackend.service.impl;

import com.vslbackend.dto.response.AdminSummaryResponse;
import com.vslbackend.dto.response.VisitLogResponse;
import com.vslbackend.dto.response.VisitTimePoint;
import com.vslbackend.entity.BlogStatus;
import com.vslbackend.entity.Role;
import com.vslbackend.entity.User;
import com.vslbackend.entity.VisitLog;
import com.vslbackend.repository.BlogRepository;
import com.vslbackend.repository.UserRepository;
import com.vslbackend.repository.VisitLogRepository;
import com.vslbackend.repository.VocabularyRepository;
import com.vslbackend.service.inter.VisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitServiceImpl implements VisitService {

    private final VisitLogRepository visitLogRepository;
    private final UserRepository userRepository;
    private final VocabularyRepository vocabularyRepository;
    private final BlogRepository blogRepository;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    // ──────────────────────── GHI LUOT TRUY CAP ────────────────────────

    @Async
    @Override
    @Transactional
    public void recordVisit(Long userId, String sessionId, String ipAddress, String userAgent) {
        try {
            User user = null;
            if (userId != null) {
                user = userRepository.findById(userId).orElse(null);
            }

            // Upsert theo phien: neu phien nay da co 1 luot (vd luc mo app la GUEST)
            // va bay gio khach vua dang nhap -> chi GAN user vao dong cu, giu nguyen
            // thoi diem truy cap dau. Tranh tao them dong moi cho cung 1 nguoi.
            if (sessionId != null && !sessionId.isBlank()) {
                VisitLog existing = visitLogRepository.findBySessionId(sessionId).orElse(null);
                if (existing != null) {
                    if (user != null && existing.getUser() == null) {
                        existing.setUser(user);
                        visitLogRepository.save(existing);
                    }
                    return; // Da co luot cho phien nay -> khong ghi trung
                }
            }

            VisitLog visit = VisitLog.builder()
                    .user(user)
                    .sessionId(sessionId)
                    .deviceInfo(parseDevice(userAgent))
                    .ipAddress(ipAddress)
                    .location(resolveLocation(ipAddress))
                    .visitedAt(LocalDateTime.now())
                    .build();
            visitLogRepository.save(visit);
        } catch (Exception ex) {
            // Tracking khong duoc lam vo trai nghiem nguoi dung -> chi log
            log.warn("Failed to record visit: {}", ex.getMessage());
        }
    }

    /** Phan tich User-Agent thanh chuoi "OS · Trinh duyet" don gian. */
    private String parseDevice(String ua) {
        if (ua == null || ua.isBlank()) return "Không xác định";
        String s = ua.toLowerCase();

        String os;
        if (s.contains("android")) os = "Android";
        else if (s.contains("iphone") || s.contains("ipad") || s.contains("ios")) os = "iOS";
        else if (s.contains("windows")) os = "Windows";
        else if (s.contains("mac os") || s.contains("macintosh")) os = "macOS";
        else if (s.contains("linux")) os = "Linux";
        else os = "Khác";

        String browser;
        if (s.contains("edg")) browser = "Edge";
        else if (s.contains("opr") || s.contains("opera")) browser = "Opera";
        else if (s.contains("chrome")) browser = "Chrome";
        else if (s.contains("firefox")) browser = "Firefox";
        else if (s.contains("safari")) browser = "Safari";
        else browser = "Trình duyệt khác";

        boolean mobile = s.contains("mobile") || s.contains("android") || s.contains("iphone");
        return os + " · " + browser + (mobile ? " · Di động" : "");
    }

    /** Suy ra vi tri tu IP (best-effort). IP noi bo (local/dev) -> "Cục bộ (Local)". Loi -> null. */
    private String resolveLocation(String ip) {
        if (ip == null || ip.isBlank() || isPrivateIp(ip)) {
            // IP private (localhost / mang LAN / gateway Docker) khong the geo-lookup.
            // Chi xay ra o moi truong dev; khi deploy that, khach co IP public se ra vi tri thuc.
            return "Cục bộ (Local)";
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(1500))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/" + ip + "?fields=status,country,regionName,city"))
                    .timeout(Duration.ofMillis(1500))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = resp.body();
            if (body == null || !body.contains("\"status\":\"success\"")) return null;
            String city = extractJson(body, "city");
            String region = extractJson(body, "regionName");
            String country = extractJson(body, "country");
            List<String> parts = new ArrayList<>();
            if (city != null && !city.isBlank()) parts.add(city);
            else if (region != null && !region.isBlank()) parts.add(region);
            if (country != null && !country.isBlank()) parts.add(country);
            return parts.isEmpty() ? null : String.join(", ", parts);
        } catch (Exception ex) {
            log.debug("Geo lookup failed for {}: {}", ip, ex.getMessage());
            return null;
        }
    }

    private boolean isPrivateIp(String ip) {
        return ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")
                || ip.startsWith("10.") || ip.startsWith("192.168.")
                || ip.startsWith("172.16.") || ip.startsWith("172.17.")
                || ip.startsWith("172.18.") || ip.startsWith("172.19.")
                || ip.startsWith("172.2") || ip.startsWith("172.30.") || ip.startsWith("172.31.")
                || ip.equalsIgnoreCase("localhost");
    }

    /** Rut gia tri chuoi tho tu JSON phang (khong dung thu vien de tranh phu thuoc). */
    private String extractJson(String json, String key) {
        String needle = "\"" + key + "\":\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int start = i + needle.length();
        int end = json.indexOf('"', start);
        return end < 0 ? null : json.substring(start, end);
    }

    // ──────────────────────── TONG QUAN ────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AdminSummaryResponse getSummary() {
        return AdminSummaryResponse.builder()
                .totalStudents(userRepository.countByRole(Role.USER))
                .totalVisits(visitLogRepository.count())
                .totalVocabularies(vocabularyRepository.count())
                // Chi dem bai da xuat ban (PUBLISHED), khong tinh nhap/da go
                .totalBlogs(blogRepository.countByStatus(BlogStatus.PUBLISHED))
                .build();
    }

    // ──────────────────────── BIEU DO THEO THOI GIAN ────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<VisitTimePoint> getVisitTimeSeries(LocalDate from, LocalDate to, String granularity) {
        String gran = granularity == null ? "DAY" : granularity.toUpperCase();
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay(); // bao gom het ngay 'to'

        List<LocalDateTime> timestamps = visitLogRepository.findVisitedAtBetween(start, end);

        // Gom nhom theo nhan tuong ung do chi tiet
        Map<String, Long> counts = new LinkedHashMap<>();
        for (LocalDateTime ts : timestamps) {
            String key = bucketLabel(ts.toLocalDate(), gran);
            counts.merge(key, 1L, Long::sum);
        }

        // Lap day khoang trong de bieu do lien tuc
        List<VisitTimePoint> points = new ArrayList<>();
        for (LocalDate cursor = truncate(from, gran); !cursor.isAfter(to); cursor = increment(cursor, gran)) {
            String key = bucketLabel(cursor, gran);
            points.add(VisitTimePoint.builder()
                    .label(key)
                    .count(counts.getOrDefault(key, 0L))
                    .build());
        }
        return points;
    }

    private String bucketLabel(LocalDate date, String gran) {
        return switch (gran) {
            case "YEAR" -> String.valueOf(date.getYear());
            case "MONTH" -> date.format(MONTH_FMT);
            default -> date.format(DAY_FMT);
        };
    }

    private LocalDate truncate(LocalDate date, String gran) {
        return switch (gran) {
            case "YEAR" -> date.withDayOfYear(1);
            case "MONTH" -> date.withDayOfMonth(1);
            default -> date;
        };
    }

    private LocalDate increment(LocalDate date, String gran) {
        return switch (gran) {
            case "YEAR" -> date.plusYears(1);
            case "MONTH" -> date.plusMonths(1);
            default -> date.plusDays(1);
        };
    }

    // ──────────────────────── BANG LOG CHI TIET ────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<VisitLogResponse> getVisitLogs(Pageable pageable) {
        return visitLogRepository.findAllWithUser(pageable).map(this::toResponse);
    }

    private VisitLogResponse toResponse(VisitLog v) {
        User u = v.getUser();
        return VisitLogResponse.builder()
                .id(v.getId())
                .visitorType(u != null ? "USER" : "GUEST")
                .userId(u != null ? u.getUserId() : null)
                .userName(u != null ? u.getFullName() : null)
                .userAvatar(u != null ? u.getAvatarUrl() : null)
                .deviceInfo(v.getDeviceInfo())
                .ipAddress(v.getIpAddress())
                .location(v.getLocation())
                .visitedAt(v.getVisitedAt())
                .build();
    }
}
