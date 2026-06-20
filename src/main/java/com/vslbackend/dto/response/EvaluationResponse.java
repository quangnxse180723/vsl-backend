package com.vslbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Ket qua danh gia thuc hanh ky hieu ngon ngu.
 *
 * <pre>
 * {
 *   "status":      "CORRECT",
 *   "message":     "Chinh xac! Ban thuc hien ky hieu rat tot.",
 *   "confidence":  87.43,
 *   "predictedId": 42,
 *   "rank":        1
 * }
 * </pre>
 *
 * status:
 *   CORRECT       - rank 1 va confidence > 50%
 *   ALMOST_CORRECT - rank 2-5
 *   INCORRECT     - rank > 5
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EvaluationResponse {

    private final String status;

    private final String message;

    /** Xac suat mo hinh du doan dung tu vung can thuc hanh (0.0 - 100.0). */
    private final double confidence;

    /** Class ID (0-999) duoc du doan cao nhat boi mo hinh. */
    private final int predictedId;

    /** Thu hang cua expectedId trong danh sach xac suat (1 = cao nhat). */
    private final int rank;
}
