"""Candidate cover pictures per topic: public-domain / CC0 results from Openverse, plus Gray's Anatomy (1918) plates."""
import json, subprocess, sys, urllib.parse, pathlib, time
OUT = pathlib.Path(sys.argv[1]); OUT.mkdir(exist_ok=True)
UA = "TopicCoverFetch/1.0 (personal study app)"
Q = {
 "cardiac": (["human heart anatomy illustration"], [490, 492]),
 "cardiac/foundations": (["heart chambers anatomy"], [493, 496, 498]),
 "cardiac/cpb": (["heart-lung machine", "open heart surgery"], []),
 "cardiac/coronary": (["coronary arteries", "coronary angiogram"], [492, 494]),
 "cardiac/aortic-valve": (["aortic valve"], [497, 495]),
 "cardiac/mitral-tricuspid": (["mitral valve", "heart valves"], [495, 498]),
 "cardiac/aorta": (["aorta anatomy", "aortic arch"], [505, 506]),
 "cardiac/congenital": (["fetal circulation", "congenital heart"], [502]),
 "cardiac/heart-failure": (["heart transplant", "artificial heart"], []),
 "cardiac/arrhythmia": (["electrocardiogram", "pacemaker"], [501]),
 "cardiac/perioperative": (["intensive care unit", "operating room surgery"], []),
 "cardiac/other": (["pericardium", "heart trauma"], [489]),
 "thoracic": (["lungs anatomy"], [970, 972]),
 "thoracic/foundations": (["thorax anatomy lungs", "spirometry"], [965, 973]),
 "thoracic/lung-cancer": (["lung cancer", "lung tumor x-ray"], []),
 "thoracic/lung-benign": (["tuberculosis lung x-ray", "pneumonia chest x-ray"], []),
 "thoracic/pleura": (["pleura", "pleural effusion"], [965, 967]),
 "thoracic/mediastinum": (["mediastinum", "thymus"], [968, 1178]),
 "thoracic/airway": (["trachea bronchi", "bronchoscopy"], [961, 962]),
 "thoracic/oesophagus": (["esophagus anatomy", "oesophagus"], [1032]),
 "thoracic/chest-wall": (["rib cage skeleton", "diaphragm anatomy"], [112, 391]),
 "thoracic/trauma": (["rib fracture x-ray", "pneumothorax"], []),
 "thoracic/transplant": (["lung transplant", "organ transplant surgery"], []),
 "vascular": (["arteries anatomy illustration", "circulatory system"], [505]),
 "vascular/foundations": (["artery vein anatomy", "blood vessel"], []),
 "vascular/aorta": (["abdominal aorta", "aortic aneurysm"], [531]),
 "vascular/carotid": (["carotid artery"], [507, 513]),
 "vascular/peripheral": (["femoral artery", "leg arteries anatomy"], [550, 551]),
 "vascular/visceral": (["mesenteric artery", "renal artery kidney"], [534, 1122]),
 "vascular/venous": (["varicose veins", "veins of leg anatomy"], [580, 581]),
 "vascular/access": (["hemodialysis", "dialysis machine"], []),
 "vascular/endovascular": (["stent", "angiography"], []),
 "vascular/trauma": (["tourniquet", "arm arteries anatomy"], [525, 523]),
 "vascular/other": (["subclavian artery", "vasculitis"], [520]),
}
def curl(url, out=None, tries=2, maxsize="6000000"):
    for i in range(tries):
        r = subprocess.run(["curl", "-sS", "-L", "-m", "45", "--max-filesize", maxsize, "-A", UA, url] + (["-o", str(out)] if out else []), capture_output=True)
        if r.returncode == 0: return r.stdout if not out else True
        time.sleep(3)
    return None
def is_image(f):
    return f.exists() and f.stat().st_size > 3000 and f.read_bytes()[:4] in (b"\xff\xd8\xff\xe0", b"\xff\xd8\xff\xe1", b"\xff\xd8\xff\xdb", b"\xff\xd8\xff\xee", b"\x89PNG", b"RIFF", b"GIF8", b"\xff\xd8\xff\xe2")
index = {}
for tag, (queries, grays) in Q.items():
    cands = []
    for n in grays:
        cands.append({"title": f"File:Gray{n}.png", "url": f"https://commons.wikimedia.org/wiki/Special:FilePath/Gray{n}.png?width=640",
                      "page": f"https://commons.wikimedia.org/wiki/File:Gray{n}.png", "license": "Public domain (Gray's Anatomy, 1918)", "creator": "Henry Vandyke Carter"})
    for q in queries:
        raw = curl("https://api.openverse.org/v1/images/?" + urllib.parse.urlencode({"q": q, "license": "cc0,pdm", "page_size": 5}))
        time.sleep(3.5)
        try: results = json.loads(raw).get("results", [])
        except Exception: print(tag, "search failed", (raw or b"")[:80]); continue
        for r in results:
            url = r["url"]
            if "upload.wikimedia.org" in url:
                url = "https://commons.wikimedia.org/wiki/Special:FilePath/" + urllib.parse.quote(urllib.parse.unquote(url.rsplit("/", 1)[1])) + "?width=640"
            cands.append({"title": r.get("title") or "", "url": url, "page": r.get("foreign_landing_url"), "license": (r.get("license") or "") + " " + (r.get("license_version") or ""), "creator": r.get("creator") or ""})
    kept = []
    for c in cands:
        if len(kept) >= 8: break
        f = OUT / f"{tag.replace('/', '-')}_{len(kept)}.img"
        curl(c["url"], f); time.sleep(0.5)
        if is_image(f): c["file"] = f.name; kept.append(c)
        elif f.exists(): f.unlink()
    index[tag] = kept
    print(tag, len(kept), flush=True)
    (OUT / "index.json").write_text(json.dumps(index, ensure_ascii=False, indent=1))
