import csv, re, collections

LEX = {
    "剧情": ["剧情", "故事", "剧本", "情节"],
    "演技": ["演技", "表演", "演得", "演员", "主演", "角色"],
    "画面特效": ["画面", "特效", "视觉", "摄影", "镜头", "画质"],
    "配乐音效": ["音乐", "配乐", "音效", "BGM", "背景音乐", "原声"],
    "节奏": ["节奏", "剪辑"],
    "台词": ["台词", "对白", "独白", "对话"],
}
SPLIT = re.compile("但是|不过|然而|却|可是|只是|虽然|尽管")

rows = list(csv.DictReader(open("data/comments.csv", encoding="utf-8")))
print("total comments:", len(rows))
mid = "Mef6dbb82"
mine = [r for r in rows if r["movieId"] == mid]
print("Interstellar comments:", len(mine))

mention = collections.Counter()
for r in mine:
    text = (r["text"] or "").strip()
    clauses = [c.strip() for c in SPLIT.split(text) if c.strip()] or [text]
    hit = set()
    for cl in clauses:
        for a, words in LEX.items():
            if any(w in cl for w in words):
                hit.add(a)
    for a in hit:
        mention[a] += 1

for a in LEX:
    print(f"  {a}: mentioned by {mention[a]} comments")
