package com.vslbackend.service.impl;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.vslbackend.dto.response.EvaluationResponse;
import com.vslbackend.entity.AttemptHistory;
import com.vslbackend.entity.LearningStatus;
import com.vslbackend.entity.User;
import com.vslbackend.entity.UserProgress;
import com.vslbackend.entity.Vocabulary;
import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import com.vslbackend.repository.AttemptHistoryRepository;
import com.vslbackend.repository.UserProgressRepository;
import com.vslbackend.repository.UserRepository;
import com.vslbackend.repository.VocabularyRepository;
import com.vslbackend.service.inter.SignLanguageEvaluationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.scheduling.annotation.Async;

import java.io.File;
import java.nio.FloatBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI inference service su dung ONNX Runtime + JavaCV.
 *
 * Model: MViTv2-Small (35M params, 135MB)
 *   Input  "video_frames"  : float32 [1, 3, 16, 224, 224]
 *   Output "action_logits" : float32 [1, 1000]
 *
 * Luong xu ly:
 *   1. Luu video tam len dia
 *   2. Trich 16 frames cach deu (FFmpegFrameGrabber)
 *   3. Resize 224x224, chuyen BGR -> RGB, normalize [0,255]->[0,1]
 *   4. Flatten theo thu tu C->T->H->W thanh float[2408448]
 *   5. Chay ONNX inference (thread-safe, OrtSession dung chung)
 *   6. Softmax + tim rank cua expectedId
 *   7. Xoa file tam (trong finally, khong phu thuoc ket qua)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignLanguageInferenceService implements SignLanguageEvaluationService {

    // -------------------- ONNX model constants --------------------
    private static final String INPUT_NODE   = "video_frames";
    private static final String OUTPUT_NODE  = "action_logits";
    private static final int REQUIRED_FRAMES = 16;
    private static final int INPUT_SIZE      = 224;
    private static final int NUM_CLASSES     = 1000;

    // Chuan hoa Kinetics chuan (MViTv2/PyTorchVideo): val = (pixel/255 - mean) / std
    // Dung chung cho ca 3 kenh RGB.
    private static final float NORM_MEAN = 0.45f;
    private static final float NORM_STD  = 0.225f;

    // Actual node names resolved from model at startup (may differ from constants above)
    private String resolvedInputNode  = INPUT_NODE;
    private String resolvedOutputNode = OUTPUT_NODE;

    // -------------------- Business logic thresholds --------------------
    private static final float CORRECT_CONFIDENCE_THRESHOLD = 0.5f;
    private static final int   ALMOST_CORRECT_MAX_RANK      = 5;

    // -------------------- Dependencies --------------------
    private final VocabularyRepository    vocabularyRepository;
    private final AttemptHistoryRepository attemptHistoryRepository;
    private final UserRepository           userRepository;
    private final UserProgressRepository   userProgressRepository;
    private final com.vslbackend.service.inter.AchievementService achievementService;

    @Value("${ai.model.path:./models/mvitv2_small.onnx}")
    private String modelPath;

    // -------------------- ONNX runtime (singleton, thread-safe) --------------------
    private OrtEnvironment ortEnvironment;
    private OrtSession     ortSession;

    /**
     * Load OrtEnvironment va OrtSession mot lan duy nhat khi app khoi dong.
     * Neu model chua ton tai, ghi log warning va de ortSession = null.
     * Cac request se nhan loi AI_MODEL_NOT_LOADED thay vi crash app.
     */
    @PostConstruct
    public void initModel() {
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            log.warn("ONNX model not found at '{}'. AI evaluation will return AI_MODEL_NOT_LOADED.", modelPath);
            return;
        }
        try {
            ortEnvironment = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setInterOpNumThreads(1);
            // intra=1 de TAT DINH (bit-exact): >1 thread khien partial-sum cua conv/matmul
            // cong vao theo thu tu khac nhau giua cac lan chay -> confidence rung o bit thap.
            // Single-thread cham hon chut nhung cho cung 1 video luon ra dung 1 ket qua.
            opts.setIntraOpNumThreads(1);
            // ALL_OPT: full graph optimization (nhanh nhat). Model da duoc xu ly bang
            // symbolic shape inference (scripts/fix_onnx_model.py) nen Loop subgraph
            // co shape ro rang -> chay tot voi moi muc optimization.
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            // BAT BUOC tat memory-pattern: model co Loop node (pooling-attention) shape dong.
            // Memory-pattern "hoc" layout bo nho tu lan chay DAU roi tai dung buffer cho cac
            // lan sau -> voi Loop node, ke hoach do KHONG hop le tu run thu 2 -> output rac
            // (trieu chung: lan dau dung 98.7%, cac lan sau deu sai id=785/0.22%).
            opts.setMemoryPatternOptimization(false);
            ortSession = ortEnvironment.createSession(modelPath, opts);

            // Log actual node names — verify they match our INPUT_NODE / OUTPUT_NODE constants
            log.info("ONNX model loaded from '{}':", modelPath);
            ortSession.getInputInfo().forEach((name, info) ->
                    log.info("  Input  node: '{}' shape={}", name, info));
            ortSession.getOutputInfo().forEach((name, info) ->
                    log.info("  Output node: '{}' shape={}", name, info));

            // Resolve actual node names; fall back to first node if constants don't match
            if (!ortSession.getInputNames().contains(INPUT_NODE)) {
                resolvedInputNode = ortSession.getInputNames().iterator().next();
                log.warn("Expected input node '{}' not found — using '{}' from model",
                        INPUT_NODE, resolvedInputNode);
            }
            if (!ortSession.getOutputNames().contains(OUTPUT_NODE)) {
                resolvedOutputNode = ortSession.getOutputNames().iterator().next();
                log.warn("Expected output node '{}' not found — using '{}' from model",
                        OUTPUT_NODE, resolvedOutputNode);
            }
            log.info("Ready: input='{}', output='{}'", resolvedInputNode, resolvedOutputNode);

        } catch (Throwable e) {
            log.error("Failed to load ONNX model [{}]: {} — AI evaluation unavailable.",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /** Dong OrtSession va OrtEnvironment khi app shutdown. */
    @PreDestroy
    public void destroyModel() {
        try {
            if (ortSession != null)     ortSession.close();
            if (ortEnvironment != null) ortEnvironment.close();
            log.info("ONNX resources released.");
        } catch (OrtException e) {
            log.warn("Error while closing ONNX resources: {}", e.getMessage());
        }
    }

    // ========================= PUBLIC API =========================

    /**
     * Chay tren aiTaskExecutor (thread pool rieng biet) de khong chiem Tomcat thread.
     * Spring MVC giu request "mo" cho den khi CompletableFuture hoan thanh.
     * Ket qua tra ve qua CompletableFuture.completedFuture(); exception tu dong
     * duoc boc vao exceptionally-completed Future va duoc GlobalExceptionHandler xu ly.
     */
    @Async("aiTaskExecutor")
    @Override
    public CompletableFuture<EvaluationResponse> evaluate(MultipartFile videoFile, int expectedId, Long userId) {
        validateVideoFile(videoFile);

        if (ortSession == null) {
            throw new AppException(ErrorCode.AI_MODEL_NOT_LOADED);
        }

        File tempFile = null;
        try {
            // 1. Luu video xuong file tam (MultipartFile con hop le: Spring giu request song)
            tempFile = File.createTempFile("vsl_practice_", ".mp4");
            videoFile.transferTo(tempFile);
            log.debug("Saved practice video to temp: {} ({} bytes)", tempFile.getName(), tempFile.length());

            // 2. Trich 16 frames cach deu nhau
            Mat[] frames = extractSampledFrames(tempFile);

            // 3. Tao tensor float[1,3,16,224,224] va flatten
            float[] tensorData = buildTensorData(frames); // frames[i].release() duoc goi ben trong

            // 4. ONNX inference voi try-with-resources (C++ tu don rac)
            float[] logits = runInference(tensorData);

            // 5. Softmax + danh gia ket qua
            float[] probabilities = softmax(logits);
            EvaluationResponse response = buildResponse(probabilities, expectedId);

            // 6. Ghi lich su (non-critical - loi khong anh huong response)
            persistAttempt(userId, expectedId, response.getPredictedId(), response.getStatus(), response.getConfidence());

            log.info("Evaluation done: expectedId={}, status={}, confidence={}%, rank={}",
                    expectedId, response.getStatus(), response.getConfidence(), response.getRank());
            return CompletableFuture.completedFuture(response);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Evaluation failed for expectedId={}: {}", expectedId, e.getMessage(), e);
            throw new AppException(ErrorCode.AI_INFERENCE_ERROR, "Khong the xu ly video: " + e.getMessage());
        } finally {
            // BAT BUOC: xoa file tam de bao ve quyen rieng tu
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("Could not delete temp file: {}. Schedule for JVM exit.", tempFile.getAbsolutePath());
                    tempFile.deleteOnExit();
                }
            }
        }
    }

    // ========================= FRAME EXTRACTION =========================

    /**
     * Trich 16 frames cach deu tu video bang decode TUAN TU (deterministic).
     *
     * KHONG dung setVideoFrameNumber (seek theo keyframe): no khong tat dinh
     * (cung 1 video, 2 lan goi ra 2 bo frame khac nhau -> argmax 453 vs 463) va
     * lech voi pipeline train/Python. Decode tuan tu toan bo roi lay mau deu cho
     * ket qua khop chinh xac Python read_all() + uniform sample (vd 72 frame ->
     * index [0,4,9,...,67]). Voi video luyen tap ngan, chi phi decode khong dang ke.
     *
     * Neu tong frames < 16: nhan ban frame cuoi den du 16.
     */
    private Mat[] extractSampledFrames(File videoFile) throws Exception {
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

        // try-with-resources: grabber.close() goi ca stop() va release() trong moi truong hop,
        // ke ca khi stop() nem exception — tranh leak con tro C++ cua JNI.
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
            try {
                grabber.start();
                return sequentialGrabAndSample(grabber, converter);
            } catch (AppException e) {
                throw e;
            } catch (Exception e) {
                throw new AppException(ErrorCode.VIDEO_CORRUPT,
                        "FFmpeg khong the mo video: " + e.getMessage());
            }
        }
    }

    /** Grab toan bo roi lay mau deu - tat dinh, khop pipeline Python. */
    private Mat[] sequentialGrabAndSample(FFmpegFrameGrabber grabber,
                                           OpenCVFrameConverter.ToMat converter) throws Exception {
        List<Mat> buffer = new ArrayList<>();
        try {
            Frame f;
            while ((f = grabber.grabImage()) != null) {
                Mat mat = converter.convert(f);
                if (mat != null && !mat.isNull()) {
                    buffer.add(mat.clone());
                }
            }
        } catch (Exception e) {
            // Giai phong buffer da load truoc khi re-throw
            buffer.forEach(Mat::release);
            throw e;
        }

        if (buffer.isEmpty()) {
            throw new AppException(ErrorCode.VIDEO_NO_FRAMES);
        }

        int bufSize = buffer.size();
        Mat[] frames = new Mat[REQUIRED_FRAMES];

        for (int i = 0; i < REQUIRED_FRAMES; i++) {
            int idx = (bufSize < REQUIRED_FRAMES)
                    ? Math.min(i, bufSize - 1)
                    : (int) ((long) i * bufSize / REQUIRED_FRAMES);
            frames[i] = buffer.get(idx);
            buffer.set(idx, null); // danh dau da tieu thu
        }

        // Giai phong cac frame khong duoc chon
        buffer.stream().filter(m -> m != null).forEach(Mat::release);

        fillNullFrames(frames);
        return frames;
    }

    /**
     * Dien null frame bang frame ke can hop le.
     * Forward fill truoc (xu ly null o giua/cuoi), backward fill sau (xu ly null o dau).
     */
    private void fillNullFrames(Mat[] frames) {
        for (int i = 1; i < frames.length; i++) {
            if (frames[i] == null && frames[i - 1] != null) {
                frames[i] = frames[i - 1].clone();
            }
        }
        for (int i = frames.length - 2; i >= 0; i--) {
            if (frames[i] == null && frames[i + 1] != null) {
                frames[i] = frames[i + 1].clone();
            }
        }
    }

    // ========================= TENSOR CONSTRUCTION =========================

    /**
     * Chuyen 16 Mat frames thanh float[2_408_448] theo thu tu CTHW.
     *
     * Shape: [1, C=3, T=16, H=224, W=224]
     * Index : c*(T*H*W) + f*(H*W) + y*W + x
     *
     * Moi frame (BGR tu OpenCV) duoc:
     *   1. Resize canh ngan ve 224 (giu ty le), center-crop 224x224 (chuan MViTv2)
     *   2. Chuyen sang RGB
     *   3. Chuan hoa Kinetics: (pixel/255 - 0.45) / 0.225
     *   4. .release() ngay lap tuc sau khi lay du lieu
     */
    private float[] buildTensorData(Mat[] frames) {
        final int T = REQUIRED_FRAMES, H = INPUT_SIZE, W = INPUT_SIZE, C = 3;
        float[] tensor = new float[C * T * H * W];

        for (int f = 0; f < T; f++) {
            if (frames[f] == null) {
                // Frame null sau fill => ghi 0 (black frame)
                continue;
            }
            Mat resized = new Mat();
            Mat cropped = null;
            Mat rgb     = new Mat();
            try {
                // Resize canh ngan ve INPUT_SIZE, giu nguyen ty le (tranh bop meo video doc/ngang)
                int srcH = frames[f].rows(), srcW = frames[f].cols();
                double scale = (double) INPUT_SIZE / Math.min(srcH, srcW);
                int nh = (int) Math.round(srcH * scale);
                int nw = (int) Math.round(srcW * scale);
                opencv_imgproc.resize(frames[f], resized, new Size(nw, nh));

                // Center-crop 224x224
                int x0 = Math.max(0, (nw - W) / 2);
                int y0 = Math.max(0, (nh - H) / 2);
                cropped = new Mat(resized, new Rect(x0, y0, W, H));

                opencv_imgproc.cvtColor(cropped, rgb, opencv_imgproc.COLOR_BGR2RGB);

                byte[] pixels = new byte[H * W * C]; // HWC layout
                rgb.data().get(pixels);

                // Vong lap: C -> T -> H -> W (CTHW)
                for (int c = 0; c < C; c++) {
                    for (int y = 0; y < H; y++) {
                        for (int x = 0; x < W; x++) {
                            int srcIdx = (y * W + x) * C + c;          // HWC -> lay kenh c
                            int dstIdx = c * (T * H * W)
                                       + f * (H * W)
                                       + y * W
                                       + x;
                            float scaled = (pixels[srcIdx] & 0xFF) / 255.0f;
                            tensor[dstIdx] = (scaled - NORM_MEAN) / NORM_STD;
                        }
                    }
                }
            } finally {
                // BAT BUOC giai phong Mat ngay trong vong lap de chong leak RAM
                resized.release();
                if (cropped != null) cropped.release();
                rgb.release();
                frames[f].release();
            }
        }
        return tensor;
    }

    // ========================= ONNX INFERENCE =========================

    /**
     * Chay inference va tra ve mang logits thu, float[1000].
     * Boc OnnxTensor va OrtSession.Result bang try-with-resources.
     */
    private float[] runInference(float[] tensorData) throws OrtException {
        long[] shape = {1L, 3L, REQUIRED_FRAMES, INPUT_SIZE, INPUT_SIZE};

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(
                ortEnvironment, FloatBuffer.wrap(tensorData), shape);
             OrtSession.Result result = ortSession.run(Map.of(resolvedInputNode, inputTensor))) {

            float[][] output = (float[][]) result.get(resolvedOutputNode)
                    .orElseThrow(() -> new OrtException("Output node '" + resolvedOutputNode + "' not found in model"))
                    .getValue();

            // Clone truoc khi result.close() giai phong bo nho native
            return output[0].clone();
        }
    }

    // ========================= MATH HELPERS =========================

    /**
     * Softmax on-the-fly voi max-subtraction de tranh overflow numerics.
     */
    private float[] softmax(float[] logits) {
        float max = Float.NEGATIVE_INFINITY;
        for (float v : logits) if (v > max) max = v;

        float[] probs = new float[logits.length];
        float sum = 0f;
        for (int i = 0; i < logits.length; i++) {
            probs[i] = (float) Math.exp(logits[i] - max);
            sum += probs[i];
        }
        for (int i = 0; i < probs.length; i++) probs[i] /= sum;
        return probs;
    }

    /**
     * Tim thu hang cua expectedId (1 = xac suat cao nhat).
     * O(n) don gian, khong sort toan bo mang.
     */
    private int findRank(float[] probs, int expectedId) {
        if (expectedId < 0 || expectedId >= probs.length) {
            log.warn("expectedId={} out of range [0,{}). Model has {} classes.",
                    expectedId, probs.length, probs.length);
            return probs.length;
        }
        float target = probs[expectedId];
        int rank = 1;
        for (float p : probs) {
            if (p > target) rank++;
        }
        return rank;
    }

    /** Tim index co xac suat cao nhat. */
    private int findTopPredictedId(float[] probs) {
        int best = 0;
        for (int i = 1; i < probs.length; i++) {
            if (probs[i] > probs[best]) best = i;
        }
        return best;
    }

    // ========================= BUSINESS LOGIC =========================

    private EvaluationResponse buildResponse(float[] probs, int expectedId) {
        int topPredictedId = findTopPredictedId(probs);
        int rank           = findRank(probs, expectedId);
        double confidence  = (expectedId >= 0 && expectedId < probs.length)
                ? Math.round(probs[expectedId] * 10000.0) / 100.0
                : 0.0;

        String status;
        String message;

        if (rank == 1 && probs[expectedId] > CORRECT_CONFIDENCE_THRESHOLD) {
            status  = "CORRECT";
            message = "Chinh xac! Ban thuc hien ky hieu rat tot.";
        } else if (rank <= ALMOST_CORRECT_MAX_RANK) {
            status  = "ALMOST_CORRECT";
            message = "Gan dung! Hay chu y hon den tu the tay va goc do co the.";
        } else {
            status  = "INCORRECT";
            message = "Chua dung. Hay xem lai video mau va thu lai tu dau.";
        }

        return EvaluationResponse.builder()
                .status(status)
                .message(message)
                .confidence(confidence)
                .predictedId(topPredictedId)
                .rank(rank)
                .build();
    }

    // ========================= ATTEMPT LOGGING =========================

    /**
     * Ghi lich su thu thuc hanh vao DB.
     * Loi o day KHONG anh huong den response tra ve cho client.
     */
    private void persistAttempt(Long userId, int expectedId, int predictedId, String status, double confidence) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            Vocabulary vocab = vocabularyRepository.findByExpectedId(expectedId).orElse(null);
            boolean passed = "CORRECT".equals(status);

            AttemptHistory history = AttemptHistory.builder()
                    .user(user)
                    .vocabulary(vocab)
                    .isCorrect(passed)
                    .aiPredictedCode((long) predictedId)
                    .confidence(confidence)
                    .attemptedAt(LocalDateTime.now())
                    .build();

            attemptHistoryRepository.save(history);

            // Unlock achievements sau mỗi lần thực hành
            if (user != null) {
                try { achievementService.checkAndUnlock(userId); }
                catch (Exception e) { log.warn("Achievement check failed for userId={}: {}", userId, e.getMessage()); }
            }

            if (user != null && vocab != null) {
                updateUserProgress(user, vocab, passed);
            }
        } catch (Exception e) {
            log.warn("Failed to persist attempt history for userId={}, expectedId={}: {}",
                    userId, expectedId, e.getMessage());
        }
    }

    /**
     * Cap nhat tien trinh hoc cua user cho 1 tu vung.
     * Trang thai chi co 2 chieu mot huong: LEARNING -> LEARNED.
     * Lan pass DAU TIEN se chuyen thanh LEARNED va KHONG bao gio bi
     * cac lan fail sau do keo nguoc lai LEARNING (tranh loi logic lam
     * tut trang thai mot cach bat hop ly).
     */
    private void updateUserProgress(User user, Vocabulary vocab, boolean passed) {
        UserProgress progress = userProgressRepository
                .findByUser_UserIdAndVocabulary_Id(user.getUserId(), vocab.getId())
                .orElseGet(() -> UserProgress.builder()
                        .user(user)
                        .vocabulary(vocab)
                        .learningStatus(LearningStatus.LEARNING)
                        .build());

        if (passed) {
            progress.setLearningStatus(LearningStatus.LEARNED);
        } else if (progress.getLearningStatus() == null) {
            progress.setLearningStatus(LearningStatus.LEARNING);
        }
        // fail sau khi da LEARNED: giu nguyen LEARNED, khong ha cap.

        progress.setLastAttemptedAt(LocalDateTime.now());
        userProgressRepository.save(progress);
    }

    // ========================= VALIDATION =========================

    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.VIDEO_EMPTY);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new AppException(ErrorCode.VIDEO_INVALID_TYPE);
        }
    }
}
