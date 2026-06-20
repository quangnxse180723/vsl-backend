package com.vslbackend.service.inter;

import com.vslbackend.dto.response.EvaluationResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;

public interface SignLanguageEvaluationService {

    /**
     * Danh gia video thuc hanh ky hieu ngon ngu (chay bat dong bo tren aiTaskExecutor).
     *
     * @param videoFile  video thuc hanh tu nguoi dung (mp4/webm/mov)
     * @param expectedId chi so class trong ONNX model tuong ung voi tu vung can luyen tap
     * @param userId     ID nguoi dung (dung de ghi lich su)
     * @return CompletableFuture bao ket qua danh gia; exception duoc boc vao exceptionally-future
     */
    CompletableFuture<EvaluationResponse> evaluate(MultipartFile videoFile, int expectedId, Long userId);
}
