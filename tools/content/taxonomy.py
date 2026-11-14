#!/usr/bin/env python3
"""Builds app/src/main/assets/taxonomy.json from content/taxonomy.txt and checks it for mistakes."""
import json, pathlib, re, sys

root = pathlib.Path(__file__).resolve().parents[2]
src = root / "content" / "taxonomy.txt"
out = root / "app" / "src" / "main" / "assets" / "taxonomy.json"

def load(path=src):
    facets, stack, seen = [], [], set()
    for n, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not raw.strip() or raw.lstrip().startswith("#"):
            continue
        parts = [p.strip() for p in raw.strip().lstrip("@").split("|")]
        if raw.startswith("@"):
            fid, prefix, title = parts[0], parts[1], parts[2]
            facets.append({"id": fid, "prefix": prefix, "title": title, **({"titleAr": parts[3]} if len(parts) > 3 else {}), "nodes": []})
            stack = []
            continue
        indent = len(raw) - len(raw.lstrip(" "))
        if indent % 2 or not facets:
            sys.exit(f"taxonomy.txt:{n}: bad indentation")
        depth = indent // 2
        if depth > len(stack):
            sys.exit(f"taxonomy.txt:{n}: indented too far")
        if not re.fullmatch(r"[a-z0-9-]+", parts[0]) or len(parts) < 2 or not parts[1]:
            sys.exit(f"taxonomy.txt:{n}: expected 'id | Title' with a lowercase id")
        stack = stack[:depth]
        tag = facets[-1]["prefix"] + "/".join([s["_slug"] for s in stack] + [parts[0]])
        if tag in seen:
            sys.exit(f"taxonomy.txt:{n}: duplicate tag {tag}")
        seen.add(tag)
        node = {"id": tag, "title": parts[1], **({"titleAr": parts[2]} if len(parts) > 2 and parts[2] else {}), "_slug": parts[0]}
        (stack[-1].setdefault("children", []) if stack else facets[-1]["nodes"]).append(node)
        stack.append(node)
    def strip(nodes):
        for node in nodes:
            node.pop("_slug")
            strip(node.get("children", []))
    for f in facets:
        strip(f["nodes"])
    return {"version": 1, "facets": facets}, seen

if __name__ == "__main__":
    taxonomy, tags = load()
    out.write_text(json.dumps(taxonomy, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"{len(tags)} tags -> {out.relative_to(root)}")
