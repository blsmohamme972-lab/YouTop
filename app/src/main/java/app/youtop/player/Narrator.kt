package app.youtop.player

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.youtop.data.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

enum class NarrationMode { NONE, AUDIO, TTS, TEXT }

data class NarrationState(
    val key: String? = null,
    val mode: NarrationMode = NarrationMode.NONE,
    val playing: Boolean = false,
    val segment: Int = -1,
    val progress: Float = 0f,
    val ended: Boolean = false,
    val speed: Float = 1f,
    val muted: Boolean = false,
)

/**
 * Plays one narration track and reports which sentence is being spoken, so the
 * spotlight and captions can follow it.
 *
 * - AUDIO: an uploaded audio file, cues come from each segment's start time.
 * - TTS:   the device's text-to-speech voice, one utterance per segment.
 * - TEXT:  no voice available for the language; segments advance at reading pace.
 *
 * All methods must be called on the main thread.
 */
class Narrator(context: Context, private val scope: CoroutineScope) {
    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(NarrationState())
    val state: StateFlow<NarrationState> = _state

    private var track: Track? = null
    private var starts = LongArray(0)        // AUDIO: start of each segment in ms
    private var weights = FloatArray(0)      // cumulative share of text before each segment
    private var gen = 0                      // invalidates callbacks from earlier utterances
    private var ticker: Job? = null

    private var player: ExoPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeak: (() -> Unit)? = null

    // ---- public controls -------------------------------------------------

    fun load(key: String, track: Track, audio: Uri?, autoPlay: Boolean = true) {
        if (_state.value.key == key) return
        stopOutput()
        this.track = track
        val total = track.segments.sumOf { it.text.length }.coerceAtLeast(1)
        var acc = 0
        weights = FloatArray(track.segments.size) { i -> (acc.toFloat() / total).also { acc += track.segments[i].text.length } }
        starts = LongArray(0)
        val mode = when {
            track.segments.isEmpty() && audio == null -> NarrationMode.NONE
            audio != null -> NarrationMode.AUDIO
            else -> NarrationMode.TTS
        }
        _state.update { NarrationState(key = key, mode = mode, speed = it.speed, muted = it.muted) }
        if (mode == NarrationMode.AUDIO) prepareAudio(audio!!)
        if (autoPlay) play()
    }

    fun unload() {
        stopOutput()
        track = null
        _state.update { NarrationState(speed = it.speed, muted = it.muted) }
    }

    fun toggle() = if (_state.value.playing) pause() else play()

    fun play() {
        val s = _state.value
        if (s.mode == NarrationMode.NONE) return
        if (s.ended) return replay()
        _state.update { it.copy(playing = true) }
        when (s.mode) {
            NarrationMode.AUDIO -> { player?.play(); startAudioTicker() }
            else -> speakFrom(s.segment.coerceAtLeast(0))
        }
    }

    fun pause() {
        if (!_state.value.playing) return
        _state.update { it.copy(playing = false) }
        when (_state.value.mode) {
            NarrationMode.AUDIO -> player?.pause()
            else -> stopSpeech()
        }
    }

    fun replay() = seekToSegment(0)

    fun seekToSegment(index: Int) {
        val n = track?.segments?.size ?: return
        if (n == 0) return seekTo(0f)
        val i = index.coerceIn(0, n - 1)
        when (_state.value.mode) {
            NarrationMode.AUDIO -> {
                _state.update { it.copy(ended = false, playing = true, segment = i) }
                player?.let { p -> p.seekTo(starts.getOrElse(i) { 0L }); p.play() }
                startAudioTicker()
            }
            NarrationMode.NONE -> Unit
            else -> {
                _state.update { it.copy(ended = false, playing = true) }
                speakFrom(i)
            }
        }
    }

