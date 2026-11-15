"""Cut one loudness-normalised mono AAC file per lesson and write the story timings.

usage: python3 cut_and_set.py recording.m4a align.json chapter.json media_dir
"""
import sys, json, subprocess, pathlib
rec, align_f, chapter_f, media = sys.argv[1:5]
al = json.load(open(align_f, encoding='utf-8'))
ch = json.load(open(chapter_f, encoding='utf-8'))
lessons = {l['id']: l for l in ch['lessons']}
pathlib.Path(media).mkdir(parents=True, exist_ok=True)
for st in al['stories']:
    lid = f"shahzad-q{st['story']:04d}"
    name = f"{lid}-story-ar.m4a"
    begin = max(0.0, st['begin']); shift = begin - st['begin']
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-ss', f"{begin:.3f}", '-to', f"{st['end']:.3f}", '-i', rec,
                    '-af', 'afade=t=in:d=0.05,loudnorm=I=-16:TP=-1.5:LRA=11', '-ac', '1', '-ar', '44100',
                    '-c:a', 'aac', '-b:a', '72k', '-movflags', '+faststart', f"{media}/{name}"], check=True)
    track = next(t for t in lessons[lid]['tracks'] if t['kind'] == 'story')
    segs = track['segments']
    assert [s['text'] for s in segs] == st['lines'][:-1], lid           # last line = quotation source
    assert segs[-1].get('quote') == st['lines'][-1], lid
    for s, t in zip(segs, st['starts']):
        s['t'] = max(0, int(round((t - shift) * 1000)))
    track['audio'] = f"gh:books/shahzad-2015/media/{name}"
    print(lid, [s['t'] for s in segs])
json.dump(ch, open(chapter_f, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
