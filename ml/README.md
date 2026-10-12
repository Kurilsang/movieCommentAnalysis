# ml — Python 离线训练管线（预留，暂不启用）

此目录承载 Python 离线训练与 ONNX 导出管线。**推理期不需要 Python。**

## 目录约定

- `export/` — ONNX 导出脚本（`optimum-cli export onnx` + int8 量化）
- `train/` — 模型训练/微调脚本（预留，暂不启用）
- `eval/` — 评测脚本（测试集加载、指标计算、混淆矩阵）
- `data/` — 训练/评测数据集

## 已知风险

`transformers` 5.x 可能破坏 `optimum` 的 ONNX 导出。若导出失败，需固定 `transformers` 到 4.x 版本。详见 `docs/决策记录.md` §9.3。

## 诚实红线

脚本若从未跑通，**不得**在答辩中声称已训练。宁可只展示"管线已就绪"。
