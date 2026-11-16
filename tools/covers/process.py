"""Turns the chosen pictures into the app's topic covers: cropped on the busiest part, drawn in the
specialty's colour on the app's dark ground so all of them look like one set."""
import json, pathlib, sys
import numpy as np
from PIL import Image, ImageFilter
src, choice_file, out = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2]), pathlib.Path(sys.argv[3])
out.mkdir(parents=True, exist_ok=True)
index = json.loads((src / "index.json").read_text()); choice = json.loads(choice_file.read_text())
TINT = {"cardiac": (255, 150, 165), "thoracic": (110, 205, 255), "vascular": (190, 165, 255)}
DARK = np.array([16, 14, 13], float)
W, H = 640, 400
credits = []
for tag, pick in choice.items():
    cand = index[pick.split(":")[0]][int(pick.split(":")[1])] if isinstance(pick, str) else index[tag][pick]
    im = Image.open(src / cand["file"])
    if im.mode in ("RGBA", "LA", "P"):
        im = im.convert("RGBA"); bg = Image.new("RGBA", im.size, "white"); bg.alpha_composite(im); im = bg
    g = np.asarray(im.convert("L"), float) / 255
    edge = np.concatenate([g[:6].ravel(), g[-6:].ravel(), g[:, :6].ravel(), g[:, -6:].ravel()])
    if np.median(edge) > 0.5: g = 1 - g                    # ink on paper (a light border): make the lines the light part
    lo, hi = np.percentile(g, 2), np.percentile(g, 99.5)
    g = np.clip((g - lo) / max(hi - lo, 1e-3), 0, 1)
    h, w = g.shape
    ch = min(h, round(w * H / W)); cw = min(w, round(ch * W / H))
    # the window with the most drawing in it
    best, by, bx = -1, 0, 0
    for y in range(0, h - ch + 1, max(1, (h - ch) // 12 or 1)):
        for x in range(0, w - cw + 1, max(1, (w - cw) // 12 or 1)):
            v = g[y:y + ch, x:x + cw].sum()
            if v > best: best, by, bx = v, y, x
    g = g[by:by + ch, bx:bx + cw]
    g = np.asarray(Image.fromarray((g * 255).astype("uint8")).resize((W, H), Image.LANCZOS), float) / 255
    g = g ** 0.85
    tint = np.array(TINT[tag.split("/")[0]], float)
    rgb = DARK + (tint - DARK) * g[..., None]
    Image.fromarray(rgb.astype("uint8")).save(out / f"{tag.replace('/', '-')}.webp", "WEBP", quality=68, method=6)
    credits.append((tag, cand["title"], cand["license"], cand["page"]))
(out.parent / "credits.json").write_text(json.dumps(credits, ensure_ascii=False, indent=1))
print(len(credits), "covers,", sum(f.stat().st_size for f in out.glob("*.webp")) // 1024, "KB")
