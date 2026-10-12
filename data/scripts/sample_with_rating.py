#!/usr/bin/env python3
"""
从 data/raw/new_data.json 重新采样，加 rating 列。

输出 data/comments.csv：movieId,text,author,publishTime,likeCount,rating
- rating: 1-5 星（从 output 前缀 "我给他打 N 星，" 解析）
- 三分类映射：4-5 星=正面，3 星=中性，1-2 星=负面

用法：python sample_with_rating.py
"""

import hashlib
import json
import random
import re
import sys
from collections import defaultdict
from pathlib import Path

# 强制 UTF-8 输出（Windows 控制台默认 GBK 会崩溃）
sys.stdout.reconfigure(encoding="utf-8")
sys.stderr.reconfigure(encoding="utf-8")

RAW_PATH = Path(__file__).parent.parent / "raw" / "new_data.json"
OUT_COMMENTS = Path(__file__).parent.parent / "comments.csv"
OUT_MOVIES = Path(__file__).parent.parent / "movies.csv"

MOVIES_COUNT = 20
PER_MOVIE = 200
SEED = 42

RATING_RE = re.compile(r"^我给他打\s*([1-5])\s*星[，,]\s*")


def movie_id(title: str) -> str:
    """M + md5(title)[:8]，稳定确定性。"""
    h = hashlib.md5(title.encode("utf-8")).hexdigest()
    return "M" + h[:8]


def main():
    print(f"[1/4] 加载 {RAW_PATH} ...")
    with open(RAW_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)
    print(f"  原始 {len(data)} 条")

    print("[2/4] 解析 + 过滤 ...")
    by_movie = defaultdict(list)
    for row in data:
        instr = row.get("instruction", "")
        output = row.get("output", "")
        # 提取电影名
        m = re.search(r'写一段关于\s*"([^"]+)"\s*的影评', instr)
        if not m:
            continue
        title = m.group(1)
        # 提取星级 + 剥离前缀
        rm = RATING_RE.match(output)
        if not rm:
            continue
        rating = int(rm.group(1))
        text = output[rm.end():].strip()
        # 过滤短文本和重复
        if len(text) < 5:
            continue
        by_movie[title].append((text, rating))

    print(f"  {len(by_movie)} 部电影，过滤后 {sum(len(v) for v in by_movie.values())} 条")

    print(f"[3/4] 采样 {MOVIES_COUNT} 部 × {PER_MOVIE} 条（seed={SEED}）...")
    # 按评论量取 top 20
    top_movies = sorted(by_movie.items(), key=lambda x: -len(x[1]))[:MOVIES_COUNT]
    rand = random.Random(SEED)

    lines = ["movieId,text,author,publishTime,likeCount,rating"]
    movie_rows = ["movieId,title,director,actors,year,posterUrl,synopsis"]
    seen = set()
    total = 0

    for title, comments in top_movies:
        mid = movie_id(title)
        # 去重
        unique = []
        for text, rating in comments:
            if text not in seen:
                seen.add(text)
                unique.append((text, rating))
        # 随机采样
        sampled = rand.sample(unique, min(PER_MOVIE, len(unique)))
        for text, rating in sampled:
            # CSV 转义
            escaped = '"' + text.replace('"', '""') + '"' if any(c in text for c in ',"\n') else text
            lines.append(f"{mid},{escaped},匿名,,0,{rating}")
            total += 1
        movie_rows.append(f"{mid},\"{title}\",,,,,")
        print(f"    {title}: {len(sampled)} 条")

    print(f"[4/4] 写出 {total} 条 ...")
    OUT_COMMENTS.write_text("\n".join(lines) + "\n", encoding="utf-8")
    OUT_MOVIES.write_text("\n".join(movie_rows) + "\n", encoding="utf-8")
    print(f"✅ {OUT_COMMENTS}: {total} 条")
    print(f"✅ {OUT_MOVIES}: {MOVIES_COUNT} 部电影")

    # 统计星级分布
    from collections import Counter
    dist = Counter()
    for line in lines[1:]:
        r = line.rsplit(",", 1)[-1]
        if r.isdigit():
            dist[int(r)] += 1
    print(f"\n星级分布: {dict(sorted(dist.items()))}")
    print(f"三分类映射: 正面(4-5星)={dist[4]+dist[5]}, 中性(3星)={dist[3]}, 负面(1-2星)={dist[1]+dist[2]}")


if __name__ == "__main__":
    main()
