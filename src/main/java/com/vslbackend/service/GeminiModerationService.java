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

    /** Mot cap {tu vung, danh muc} - dung lam input list & output match cho tinh nang tim tu dong nghia. */
    public record VocabEntry(String word, String categoryName) {}
    public record SynonymMatch(String word, String categoryName) {}

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

    private static final String SYNONYM_SYSTEM_PROMPT = """
            Ban la tro ly cho mot he thong tu vung ngon ngu ky hieu (VSL) tieng Viet.
            Nguoi dung muon de xuat mot TU VUNG MOI. Ban duoc cho: tu ung vien va DANH SACH tu vung da co (kem danh muc).
            Nhiem vu: tim trong danh sach da co nhung tu CO CUNG NGHIA hoac LA TU DONG NGHIA gan nghia voi tu ung vien.
            Quy tac:
            - Chi chon tu thuc su dong nghia / cung y nghia (vd: "so hai" ~ "lo so"), KHONG chon vi trung mot phan chu cai.
            - KHONG lap lai chinh tu ung vien (neu trung chinh xac thi bo qua).
            - Neu khong co tu nao dong nghia, tra ve mang rong.
            - Chi tra ve JSON dung dinh dang: {"synonyms":[{"word":"<tu da co>","categoryName":"<danh muc>"}]}.
            """;

    /**
     * Tim cac tu da co trong he thong co nghia GIONG / dong nghia voi tu ung vien (dung AI).
     * NEM AppException(INTERNAL_ERROR) neu goi Gemini that bai (vd het quota 429) - de tang goi
     * co the phan biet "khong co tu dong nghia" voi "goi AI that bai" va bao cho nguoi dung.
     * Loi phan tich JSON (hiem) thi coi nhu khong co ket qua (tra list rong).
     */
    public List<SynonymMatch> findSynonyms(String candidateWord, List<VocabEntry> existing) {
        if (candidateWord == null || candidateWord.isBlank() || existing == null || existing.isEmpty()) {
            return List.of();
        }
        StringBuilder list = new StringBuilder();
        for (VocabEntry e : existing) {
            list.append("- ").append(e.word()).append(" | ").append(e.categoryName()).append("\n");
        }
        String prompt = SYNONYM_SYSTEM_PROMPT
                + "\n\nTU UNG VIEN: " + candidateWord
                + "\n\nDANH SACH TU VUNG DA CO (tu | danh muc):\n" + list;

        String body = callGemini(prompt, ErrorCode.INTERNAL_ERROR);  // nem neu HTTP != 200 / mang loi
        return parseSynonyms(body);
    }

    private List<SynonymMatch> parseSynonyms(String body) {
        try {
            String text = extractGeminiText(body);
            if (text.isBlank()) return List.of();

            JsonNode result = objectMapper.readTree(stripCodeFence(text));
            JsonNode syns = result.path("synonyms");
            if (!syns.isArray()) return List.of();

            List<SynonymMatch> matches = new java.util.ArrayList<>();
            for (JsonNode s : syns) {
                String word = s.path("word").asText("");
                String cat = s.path("categoryName").asText("");
                if (!word.isBlank()) matches.add(new SynonymMatch(word, cat));
            }
            return matches;
        } catch (Exception e) {
            log.warn("Failed to parse Gemini synonym response: {}", truncate(body, 500));
            return List.of();
        }
    }

    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public ModerationResult moderate(String title, String content) {
        if (!isEnabled()) {
            log.warn("Gemini moderation is disabled or missing API key - allowing blog without moderation.");
            return new ModerationResult(true, "");
        }
        String userText = SYSTEM_PROMPT
                + "\n\nTIEU DE: " + (title == null ? "" : title)
                + "\nNOI DUNG: " + (content == null ? "" : content);

        // Kiem duyet blog FAIL-CLOSED: loi API -> nem MODERATION_ERROR (callGemini da nem san).
        String body = callGemini(userText, ErrorCode.MODERATION_ERROR);
        return parseResult(body);
    }

    /**
     * Goi Gemini voi mot prompt text, tra ve raw response body (JSON cua Gemini API).
     * Dung chung cho ca kiem duyet blog lan tim tu dong nghia.
     * Khi loi (build body / mang / HTTP != 200) thi nem AppException voi errorCode truyen vao.
     */
    private String callGemini(String prompt, ErrorCode errorCode) {
        String requestBody;
        try {
            Map<String, Object> payload = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of(
                            "temperature", 0,
                            "responseMimeType", "application/json"
                    )
            );
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to build Gemini request body", e);
            throw new AppException(errorCode);
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
            log.error("Gemini call failed", e);
            throw new AppException(errorCode);
        }

        if (response.statusCode() != 200) {
            log.error("Gemini returned HTTP {}: {}", response.statusCode(), truncate(response.body(), 500));
            throw new AppException(errorCode);
        }
        return response.body();
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

    /** Lay noi dung text tu response Gemini: candidates[0].content.parts[0].text. */
    private String extractGeminiText(String body) {
        try {
            JsonNode candidates = objectMapper.readTree(body).path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) return "";
            return candidates.get(0).path("content").path("parts").get(0).path("text").asText("");
        } catch (Exception e) {
            return "";
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
