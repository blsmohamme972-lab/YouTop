"""Align the recorded reading of stories 31-40 to the story text.

usage: python3 align.py reading.txt words.json chapter.json out.json
words.json: faster-whisper "small" (language ar) word timestamps of the 16 kHz mono recording.
Character-level difflib match of normalised Arabic; each sentence starts at the time of its first
matched character's word; if its first words were not recognised, it starts right after the
previous sentence's last recognised word.
"""
import sys, json, re, difflib

def norm(s):
    s = re.sub(r'[ً-ْٰـ]', '', s)
    s = re.sub(r'[إأآٱ]', 'ا', s).replace('ى', 'ي').replace('ة', 'ه').replace('ؤ', 'و').replace('ئ', 'ي')
    return re.sub(r'[^ء-ي0-9A-Za-z]', '', s)

reading, words_f, chapter_f, out_f = sys.argv[1:5]
lines = open(reading, encoding='utf-8').read().split('\n')
start = next(i for i, l in enumerate(lines) if l.startswith('القصة '))
stories = []
for l in lines[start:]:
    l = l.strip()
    if not l: continue
    if l.startswith('القصة ') and len(l.split()) <= 3:
        stories.append({'title': l, 'lines': []})
    else:
        stories[-1]['lines'].append(l)
assert len(stories) == 10, len(stories)

# reference: a list of units (title / sentence), each a list of normalised words
units = []
for si, st in enumerate(stories):
    units.append((si, -1, st['title']))
    for li, l in enumerate(st['lines']):
        units.append((si, li, l))
ref, ref_unit, ref_word = [], [], []
for ui, (_, _, text) in enumerate(units):
    for wi, w in enumerate(text.split()):
        for ch in norm(w):
            ref.append(ch); ref_unit.append(ui); ref_word.append(wi)

hyp_words = json.load(open(words_f, encoding='utf-8'))
hyp, hyp_idx = [], []
for k, w in enumerate(hyp_words):
    for ch in norm(w['w']):
        hyp.append(ch); hyp_idx.append(k)

sm = difflib.SequenceMatcher(None, ref, hyp, autojunk=False)
first = {}   # unit -> (word index in unit, start time)
last = {}    # unit -> end time
for a, b, n in sm.get_matching_blocks():
    if n < 3: continue   # ignore stray 1-2 character coincidences
    for j in range(n):
        u, wi, h = ref_unit[a + j], ref_word[a + j], hyp_words[hyp_idx[b + j]]
        if u not in first or (wi, h['s']) < first[u]:
            first[u] = (wi, h['s']) if u not in first or wi < first[u][0] else first[u]
        last[u] = max(last.get(u, 0), h['e'])

times, prev_end, report = {}, 0.0, []
for ui, (si, li, text) in enumerate(units):
    if ui in first and first[ui][0] <= 1:
        t = first[ui][1]
    else:
        t = prev_end
        report.append(f'story {si+31} line {li}: first words not recognised, start after previous ({t:.2f}s)')
    t = max(t, prev_end - 0.3)
    times[ui] = t
    if ui in last: prev_end = max(prev_end, last[ui])
    times[(ui, 'end')] = prev_end

out = []
for si, st in enumerate(stories):
    us = [ui for ui, u in enumerate(units) if u[0] == si and u[1] >= 0]
    nxt = [ui for ui, u in enumerate(units) if u[0] == si + 1 and u[1] == -1]
    begin = times[us[0]] - 0.15
    end = min(times[(us[-1], 'end')] + 0.8, times[nxt[0]] - 0.05) if nxt else times[(us[-1], 'end')] + 1.2
    out.append({'story': si + 31, 'begin': round(begin, 3), 'end': round(end, 3),
                'lines': st['lines'], 'starts': [round(times[ui] - begin, 3) for ui in us]})
json.dump({'stories': out, 'report': report}, open(out_f, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print('\n'.join(report))
for o in out:
    print(o['story'], o['begin'], o['end'], [round(x, 1) for x in o['starts']])
