package com.vslbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Kiem duyet noi dung bai blog bang Gemini truoc khi cho dang.
 * Tra ve {@link ModerationResult}: allowed = co cho dang khong, reason = ly do neu bi chan.
 * <p>
 * Khi chua cau hinh API key (gemini.api.key rong) hoac gemini.enabled=false thi bo qua
 * kiem duyet (cho dang) de moi truong dev khong co key van chay duoc.
 * Khi goi API loi (mang / quota) thi FAIL-CLOSED: nem MODERATION_ERROR de nguoi dung thu lai,
 * tranh de lot noi dung chua kiem duyet.
 */
@Slf4j
@Service
public class GeminiModerationService {

    // ObjectMapper rieng cho service nay - chi dung de tao/doc JSON don gian gui Gemini,
    // khong can cau hinh dac biet cua Spring nen khoi phai phu thuoc bean ObjectMapper
    // (tranh loi UnsatisfiedDependencyException neu Jackson auto-config khong expose bean).
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.enabled:true}")
    private boolean enabled;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public record ModerationResult(boolean allowed, String reason) {}

    private static final String SYSTEM_PROMPT = """
            Ban la bo loc kiem duyet noi dung cho mot cong dong hoc ngon ngu ky hieu (VSL) danh cho nguoi Viet.
            Hay danh gia bai viet blog duoi day (tieu de + noi dung).
            CHAN bai viet neu chua bat ky noi dung nao thuoc cac loai:
            - Quang cao / spam / ban hang tra hinh
            - Mai dam / khieu dam / noi dung tinh duc
            - Lua dao / gian lan / da cap / co bac
            - Ngon tu tho tuc / xuc pham / thu ghet / phan biet doi xu
            - Bao luc / khung bo
            - Chat cam / vu khi / hoat dong phi phap
            Neu bai viet lanh manh, phu hop cong dong hoc tap thi CHO PHEP dang.
            Chi tra ve JSON dung dinh dang: {"allowed": <true|false>, "reason": "<ly do>"}.
            - Neu allowed = false: reason la mot cau ngan gon bang TIENG VIET giai thich bai vi pham dieu gi.
            - Neu allowed = true: reason la chuoi rong "".
            """;

    public ModerationResult moderate(String title, String content) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            log.warn("Gemini moderation is disabled or missing API key - allowing blog without moderation.");
            return new ModerationResult(true, "");
        }

        String userText = SYSTEM_PROMPT
                + "\n\nTIEU DE: " + (title == null ? "" : title)
                + "\nNOI DUNG: " + (content == null ? "" : content);

        String requestBody;
        try {
            Map<String, Object> payload = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", userText)))),
                    "generationConfig", Map.of(
                            "temperature", 0,
                            "responseMimeType", "application/json"
                    )
            );
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to build Gemini request body", e);
            throw new AppException(ErrorCode.MODERATION_ERROR);
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent";

        HttpResponse<String> response;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.error("Gemini moderation call failed", e);
            throw new AppException(ErrorCode.MODERATION_ERROR);
        }

        if (response.statusCode() != 200) {
            log.error("Gemini moderation returned HTTP {}: {}", response.statusCode(),
                    truncate(response.body(), 500));
            throw new AppException(ErrorCode.MODERATION_ERROR);
        }

        return parseResult(response.body());
    }

    private ModerationResult parseResult(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode candidates = root.path("candidates");

            // Gemini tu chan prompt (noi dung qua nang) -> khong co candidate -> coi nhu vi pham
            if (!candidates.isArray() || candidates.isEmpty()) {
                String blockReason = root.path("promptFeedback").path("blockReason").asText("");
                log.warn("Gemini returned no candidates (blockReason={})", blockReason);
                return new ModerationResult(false,
                        "Noi dung vi pham chinh sach an toan va khong the dang.");
            }

            String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText("");
            if (text.isBlank()) {
                throw new AppException(ErrorCode.MODERATION_ERROR);
            }

            JsonNode result = objectMapper.readTree(stripCodeFence(text));
            boolean allowed = result.path("allowed").asBoolean(false);
            String reason = result.path("reason").asText("");
            return new ModerationResult(allowed, reason);
        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Failed to parse Gemini moderation response: {}", truncate(body, 500), e);
            throw new AppException(ErrorCode.MODERATION_ERROR);
        }
    }

    /** Loai bo ```json ... ``` neu model lo boc code fence du da yeu cau JSON thuan. */
    private String stripCodeFence(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline >= 0) t = t.substring(firstNewline + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
