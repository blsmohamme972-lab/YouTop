import sys, subprocess, numpy as np
from faster_whisper import WhisperModel
m = WhisperModel("small", device="cpu", compute_type="int8", cpu_threads=4)
for f in sys.argv[1:]:
    a = np.frombuffer(subprocess.run(['ffmpeg','-v','error','-i',f,'-ac','1','-ar','16000','-f','f32le','-'],capture_output=True,check=True).stdout, dtype=np.float32)
    head = ' '.join(s.text for s in m.transcribe(a[:16000*4], language='ar')[0])
    tail = ' '.join(s.text for s in m.transcribe(a[-16000*5:], language='ar')[0])
    print(f.split('/')[-1][8:13], f"{len(a)/16000:.1f}s | HEAD:", head, "| TAIL:", tail, flush=True)
