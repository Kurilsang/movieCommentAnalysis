# -*- coding: utf-8 -*-
"""Inspect structure of new_data.json: row count, fields, distinct movies."""
import json
import re
from collections import Counter

P = r"D:\Program Files\Projects\personal\movieCommentAnalysis\data\raw\new_data.json"
with open(P, encoding="utf-8") as f:
    data = json.load(f)

print("type:", type(data).__name__)
print("total rows:", len(data))

keys = Counter()
for r in data:
    keys[tuple(sorted(r.keys()))] += 1
print("field key combos:", keys)

print("\nfirst 3 records:")
for r in data[:3]:
    print(json.dumps(r, ensure_ascii=False)[:400])

# movie title from instruction
title_re = re.compile(r'[""\u201c]?([^\u201c\u201d"]+?)[""\u201d]?\s*(?:\u7535\u5f71|\u7684\u5f71\u8bc4)')
pat = re.compile(r'\u201c(.+?)\u201d')  # “...”
titles = Counter()
no_title = 0
for r in data:
    m = pat.search(r.get("instruction", ""))
    if m:
        titles[m.group(1)] += 1
    else:
        no_title += 1
print("\ndistinct movies:", len(titles), "| rows without quoted title:", no_title)
print("top 30 movies by comment count:")
for t, c in titles.most_common(30):
    print(f"  {c:6d}  {t}")

# rating prefix in output
rating_re = re.compile(r'^\u6211\u7ed9\s*(\d)\s*\u661f\s*[:：]')
rated = Counter()
for r in data:
    m = rating_re.match(r.get("output", ""))
    rated[m.group(1) if m else "NO_PREFIX"] += 1
print("\nrating prefix distribution:", dict(rated))

# instruction variants
instr_pat = Counter()
for r in data[:2000]:
    instr_pat[re.sub(r'\u201c.+?\u201d', '<T>', r.get("instruction", ""))] += 1
print("\ninstruction templates (first 2000 rows):")
for k, v in instr_pat.most_common(5):
    print(f"  {v:5d}  {k}")
