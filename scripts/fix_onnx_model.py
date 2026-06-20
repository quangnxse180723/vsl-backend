"""
Sua loi "Loop subgraph input has unknown shape" cua MViTv2 ONNX model.

Nguyen nhan: MViTv2 dung pooling-attention voi Loop node co shape dong;
ONNX Runtime khong resolve duoc shape ben trong Loop subgraph luc chay.

Cach sua: chay symbolic shape inference de bake shape cu the vao toan graph
(input da co dinh [1,3,16,224,224]) -> Loop subgraph co shape ro rang.

Output: models/mvitv2_small_fixed.onnx
"""
import sys
import numpy as np
import onnx
import onnxruntime as ort
from onnxruntime.tools.symbolic_shape_infer import SymbolicShapeInference

SRC = "models/mvitv2_small.onnx"
DST = "models/mvitv2_small_fixed.onnx"
INPUT_NAME = "video_frames"
SHAPE = (1, 3, 16, 224, 224)


def try_infer(path, label):
    """Chay thu inference, in ket qua hoac loi."""
    print(f"\n=== Test inference: {label} ({path}) ===")
    try:
        so = ort.SessionOptions()
        sess = ort.InferenceSession(path, so, providers=["CPUExecutionProvider"])
        x = np.random.rand(*SHAPE).astype(np.float32)
        out = sess.run(None, {INPUT_NAME: x})
        print(f"  OK -> output shape = {out[0].shape}")
        return True
    except Exception as e:
        print(f"  FAIL -> {str(e)[:200]}")
        return False


def main():
    print("Loading model:", SRC)
    model = onnx.load(SRC)

    # 1. Xac nhan model goc that su loi
    base_ok = try_infer(SRC, "ORIGINAL")

    # 2. Symbolic shape inference (auto_merge de xu ly cac dim ky hieu)
    print("\nRunning symbolic shape inference...")
    try:
        inferred = SymbolicShapeInference.infer_shapes(
            model, auto_merge=True, guess_output_rank=True, verbose=0
        )
        onnx.save(inferred, DST)
        print("Saved:", DST)
    except Exception as e:
        print("Symbolic shape inference failed:", str(e)[:200])
        # Fallback: onnx shape inference thuong
        print("Fallback: onnx.shape_inference...")
        inferred = onnx.shape_inference.infer_shapes(model, strict_mode=False)
        onnx.save(inferred, DST)
        print("Saved (fallback):", DST)

    # 3. Test model da fix
    fixed_ok = try_infer(DST, "FIXED")

    print("\n================ SUMMARY ================")
    print(f"  original works: {base_ok}")
    print(f"  fixed works:    {fixed_ok}")
    sys.exit(0 if fixed_ok else 1)


if __name__ == "__main__":
    main()
