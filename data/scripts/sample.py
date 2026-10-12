# -*- coding: utf-8 -*-
"""
Build sample dataset from data/raw/new_data.json (Leoooooops/douban_movie_comments).

Source schema (instruction-tuning JSON):
  {"instruction": "写一段关于 \"<电影名>\" 的影评", "input": "", "output": "我给他打 N 星，<评论文本>"}

Outputs:
  data/comments.csv : movieId,text,author,publishTime,likeCount  (20 movies x 200 comments)
  data/movies.csv   : movieId,title,director,actors,year,posterUrl,synopsis
"""
import csv
import hashlib
import json
import os
import re
from collections import defaultdict

BASE = r"D:\Program Files\Projects\personal\movieCommentAnalysis\data"
SRC = os.path.join(BASE, "raw", "new_data.json")
COMMENTS_OUT = os.path.join(BASE, "comments.csv")
MOVIES_OUT = os.path.join(BASE, "movies.csv")

N_MOVIES = 20
PER_MOVIE = 200
MIN_TEXT_LEN = 5  # characters, after stripping
SEED = 42

title_re = re.compile(r'"([^"]+)"')
rating_re = re.compile(r'^\u6211\u7ed9\u4ed6\u6253\s*([0-9.]+)\s*\u661f\s*[\uff0c,:\uff1a.]?\s*', re.S)

with open(SRC, encoding="utf-8") as f:
    data = json.load(f)
print("loaded rows:", len(data))

# ---- parse ----
by_movie = defaultdict(list)   # title -> list of (idx, text, rating)
stats = {"no_title": 0, "no_rating_prefix": 0, "too_short": 0, "dup_dropped": 0}
seen_text = defaultdict(set)

for idx, r in enumerate(data):
    instr = r.get("instruction", "") or ""
    out = r.get("output", "") or ""
    m = title_re.search(instr)
    if not m:
        stats["no_title"] += 1
        continue
    title = m.group(1).strip()
    rm = rating_re.match(out)
    if rm:
        rating = rm.group(1)
        text = out[rm.end():]
    else:
        stats["no_rating_prefix"] += 1
        rating = ""
        text = out
    text = text.strip()
    if len(text) < MIN_TEXT_LEN:
        stats["too_short"] += 1
        continue
    key = text
    if key in seen_text[title]:
        stats["dup_dropped"] += 1
        continue
    seen_text[title].add(key)
    by_movie[title].append((idx, text, rating))

print("parse stats:", stats)
print("distinct movies:", len(by_movie))

# ---- choose movies: top 20 by qualifying comment count, deterministic tie-break ----
ranked = sorted(by_movie.items(), key=lambda kv: (-len(kv[1]), kv[0]))
eligible = [(t, rows) for t, rows in ranked if len(rows) >= PER_MOVIE]
print("movies with >= %d comments: %d" % (PER_MOVIE, len(eligible)))
chosen = eligible[:N_MOVIES]
assert len(chosen) == N_MOVIES, "not enough eligible movies"


def stable_id(title: str) -> str:
    return "M" + hashlib.md5(title.encode("utf-8")).hexdigest()[:8]


import random
rng = random.Random(SEED)

comment_rows = []
movie_rows = []
for title, rows in chosen:
    mid = stable_id(title)
    picks = rng.sample(rows, PER_MOVIE)
    picks.sort(key=lambda x: x[0])  # keep original dataset order
    for idx, text, rating in picks:
        comment_rows.append([mid, text, "\u533f\u540d", "", 0])
    movie_rows.append([mid, title, "", "", "", "", ""])

with open(COMMENTS_OUT, "w", encoding="utf-8", newline="") as f:
    w = csv.writer(f)
    w.writerow(["movieId", "text", "author", "publishTime", "likeCount"])
    w.writerows(comment_rows)

with open(MOVIES_OUT, "w", encoding="utf-8", newline="") as f:
    w = csv.writer(f)
    w.writerow(["movieId", "title", "director", "actors", "year", "posterUrl", "synopsis"])
    w.writerows(movie_rows)

print("\nwrote", COMMENTS_OUT, "rows:", len(comment_rows))
print("wrote", MOVIES_OUT, "rows:", len(movie_rows))
print("\nchosen movies:")
for title, rows in chosen:
    print(f"  {stable_id(title)}  available={len(rows):5d}  {title}")

# ---- verification ----
import collections
cnt = collections.Counter(r[0] for r in comment_rows)
assert all(v == PER_MOVIE for v in cnt.values())
assert all(len(r[1]) >= MIN_TEXT_LEN for r in comment_rows)
assert len(comment_rows) == N_MOVIES * PER_MOVIE
print("\nVERIFIED: %d movies x %d comments = %d rows, all text >= %d chars"
      % (len(cnt), PER_MOVIE, len(comment_rows), MIN_TEXT_LEN))
