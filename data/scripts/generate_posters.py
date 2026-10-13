#!/usr/bin/env python3
"""
为每部电影生成 SVG 占位封面。

为什么：源数据集没有海报，而前端原本回退到 via.placeholder.com（外部网络请求），
违反"演示路径绝不依赖实时网络请求"的铁律。这里生成本地 SVG，零网络依赖。

设计：基于片名哈希取确定性渐变色 + 片名文字。视觉上明确是"生成的封面"，
不伪装成真实海报。
"""

import hashlib
import sys
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8")

ROOT = Path(__file__).parent.parent.parent
CSV = ROOT / "data" / "movies.csv"
OUT_DIR = ROOT / "frontend" / "public" / "posters"

# 6 组配色（渐变起止色 + 文字色）
PALETTES = [
    ("#1e3a8a", "#3b82f6", "#ffffff"),   # 深蓝
    ("#7c2d12", "#ea580c", "#ffffff"),   # 橙棕
    ("#14532d", "#22c55e", "#ffffff"),   # 深绿
    ("#581c87", "#a855f7", "#ffffff"),   # 紫
    ("#7f1d1d", "#ef4444", "#ffffff"),   # 红
    ("#134e4a", "#14b8a6", "#ffffff"),   # 青
]


def palette_for(title: str):
    h = int(hashlib.md5(title.encode("utf-8")).hexdigest(), 16)
    return PALETTES[h % len(PALETTES)]


def wrap_title(title: str, max_chars: int = 13, max_lines: int = 5):
    """
    折行：中文按字符数，英文按单词边界（避免 "Zooto / pia" 这种断词）。

    策略：先把标题切成 token（英文单词 / 单个中文字 / 空格），
    逐个累加，超宽就换行。英文单词不拆分。
    """
    tokens = []
    buf = ""
    for ch in title:
        if ch.isascii() and (ch.isalnum() or ch in "'-&."):
            buf += ch
        else:
            if buf:
                tokens.append(buf)
                buf = ""
            tokens.append(ch)
    if buf:
        tokens.append(buf)

    lines, cur = [], ""
    for tok in tokens:
        if tok == " ":
            if cur and not cur.endswith(" "):
                cur += " "
            continue
        # 单个英文单词比整行还宽 → 只能硬断
        if len(tok) > max_chars:
            if cur.strip():
                lines.append(cur.rstrip())
                cur = ""
            while len(tok) > max_chars:
                lines.append(tok[:max_chars])
                tok = tok[max_chars:]
            cur = tok
            continue
        if len(cur) + len(tok) > max_chars:
            if cur.strip():
                lines.append(cur.rstrip())
            cur = tok
        else:
            cur += tok
        if len(lines) >= max_lines:
            break

    if cur.strip() and len(lines) < max_lines:
        lines.append(cur.rstrip())

    lines = [l for l in lines if l.strip()]
    if not lines:
        lines = [title[:max_chars]]

    # 被截断时加省略号
    joined = "".join(lines).replace(" ", "")
    if len(joined) < len(title.replace(" ", "")):
        lines[-1] = lines[-1].rstrip() + "…"
    return lines[:max_lines]


def make_svg(movie_id: str, title: str) -> str:
    c1, c2, fg = palette_for(title)
    lines = wrap_title(title)
    total = len(lines)
    line_h = 26
    start_y = 280 / 2 - (total - 1) * line_h / 2 + 2

    tspans = "\n".join(
        f'    <tspan x="100" y="{start_y + i * line_h:.0f}">{escape(l)}</tspan>'
        for i, l in enumerate(lines)
    )

    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="200" height="280" viewBox="0 0 200 280">
  <defs>
    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="{c1}"/>
      <stop offset="100%" stop-color="{c2}"/>
    </linearGradient>
  </defs>
  <rect width="200" height="280" fill="url(#g)"/>
  <rect x="10" y="10" width="180" height="260" fill="none" stroke="{fg}" stroke-opacity="0.22" stroke-width="1" rx="4"/>
  <text font-family="'Microsoft YaHei','PingFang SC','Noto Sans CJK SC',sans-serif"
        font-size="14" font-weight="600" fill="{fg}" text-anchor="middle" opacity="0.96">
{tspans}
  </text>
</svg>
'''


def escape(s: str) -> str:
    return (s.replace("&", "&amp;").replace("<", "&lt;")
             .replace(">", "&gt;").replace('"', "&quot;"))


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    lines = CSV.read_text(encoding="utf-8").splitlines()
    header = lines[0]

    out_lines = [header]
    count = 0
    for line in lines[1:]:
        if not line.strip():
            continue
        # 解析：movieId,"title",director,actors,year,posterUrl,synopsis
        parts = line.split(",", 2)
        movie_id = parts[0].strip()
        rest = parts[1] if len(parts) > 1 else ""
        # 剥掉标题的引号
        if rest.startswith('"'):
            end = rest.find('"', 1)
            title = rest[1:end]
            tail = rest[end + 1:]
        else:
            title = rest.split(",")[0]
            tail = rest[len(title):]

        # 生成封面
        svg = make_svg(movie_id, title)
        (OUT_DIR / f"{movie_id}.svg").write_text(svg, encoding="utf-8")
        count += 1

        # 重写 CSV：posterUrl 列填 /posters/{id}.svg
        # tail 形如 ",,,,," → 需要替换第 4 个字段（posterUrl）
        fields = tail.split(",")  # ['', '', '', '', ''] 对应 director,actors,year,posterUrl,synopsis
        while len(fields) < 5:
            fields.append("")
        fields[3] = f"/posters/{movie_id}.svg"
        new_line = f'{movie_id},"{title}",' + ",".join(fields[:5])
        out_lines.append(new_line)

    CSV.write_text("\n".join(out_lines) + "\n", encoding="utf-8")
    print(f"✅ 生成 {count} 个 SVG 封面到 {OUT_DIR}")
    print(f"✅ 更新 {CSV} 的 posterUrl 列")
    print("\n前 3 行 CSV：")
    for l in out_lines[:4]:
        print("  " + l)


if __name__ == "__main__":
    main()
