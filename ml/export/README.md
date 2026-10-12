# ONNX 导出脚本（ml/export/）

**用途**：把 `IDEA-CCNL/Erlangshen-Roberta-110M-Sentiment` 导出为 ONNX 并 int8 量化。

**重要**：这是**一次性**操作，导出后推理期不需要 Python。

## 前置条件

- Python 3.10–3.12
- 网络能访问 HuggingFace（或用镜像）

## 用法

```bash
cd ml/export
pip install -r requirements.txt
python export_onnx.py
```

## 输出

- `ml/models/sentiment.onnx`（int8 量化，约 100 MB）
- `ml/models/tokenizer/`（tokenizer 文件）

## 已知风险

`transformers` 5.x 可能破坏 `optimum` 导出。若报错，固定 `transformers` 到 4.x：
```bash
pip install "transformers<5" optimum onnxruntime
```