    fun seekTo(fraction: Float) {
        val f = fraction.coerceIn(0f, 1f)
        when (_state.value.mode) {
            NarrationMode.AUDIO -> player?.let { p ->
                val d = p.duration.takeIf { it > 0 } ?: return
                p.seekTo((f * d).toLong())
                _state.update { it.copy(progress = f, ended = false, segment = segmentAt((f * d).toLong())) }
                if (_state.value.playing) startAudioTicker()
            }
            NarrationMode.NONE -> Unit
            else -> {
                val i = weights.indexOfLast { it <= f }.coerceAtLeast(0)
                if (_state.value.playing) {
                    _state.update { it.copy(ended = false) }
                    speakFrom(i)
                } else {
                    _state.update { it.copy(ended = false, segment = i, progress = weights.getOrElse(i) { 0f }) }
                }
            }
        }
    }

    fun setSpeed(speed: Float) {
        _state.update { it.copy(speed = speed) }
        player?.setPlaybackSpeed(speed)
        if (_state.value.playing && _state.value.mode != NarrationMode.AUDIO) speakFrom(_state.value.segment.coerceAtLeast(0))
    }

    fun setMuted(muted: Boolean) {
        _state.update { it.copy(muted = muted) }
        player?.volume = if (muted) 0f else 1f
        if (_state.value.playing && _state.value.mode == NarrationMode.TTS) speakFrom(_state.value.segment.coerceAtLeast(0))
    }

