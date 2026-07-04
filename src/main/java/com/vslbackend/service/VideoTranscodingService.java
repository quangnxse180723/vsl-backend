package com.vslbackend.service;

import com.vslbackend.exception.AppException;
import com.vslbackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

/**
 * Chuan hoa video tai len ve H.264 (avc1) + AAC trong container MP4 "faststart".
 *
 * Ly do can thiet: video nguon truoc khi upload co the duoc encode bang codec
 * ma trinh duyet KHONG ho tro giai ma qua the <video> HTML5 (vd MPEG-4 Part 2 /
 * "mp4v" tu cac tool cu) - phat hien thuc te tren du lieu da upload cho thay
 * phan lon video roi vao truong hop nay va chi dung mai/khong phan hoi tren UI.
 * H.264 + AAC la to hop duoc TAT CA trinh duyet hien dai ho tro native.
 */
@Slf4j
@Service
public class VideoTranscodingService {

    private static final double DEFAULT_FRAME_RATE = 25.0;
    private static final int VIDEO_BITRATE = 2_000_000;
    private static final int AUDIO_BITRATE = 128_000;

    /**
     * Doc video tu inputFile, decode toan bo frame va re-encode sang H.264/AAC.
     * Tra ve file MP4 tam moi (goi phai xoa sau khi dung xong).
     */
    public File transcodeToH264(File inputFile) {
        File outputFile;
        try {
            outputFile = File.createTempFile("vsl_transcoded_", ".mp4");
        } catch (IOException e) {
            throw new AppException(ErrorCode.MINIO_UPLOAD_ERROR, "Khong the tao file tam: " + e.getMessage());
        }

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile)) {
            try {
                grabber.start();
            } catch (Exception e) {
                throw new AppException(ErrorCode.VIDEO_CORRUPT,
                        "FFmpeg khong the doc video nguon: " + e.getMessage());
            }

            boolean hasAudio = grabber.getAudioChannels() > 0;
            double frameRate = grabber.getFrameRate() > 0 ? grabber.getFrameRate() : DEFAULT_FRAME_RATE;

            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                    outputFile, grabber.getImageWidth(), grabber.getImageHeight(), hasAudio ? grabber.getAudioChannels() : 0)) {

                recorder.setFormat("mp4");
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
                recorder.setFrameRate(frameRate);
                recorder.setVideoBitrate(VIDEO_BITRATE);
                // moov atom o dau file -> trinh duyet phat progressive ngay, khong can tai het file.
                recorder.setOption("movflags", "faststart");

                if (hasAudio) {
                    recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
                    recorder.setAudioBitrate(AUDIO_BITRATE);
                    recorder.setSampleRate(grabber.getSampleRate());
                }

                try {
                    recorder.start();
                    Frame frame;
                    while ((frame = grabber.grab()) != null) {
                        recorder.record(frame);
                    }
                    recorder.stop();
                } catch (Exception e) {
                    throw new AppException(ErrorCode.VIDEO_CORRUPT,
                            "Loi khi transcode video: " + e.getMessage());
                }
            }

            grabber.stop();
        } catch (AppException e) {
            outputFile.delete();
            throw e;
        } catch (Exception e) {
            outputFile.delete();
            throw new AppException(ErrorCode.VIDEO_CORRUPT, "Loi khi transcode video: " + e.getMessage());
        }

        log.info("Transcoded video: {} ({} bytes) -> {} ({} bytes)",
                inputFile.getName(), inputFile.length(), outputFile.getName(), outputFile.length());
        return outputFile;
    }
}
