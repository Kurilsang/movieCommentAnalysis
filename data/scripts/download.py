"""Download Leoooooops/douban_movie_comments from hf-mirror into data/raw/."""
import os
import sys
import requests

BASE = r"D:\Program Files\Projects\personal\movieCommentAnalysis\data"
RAW = os.path.join(BASE, "raw")
os.makedirs(RAW, exist_ok=True)

FILES = [
    "new_data.json",
    "README.md",
    ".gitattributes",
]
ENDPOINT = "https://hf-mirror.com/datasets/Leoooooops/douban_movie_comments/resolve/main/"

for fname in FILES:
    url = ENDPOINT + fname
    dest = os.path.join(RAW, fname)
    if os.path.exists(dest) and os.path.getsize(dest) > 0:
        print(f"skip (exists): {dest} ({os.path.getsize(dest)} bytes)")
        continue
    print(f"downloading {url} -> {dest}")
    with requests.get(url, stream=True, timeout=120, allow_redirects=True) as r:
        r.raise_for_status()
        total = 0
        with open(dest, "wb") as f:
            for chunk in r.iter_content(chunk_size=1 << 20):
                f.write(chunk)
                total += len(chunk)
                print(f"\r  {total/1048576:.1f} MB", end="", flush=True)
        print()
    print(f"done: {dest} ({os.path.getsize(dest)} bytes)")
print("ALL DONE")