    /** Reads a one-off text aloud (quiz questions); pauses the narration first. */
    fun say(text: String, lang: String) {
        pause()
        withTts {
            val engine = tts ?: return@withTts
            if (!setLanguage(engine, lang)) return@withTts
            engine.setSpeechRate(1f)
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "say")
        }
    }

    fun stopSaying() {
        pendingSpeak = null
        tts?.stop()
    }

    fun release() {
        stopOutput()
        player?.release(); player = null
        tts?.shutdown(); tts = null
    }

    // ---- audio file ------------------------------------------------------

    private fun prepareAudio(uri: Uri) {
        val p = player ?: ExoPlayer.Builder(appContext).build().also { created ->
            created.setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
                true,
            )
            created.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) computeStarts()
                    if (playbackState == Player.STATE_ENDED && _state.value.mode == NarrationMode.AUDIO) {
                        ticker?.cancel()
                        _state.update { it.copy(playing = false, ended = true, progress = 1f) }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    // Audio missing or unreachable: carry on with the device voice.
                    if (_state.value.mode != NarrationMode.AUDIO) return
                    ticker?.cancel()
                    val wasPlaying = _state.value.playing
                    _state.update { it.copy(mode = NarrationMode.TTS) }
                    if (wasPlaying) speakFrom(_state.value.segment.coerceAtLeast(0))
                }
            })
            player = created
        }
        p.setMediaItem(MediaItem.fromUri(uri))
        p.setPlaybackSpeed(_state.value.speed)
        p.volume = if (_state.value.muted) 0f else 1f
        p.prepare()
    }

    /** Segment start times: taken from the lesson when given, otherwise spread by text length. */
    private fun computeStarts() {
        val segs = track?.segments ?: return
        val duration = player?.duration?.takeIf { it > 0 } ?: return
        starts = if (segs.isNotEmpty() && segs.all { it.t != null }) {
            LongArray(segs.size) { segs[it].t!! }
        } else {
            LongArray(segs.size) { (weights[it] * duration).toLong() }
        }
    }

    private fun segmentAt(positionMs: Long): Int {
        if (starts.isEmpty()) return -1
        var i = starts.indexOfLast { it <= positionMs }
        if (i < 0) i = 0
        return i
    }

    private fun startAudioTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val p = player ?: break
                val d = p.duration
                if (d > 0) {
                    val pos = p.currentPosition
                    _state.update { it.copy(progress = (pos.toFloat() / d).coerceIn(0f, 1f), segment = segmentAt(pos)) }
                }
                if (!_state.value.playing) break
                delay(33)
            }
        }
    }

    // ---- device voice ----------------------------------------------------

    private fun withTts(block: () -> Unit) {
        if (ttsReady) return block()
        pendingSpeak = block
        if (tts != null) return
        tts = TextToSpeech(appContext) { status ->
            scope.launch(Dispatchers.Main) {
                if (status == TextToSpeech.SUCCESS) {
                    ttsReady = true
                    tts?.setOnUtteranceProgressListener(listener)
                    pendingSpeak?.invoke()
                } else if (_state.value.mode == NarrationMode.TTS) {
                    _state.update { it.copy(mode = NarrationMode.TEXT) }
                    if (_state.value.playing) speakFrom(_state.value.segment.coerceAtLeast(0))
                }
                pendingSpeak = null
            }
        }
    }

    private fun setLanguage(engine: TextToSpeech, lang: String): Boolean {
        val result = engine.setLanguage(Locale.forLanguageTag(lang))
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    private fun speakFrom(index: Int) {
        val t = track ?: return
        ticker?.cancel()
        val myGen = ++gen
        if (index >= t.segments.size) {
            _state.update { it.copy(playing = false, ended = true, progress = 1f) }
            return
        }
        _state.update { it.copy(segment = index, progress = weights[index]) }
        if (_state.value.mode == NarrationMode.TEXT) return readSilently(index, myGen)
        withTts {
            if (myGen != gen) return@withTts
            val engine = tts ?: return@withTts
            if (!setLanguage(engine, t.lang)) {
                _state.update { it.copy(mode = NarrationMode.TEXT) }
                return@withTts readSilently(index, myGen)
            }
            engine.setSpeechRate(_state.value.speed)
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, if (_state.value.muted) 0f else 1f)
            }
            engine.speak(t.segments[index].text, TextToSpeech.QUEUE_FLUSH, params, "$myGen:$index")
        }
    }

    private fun readSilently(index: Int, myGen: Int) {
        val t = track ?: return
        val text = t.segments[index].text
        ticker = scope.launch {
            val total = (text.length * 75L).coerceAtLeast(1800L)
            var elapsed = 0f
            while (elapsed < total) {
                delay(50)
                elapsed += 50 * _state.value.speed
                _state.update { it.copy(progress = progressWithin(index, elapsed / total)) }
            }
            if (myGen == gen && _state.value.playing) speakFrom(index + 1)
        }
    }

    private fun progressWithin(index: Int, fraction: Float): Float {
        val start = weights.getOrElse(index) { 0f }
        val end = weights.getOrElse(index + 1) { 1f }
        return start + (end - start) * fraction.coerceIn(0f, 1f)
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            val (g, i) = parse(utteranceId) ?: return
            scope.launch(Dispatchers.Main) {
                val len = track?.segments?.getOrNull(i)?.text?.length ?: return@launch
                if (g == gen && len > 0) _state.update { it.copy(progress = progressWithin(i, start.toFloat() / len)) }
            }
        }

        override fun onDone(utteranceId: String?) {
            val (g, i) = parse(utteranceId) ?: return
            scope.launch(Dispatchers.Main) {
                if (g == gen && _state.value.playing) speakFrom(i + 1)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            // The voice failed on this sentence: keep going with text only.
            val (g, i) = parse(utteranceId) ?: return
            scope.launch(Dispatchers.Main) {
                if (g != gen) return@launch
                _state.update { it.copy(mode = NarrationMode.TEXT) }
                if (_state.value.playing) speakFrom(i)
            }
        }

        private fun parse(id: String?): Pair<Int, Int>? {
            val parts = id?.split(':') ?: return null
            if (parts.size != 2) return null
            return (parts[0].toIntOrNull() ?: return null) to (parts[1].toIntOrNull() ?: return null)
        }
    }

    private fun stopSpeech() {
        gen++
        ticker?.cancel()
        pendingSpeak = null
        tts?.stop()
    }

    private fun stopOutput() {
        stopSpeech()
        player?.stop()
        player?.clearMediaItems()
    }
}
