#!/usr/bin/env python3
"""
把 IDEA-CCNL/Erlangshen-Roberta-110M-Sentiment 导出为 ONNX 并 int8 量化。

一次性操作，导出后推理期不需要 Python。

输出：
- ml/models/sentiment.onnx（int8 量化，约 100 MB）
- ml/models/tokenizer/（tokenizer 文件）

已知风险：transformers 5.x 可能破坏 optimum 导出。
若报错，固定 transformers 到 4.x：pip install "transformers<5"
"""

import sys
from pathlib import Path

MODEL_ID = "IDEA-CCNL/Erlangshen-Roberta-110M-Sentiment"
# 用 hf-mirror.com 加速（国内）
HF_ENDPOINT = "https://hf-mirror.com"

OUTPUT_DIR = Path(__file__).parent.parent / "models"


def main():
    import os
    os.environ.setdefault("HF_ENDPOINT", HF_ENDPOINT)

    print(f"[1/4] 加载模型 {MODEL_ID} ...")
    try:
        from transformers import AutoTokenizer, AutoModelForSequenceClassification
    except ImportError:
        print("请先安装依赖：pip install -r requirements.txt", file=sys.stderr)
        sys.exit(1)

    tokenizer = AutoTokenizer.from_pretrained(MODEL_ID)
    model = AutoModelForSequenceClassification.from_pretrained(MODEL_ID)

    print(f"[2/4] 导出 ONNX ...")
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    onnx_path = OUTPUT_DIR / "sentiment_fp32.onnx"

    try:
        from optimum.onnxruntime import ORTModelForSequenceClassification
        ort_model = ORTModelForSequenceClassification.from_pretrained(MODEL_ID, export=True)
        ort_model.save_pretrained(OUTPUT_DIR / "onnx_fp32")
        print(f"  FP32 ONNX 已保存到 {OUTPUT_DIR / 'onnx_fp32'}")
    except Exception as e:
        print(f"optimum 导出失败：{e}", file=sys.stderr)
        print("尝试固定 transformers 版本：pip install 'transformers<5'", file=sys.stderr)
        sys.exit(1)

    print(f"[3/4] int8 动态量化 ...")
    try:
        from onnxruntime.quantization import quantize_dynamic, QuantType
        quantize_dynamic(
            str(OUTPUT_DIR / "onnx_fp32" / "model.onnx"),
            str(OUTPUT_DIR / "sentiment.onnx"),
            weight_type=QuantType.QInt8,
        )
        size_mb = (OUTPUT_DIR / "sentiment.onnx").stat().st_size / 1024 / 1024
        print(f"  int8 量化完成：{size_mb:.1f} MB")
    except Exception as e:
        print(f"量化失败：{e}", file=sys.stderr)
        sys.exit(1)

    print(f"[4/4] 保存 tokenizer ...")
    tokenizer.save_pretrained(OUTPUT_DIR / "tokenizer")

    print("\n✅ 导出完成。请将以下文件复制到 Java 侧：")
    print(f"  - {OUTPUT_DIR / 'sentiment.onnx'}")
    print(f"  - {OUTPUT_DIR / 'tokenizer'}")
    print("\n下一步：在 Java 侧用 ONNX Runtime 加载，实现 L2SentimentAnalyzer。")


if __name__ == "__main__":
    main()
