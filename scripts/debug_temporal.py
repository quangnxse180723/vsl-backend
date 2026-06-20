"""
Do tim cach lay mau frame (temporal sampling) + value scaling dung.
Tat ca dung chung: shortside-resize 224 + center-crop, RGB, Kinetics norm.
Chi thay doi cach chon 16 frame va cach scale gia tri.
"""
import numpy as np
import cv2
import onnxruntime as ort

MODEL = "models/mvitv2_small.onnx"
VIDEO = r"C:/Users/HT/Downloads/01_Co-Hien_1-100_1-2-3_0108___left_device01_signer01_left_ord1_82.mp4"
EXPECTED = 82
T, S = 16, 224
MEAN = np.array([0.45, 0.45, 0.45], dtype=np.float32)
STD = np.array([0.225, 0.225, 0.225], dtype=np.float32)


def read_all(path):
    cap = cv2.VideoCapture(path)
    frames = []
    while True:
        ok, fr = cap.read()
        if not ok:
            break
        frames.append(fr)
    cap.release()
    return frames  # list BGR


def pick(frames, mode):
    n = len(frames)
    if mode == "uniform_all":
        idx = [int(i * n / T) for i in range(T)]
    elif mode == "first16":
        idx = list(range(min(T, n))) + [n - 1] * max(0, T - n)
    elif mode == "center16":
        start = max(0, n // 2 - T // 2)
        idx = [min(start + i, n - 1) for i in range(T)]
    elif mode == "center_stride2":
        start = max(0, n // 2 - T)  # 16 frame, stride 2, quanh giua
        idx = [min(start + i * 2, n - 1) for i in range(T)]
    else:
        raise ValueError(mode)
    return [frames[i] for i in idx]


def crop(fr):
    h, w = fr.shape[:2]
    sc = S / min(h, w)
    nh, nw = int(round(h * sc)), int(round(w * sc))
    r = cv2.resize(fr, (nw, nh))
    y0, x0 = (nh - S) // 2, (nw - S) // 2
    return r[y0:y0 + S, x0:x0 + S]


def build(sel, scaling):
    arr = np.zeros((3, T, S, S), dtype=np.float32)
    for f, fr in enumerate(sel):
        img = cv2.cvtColor(crop(fr), cv2.COLOR_BGR2RGB).astype(np.float32)
        if scaling == "norm01":
            x = (img / 255.0 - MEAN) / STD
        elif scaling == "div255":
            x = img / 255.0
        elif scaling == "raw255":
            x = img
        else:
            raise ValueError(scaling)
        arr[:, f, :, :] = np.transpose(x, (2, 0, 1))
    return arr[np.newaxis, ...]


def main():
    sess = ort.InferenceSession(MODEL, providers=["CPUExecutionProvider"])
    iname = sess.get_inputs()[0].name
    frames = read_all(VIDEO)
    print(f"total frames decoded: {len(frames)}")

    print(f"\n{'temporal':<16}{'scaling':<10}{'p[82]':>9}  argmax (id:prob)")
    print("-" * 70)
    best = (None, -1)
    for tm in ["uniform_all", "first16", "center16", "center_stride2"]:
        sel = pick(frames, tm)
        for sc in ["norm01", "div255", "raw255"]:
            x = build(sel, sc)
            logits = sess.run(None, {iname: x})[0][0]
            e = np.exp(logits - logits.max())
            p = e / e.sum()
            am = int(np.argmax(p))
            print(f"{tm:<16}{sc:<10}{p[EXPECTED]:>9.4f}  {am}:{p[am]:.3f}")
            if p[EXPECTED] > best[1]:
                best = (f"{tm}+{sc}", p[EXPECTED])
    print("-" * 70)
    print(f"BEST cho class {EXPECTED}: {best[0]} (p={best[1]:.4f})")


if __name__ == "__main__":
    main()
