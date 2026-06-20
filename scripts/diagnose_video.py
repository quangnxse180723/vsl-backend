"""
Ground-truth diagnostic: chay 1 video BAT KY qua pipeline ONNX tat dinh (Python).
Dung de phan dinh: loi o MODEL/DATA hay o pipeline JAVA?

Cach dung:
    python scripts/diagnose_video.py "<duong_dan_video>" <expected_id>

Vi du (video center can kiem chung):
    python scripts/diagnose_video.py "C:/.../..._center_device02_..._82.mp4" 82

Pipeline o day = decode TUAN TU toan bo frame (read_all) + uniform sample 16,
giong het debug_temporal.py mode 'uniform_all' + scaling 'norm01'.
Day la "ban goc dung" ma Java PHAI tai lap.
"""
import sys
import numpy as np
import cv2
import onnxruntime as ort

MODEL = "models/mvitv2_small.onnx"
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


def crop(fr):
    h, w = fr.shape[:2]
    sc = S / min(h, w)
    nh, nw = int(round(h * sc)), int(round(w * sc))
    r = cv2.resize(fr, (nw, nh))
    y0, x0 = (nh - S) // 2, (nw - S) // 2
    return r[y0:y0 + S, x0:x0 + S]


def build_uniform(frames):
    n = len(frames)
    idx = [int(i * n / T) for i in range(T)]          # uniform_all (giong Java i*total/T)
    sel = [frames[i] for i in idx]
    arr = np.zeros((3, T, S, S), dtype=np.float32)
    for f, fr in enumerate(sel):
        img = cv2.cvtColor(crop(fr), cv2.COLOR_BGR2RGB).astype(np.float32)
        x = (img / 255.0 - MEAN) / STD                 # norm01 (Kinetics)
        arr[:, f, :, :] = np.transpose(x, (2, 0, 1))   # HWC -> CHW
    return arr[np.newaxis, ...], idx


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(1)
    video, expected = sys.argv[1], int(sys.argv[2])

    sess = ort.InferenceSession(MODEL, providers=["CPUExecutionProvider"])
    iname = sess.get_inputs()[0].name

    frames = read_all(video)
    print(f"Video : {video}")
    print(f"Frames decoded (tat dinh): {len(frames)}")
    if not frames:
        print("!! Khong decode duoc frame nao."); sys.exit(2)

    x, idx = build_uniform(frames)
    print(f"Sampled frame indices: {idx}")

    logits = sess.run(None, {iname: x})[0][0]
    e = np.exp(logits - logits.max())
    p = e / e.sum()
    top5 = np.argsort(p)[::-1][:5]

    print("\n===== KET QUA (recipe chuan: uniform_all + Kinetics norm) =====")
    print(f"  p[expected={expected}] = {p[expected]:.4f}")
    print(f"  argmax               = {int(top5[0])}  (p={p[top5[0]]:.4f})")
    print(f"  top-5                = " + " ".join(f"{int(i)}:{p[i]:.3f}" for i in top5))
    rank = int((p > p[expected]).sum()) + 1
    print(f"  rank cua expected    = {rank}")

    print("\n===== CHAN DOAN =====")
    if int(top5[0]) == expected or rank <= 5:
        print("  -> Python ONNX DOAN DUNG/GAN DUNG. Model+preprocessing OK.")
        print("     => Loi nam o PIPELINE JAVA (gan nhu chac chan: frame extraction")
        print("        setVideoFrameNumber khong tat dinh). Sua Java -> decode tuan tu.")
    else:
        print("  -> Python ONNX cung DOAN SAI tren video nay.")
        print("     => KHONG phai loi Java. Kiem: (a) viewpoint/chat luong video,")
        print("        (b) lech index nhan (CSV 0- vs 1-indexed), (c) model tu lam.")


if __name__ == "__main__":
    main()
