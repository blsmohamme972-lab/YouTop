"""Pictures and spotlight spots for lessons 31-40 of shahzad-2015, set after looking at each picture.

Usage:  python3 spots-0031-0040.py <exported chapter q0001-0100.json> [<folder with shahzad-q00NN.webp> <out folder>]
Sets "image" and one spot per sentence (story, then explanation); with the two folders it also draws every
circle on the picture, numbered (S = story, E = explanation), to check them by eye.
Each spot is (x, y, r): centre as fractions of width and height, radius as a fraction of the width; None = no spot.
The story text was NOT changed: the owner recorded these stories before the pictures existed.
"""
import json, sys

MEDIA = "gh:books/shahzad-2015/media/"
SPOTS = {
 31: ([(.44,.20,.30), (.15,.50,.25), (.30,.60,.12), (.78,.30,.22), (.92,.27,.10), (.31,.56,.14), (.72,.78,.20), (.20,.42,.20), (.44,.20,.30)],
      [(.78,.30,.22), (.92,.27,.10), (.31,.56,.14), (.72,.78,.20), (.78,.30,.22)]),
 32: ([(.50,.20,.50), (.20,.38,.25), (.20,.68,.25), (.68,.62,.28), (.40,.30,.12), (.65,.15,.30), (.09,.38,.11), (.40,.63,.15), None],
      [(.68,.62,.28), (.31,.25,.14), (.50,.20,.50), (.40,.25,.15), (.09,.38,.11)]),
 33: ([(.30,.36,.33), (.30,.36,.33), (.52,.30,.14), (.80,.30,.22), (.15,.72,.20), (.15,.20,.13), (.72,.84,.24), (.72,.10,.20), (.14,.36,.17), (.75,.62,.20)],
      [(.52,.30,.14), (.62,.25,.10), (.80,.30,.22), (.62,.25,.10), (.30,.36,.33), (.67,.60,.15)]),
 34: ([(.20,.62,.25), (.77,.38,.20), (.17,.30,.28), (.30,.14,.20), (.60,.57,.11), (.63,.47,.09), (.87,.52,.17), (.40,.45,.45), (.77,.38,.20)],
      [(.50,.50,.50), (.17,.30,.28), (.30,.14,.20), (.60,.57,.11), (.63,.47,.09), (.87,.52,.17), (.78,.47,.22)]),
 35: ([(.52,.33,.36), (.25,.30,.20), (.55,.35,.33), (.80,.27,.17), (.40,.33,.20), (.62,.15,.25), (.55,.52,.25), (.50,.47,.47), (.20,.75,.22), (.80,.75,.22)],
      [(.50,.40,.50), (.13,.30,.25), (.80,.27,.17), (.62,.15,.25), (.52,.33,.36), (.27,.30,.15), (.55,.52,.25)]),
 36: ([(.85,.65,.30), (.58,.33,.17), (.25,.30,.28), (.07,.31,.08), (.20,.32,.10), (.40,.32,.12), (.25,.335,.27), (.80,.24,.20), (.62,.57,.30), (.58,.33,.17)],
      [(.25,.30,.28), (.07,.31,.08), (.20,.32,.10), (.36,.32,.09), (.455,.33,.07), (.25,.335,.27), (.87,.55,.10)]),
 37: ([(.58,.33,.30), (.52,.47,.20), (.15,.50,.17), (.45,.62,.20), (.62,.32,.08), (.67,.70,.11), (.93,.55,.09), (.76,.20,.16), (.15,.50,.17), (.45,.62,.20)],
      [(.52,.47,.20), (.67,.70,.11), (.93,.55,.09), (.76,.20,.16), (.62,.32,.10)]),
 38: ([(.50,.47,.45), (.87,.30,.15), (.52,.38,.12), (.52,.50,.14), (.52,.59,.17), (.12,.38,.17), (.07,.60,.10), (.04,.575,.06), (.12,.38,.17)],
      [(.50,.47,.45), (.52,.38,.12), (.52,.50,.14), (.52,.59,.17), (.42,.38,.08)]),
 39: ([(.25,.22,.30), (.37,.09,.15), (.87,.60,.18), (.20,.48,.22), (.45,.65,.35), (.72,.28,.28), (.58,.34,.11), (.15,.90,.17), (.78,.72,.15), (.37,.09,.15)],
      [(.45,.65,.35), (.20,.48,.22), (.83,.31,.10), (.70,.30,.25), (.15,.90,.17), (.45,.65,.35)]),
 40: ([(.45,.30,.48), (.63,.42,.18), (.60,.32,.42), (.92,.30,.09), (.63,.33,.14), (.28,.33,.10), (.67,.66,.30), (.10,.52,.13), (.38,.63,.14), (.38,.63,.14)],
      [(.10,.52,.13), (.60,.32,.42), (.92,.30,.09), (.63,.33,.14), (.28,.33,.10), (.63,.42,.18), (.67,.66,.30)]),
}

def apply(chapter):
    for l in chapter["lessons"]:
        n = int(l["id"][-4:])
        if n not in SPOTS:
            continue
        l["image"] = f"{MEDIA}shahzad-q{n:04d}.webp"
        for track, spots in zip(l["tracks"], SPOTS[n]):
            segs = [s for s in track["segments"] if "quote" not in s]
            assert len(segs) == len(spots), (n, track["kind"], len(segs), len(spots))
            for seg, sp in zip(segs, spots):
                seg.pop("spot", None)
                if sp:
                    seg["spot"] = {"x": sp[0], "y": sp[1], "r": sp[2]}

def draw(media, out):
    from PIL import Image, ImageDraw
    for n, (story, expl) in SPOTS.items():
        for tag, spots, colour in (("S", story, (255, 220, 0)), ("E", expl, (0, 255, 255))):
            im = Image.open(f"{media}/shahzad-q{n:04d}.webp").convert("RGB")
            d = ImageDraw.Draw(im); w, h = im.size
            for i, sp in enumerate(spots, 1):
                if sp:
                    x, y, r = sp[0] * w, sp[1] * h, sp[2] * w
                    d.ellipse([x - r, y - r, x + r, y + r], outline=colour, width=5)
                    d.rectangle([x - 26, y - 20, x + 26, y + 20], fill=(0, 0, 0))
                    d.text((x - 20, y - 14), f"{tag}{i}", fill=colour, font_size=28)
            im.save(f"{out}/{n}-{tag}.jpg", quality=80)

if __name__ == "__main__":
    path = sys.argv[1]
    chapter = json.load(open(path, encoding="utf-8"))
    apply(chapter)
    json.dump(chapter, open(path, "w", encoding="utf-8"), ensure_ascii=False)
    if len(sys.argv) > 3:
        draw(sys.argv[2], sys.argv[3])
