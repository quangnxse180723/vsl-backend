"""
Do tim preprocessing dung cho mvitv2_small.onnx bang video da biet nhan (class 82).

Thu nhieu bien the: resize truc tiep vs short-side+center-crop, RGB vs BGR,
co/khong mean-std, va in prob[82] + top-5 cho tung bien the.
Bien the nao lam prob[82] cao nhat -> do la recipe dung.
"""
import sys
import numpy as np
import cv2
import onnxruntime as ort

MODEL = "models/mvitv2_small.onnx"
VIDEO = r"C:/Users/HT/Downloads/01_Co-Hien_1-100_1-2-3_0108___left_device01_signer01_left_ord1_82.mp4"
EXPECTED = 82
T, S = 16, 224
MEAN = np.array([0.45, 0.45, 0.45], dtype=np.float32)
STD = np.array([0.225, 0.225, 0.225], dtype=np.float32)


def sample_frames(path, n=T):
    """Lay n frame cach deu (BGR tu cv2)."""
    cap = cv2.VideoCapture(path)
    total = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    idxs = [int(i * total / n) for i in range(n)]
    frames = []
    for i in range(total):
        ok, fr = cap.read()
        if not ok:
            break
        if i in idxs:
            frames.append(fr)
    cap.release()
    while len(frames) < n:
        frames.append(frames[-1])
    return frames[:n]  # list of BGR HxWx3


def resize_direct(fr):
    return cv2.resize(fr, (S, S))


def resize_crop(fr, short=S):
    h, w = fr.shape[:2]
    scale = short / min(h, w)
    nh, nw = int(round(h * scale)), int(round(w * scale))
    r = cv2.resize(fr, (nw, nh))
    y0 = (nh - S) // 2
    x0 = (nw - S) // 2
    return r[y0:y0 + S, x0:x0 + S]


def build(frames, resize_fn, to_rgb, norm):
    """-> tensor [1,3,T,S,S] float32 (CTHW)."""
    arr = np.zeros((3, T, S, S), dtype=np.float32)
    for f, fr in enumerate(frames):
        img = resize_fn(fr)
        if to_rgb:
            img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        x = img.astype(np.float32) / 255.0  # HWC
        if norm:
            x = (x - MEAN) / STD
        # HWC -> CHW
        arr[:, f, :, :] = np.transpose(x, (2, 0, 1))
    return arr[np.newaxis, ...]


def main():
    sess = ort.InferenceSession(MODEL, providers=["CPUExecutionProvider"])
    iname = sess.get_inputs()[0].name
    frames = sample_frames(VIDEO)
    print(f"Decoded {len(frames)} frames, size={frames[0].shape}")

    variants = [
        ("direct resize, RGB, norm",      resize_direct, True,  True),
        ("direct resize, RGB, /255 only", resize_direct, True,  False),
        ("direct resize, BGR, norm",      resize_direct, False, True),
        ("shortside+crop, RGB, norm",     resize_crop,   True,  True),
        ("shortside+crop, RGB, /255",     resize_crop,   True,  False),
        ("shortside+crop, BGR, norm",     resize_crop,   False, True),
    ]

    print(f"\n{'variant':<32} {'prob[82]':>10}  top-5 (id:prob)")
    print("-" * 90)
    best = (None, -1)
    for label, rfn, rgb, norm in variants:
        x = build(frames, rfn, rgb, norm)
        logits = sess.run(None, {iname: x})[0][0]
        e = np.exp(logits - logits.max())
        p = e / e.sum()
        top5 = np.argsort(p)[::-1][:5]
        top5s = " ".join(f"{i}:{p[i]:.3f}" for i in top5)
        print(f"{label:<32} {p[EXPECTED]:>10.4f}  {top5s}")
        if p[EXPECTED] > best[1]:
            best = (label, p[EXPECTED])

    print("-" * 90)
    print(f"BEST cho class {EXPECTED}: '{best[0]}' (prob={best[1]:.4f})")


if __name__ == "__main__":
    main()
