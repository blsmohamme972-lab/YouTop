package app.youtop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.youtop.MainViewModel
import app.youtop.R
import app.youtop.data.Facet
import app.youtop.data.TagNode
import app.youtop.data.flatten
import app.youtop.study.Scheduler
import app.youtop.study.Tags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone
import kotlin.math.roundToInt

/** Each specialty has its own colour and sign, so the three are told apart at a glance. */
private class Look(val color: Color, val icon: ImageVector)

private fun lookOf(tag: String) = when (tag.substringBefore('/')) {
    "cardiac" -> Look(Color(0xFFFB7185), Icons.Rounded.Favorite)
    "thoracic" -> Look(Color(0xFF38BDF8), Icons.Rounded.Air)
    "vascular" -> Look(Color(0xFFA78BFA), Icons.Rounded.WaterDrop)
    else -> Look(Accent, Icons.Rounded.Sell)
}

/** True on a phone set to Arabic: tags then show their Arabic names. */
@Composable
fun arabicInterface(): Boolean = LocalConfiguration.current.locales[0].language == "ar"

/**
 * Choosing what to study. On top, what the feed is limited to now and one button to go and study
 * it; then the three specialties as cards that open into their subtopics; then the other tag
 * groups (kind of knowledge, question style, level, flags) as chips. One tap chooses a tag, a
 * second tap lets it go. Every tag shows how many lessons it has, how many are due and how well
 * they are going.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopicsTab(vm: MainViewModel, onStudy: () -> Unit) {
    val taxonomy by vm.taxonomy.collectAsStateWithLifecycle()
    val books by vm.books.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val study = settings.study
    val focus = study.focus
    val arabic = arabicInterface()
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val setFocus: (Set<String>) -> Unit = { vm.setStudy(study.copy(focus = it)) }
    val toggle: (String) -> Unit = { setFocus(Tags.toggle(focus, it)) }

    val stats by produceState(emptyMap<String, Tags.Stat>(), books, progress.cards, study.excludedBooks) {
        value = withContext(Dispatchers.Default) {
            val lessons = books.filter { it.id !in study.excludedBooks }.flatten().map { it.key to it.lesson.tags }
            val now = System.currentTimeMillis()
            Tags.stats(lessons, progress.cards, now, Scheduler.dayEnd(now, TimeZone.getDefault().getOffset(now)))
        }
    }
    // How many lessons the chosen tags leave in the feed; null while it is being counted.
    val matching by produceState<Int?>(null, books, focus, study.excludedBooks) {
        value = null
        value = withContext(Dispatchers.Default) {
            books.filter { it.id !in study.excludedBooks }.sumOf { b -> b.chapters.sumOf { c -> c.lessons.count { Tags.matches(it.tags, focus) } } }
        }
    }
    // Topics with the highest share of wrong answers, most specific first.
    val weakest = remember(stats) {
        stats.entries.filter { ':' !in it.key && it.value.weakness != null && it.value.weakness!! > 0f }
            .sortedWith(compareByDescending<Map.Entry<String, Tags.Stat>> { it.value.weakness }.thenByDescending { it.key.count { c -> c == '/' } })
            .take(6).map { it.key }
    }

    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "focus") {
            Card(spacing = 12.dp) {
                if (focus.isEmpty()) {
                    Text(stringResource(R.string.focus_all), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.focus_all_hint), color = Faint, fontSize = 13.sp, lineHeight = 19.sp)
                } else {
                    SectionTitle(stringResource(R.string.focus_title))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        focus.sorted().forEach { tag ->
                            Row(
                                Modifier.clip(CircleShape).background(Accent).clickable(onClickLabel = stringResource(R.string.focus_remove)) { toggle(tag) }
                                    .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(taxonomy.label(tag, arabic) ?: tag, color = OnAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Rounded.Close, null, tint = OnAccent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val n = matching
                        PrimaryButton(
                            if (n == null) stringResource(R.string.focus_study) else pluralStringResource(R.plurals.focus_study_n, n, n),
                            Icons.Rounded.PlayArrow, Modifier.weight(1f), enabled = n != 0, onClick = onStudy,
                        )
                        SecondaryButton(stringResource(R.string.focus_clear)) { setFocus(emptySet()) }
                    }
                    if (matching == 0) Text(stringResource(R.string.nothing_matches_body), color = Bad, fontSize = 13.sp)
                }
            }
        }
        if (weakest.isNotEmpty()) {
            item(key = "weak") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                        Icon(Icons.Rounded.TrendingDown, null, tint = Bad, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.weakest), color = Faint, fontSize = 13.sp)
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        weakest.forEach { tag ->
                            TagChip(
                                taxonomy.label(tag, arabic) ?: tag, percent(1f - (stats[tag]?.weakness ?: 0f)),
                                selected = tag in focus, tint = Bad,
                            ) { toggle(tag) }
                        }
                    }
                }
            }
        }
        taxonomy.facets.forEachIndexed { index, facet ->
            if (facet.id == "topic") {
                items(facet.nodes.size, key = { "topic:${facet.nodes[it].id}:$it" }) { i ->
                    val node = facet.nodes[i]
                    val open = expanded[node.id] ?: (stats[node.id] != null || focus.any { Tags.under(it, node.id) })
                    Specialty(node, stats, focus, arabic, open, onOpen = { expanded[node.id] = !open }, onToggle = toggle)
                }
            } else {
                item(key = "facet:${facet.id}:$index") { FacetCard(facet, stats, focus, arabic, toggle) }
            }
        }
    }
}

/** One specialty: a header that chooses all of it, and its subtopics underneath. */
@Composable
private fun Specialty(node: TagNode, stats: Map<String, Tags.Stat>, focus: Set<String>, arabic: Boolean, open: Boolean, onOpen: () -> Unit, onToggle: (String) -> Unit) {
    val look = lookOf(node.id)
    val stat = stats[node.id]
    val selected = node.id in focus
    val shape = RoundedCornerShape(22.dp)
    val turn by animateFloatAsState(if (open) 180f else 0f, label = "turn")
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.verticalGradient(listOf(look.color.copy(alpha = 0.16f), Surface1)))
            .border(1.dp, if (selected) look.color else look.color.copy(alpha = 0.22f), shape),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = node.children.isNotEmpty(), onClick = onOpen).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(look.color.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                Icon(look.icon, null, tint = look.color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(node.label(arabic), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (node.children.isNotEmpty()) Icon(Icons.Rounded.ExpandMore, null, tint = Faint, modifier = Modifier.size(20.dp).rotate(turn))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (stat == null) stringResource(R.string.topic_empty) else ratio(stat.seen, stat.lessons),
                        color = Faint, fontSize = 12.sp,
                    )
                    if (stat != null && stat.due > 0) DueBadge(stat.due)
                    stat?.weakness?.let { Score(it) }
                }
                ProgressLine((stat?.seen ?: 0).toFloat() / (stat?.lessons ?: 1).coerceAtLeast(1), color = look.color)
            }
            Spacer(Modifier.width(12.dp))
            Tick(selected, look.color, stringResource(R.string.focus_choose, node.label(arabic))) { onToggle(node.id) }
        }
        AnimatedVisibility(visible = open && node.children.isNotEmpty()) {
            Column(Modifier.padding(bottom = 8.dp)) {
                node.children.forEach { child ->
                    val s = stats[child.id]
                    val on = child.id in focus
                    Row(
                        Modifier.fillMaxWidth().semantics { this.selected = on }
                            .clickable(role = Role.Checkbox) { onToggle(child.id) }
                            .background(if (on) look.color.copy(alpha = 0.12f) else Color.Transparent)
                            .padding(start = 18.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(if (s == null) Color.White.copy(alpha = 0.2f) else look.color))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            child.label(arabic),
                            color = if (s == null && !on) Color.White.copy(alpha = 0.45f) else Color.White,
                            fontSize = 15.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                        if (s != null) {
                            Spacer(Modifier.width(8.dp))
                            s.weakness?.let { Score(it); Spacer(Modifier.width(8.dp)) }
                            if (s.due > 0) { DueBadge(s.due); Spacer(Modifier.width(8.dp)) }
                            Text(ratio(s.seen, s.lessons), color = Faint, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Tick(on, look.color, null, small = true, onClick = null)
                    }
                }
            }
        }
    }
}

/** A flat group of tags (kind of knowledge, question style, level, flags) as chips with their lesson counts. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FacetCard(facet: Facet, stats: Map<String, Tags.Stat>, focus: Set<String>, arabic: Boolean, onToggle: (String) -> Unit) {
    Card(title = facet.label(arabic)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            facet.nodes.forEach { node ->
                val stat = stats[node.id]
                TagChip(node.label(arabic), stat?.let { num(it.lessons) }, selected = node.id in focus, dim = stat == null) { onToggle(node.id) }
            }
        }
    }
}

/** A tag to tap: its name, a small figure after it, a tick when chosen. */
@Composable
private fun TagChip(text: String, figure: String?, selected: Boolean, tint: Color = Color.White, dim: Boolean = false, onClick: () -> Unit) {
    val back by animateColorAsState(if (selected) Accent else tint.copy(alpha = if (tint == Color.White) 0.08f else 0.16f), label = "chip")
    val ink = if (selected) OnAccent else Color.White.copy(alpha = if (dim) 0.45f else 1f)
    Row(
        Modifier.clip(CircleShape).background(back)
            .border(1.dp, if (selected) Accent else tint.copy(alpha = if (dim) 0.08f else 0.18f), CircleShape)
            .semantics { this.selected = selected }
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) { Icon(Icons.Rounded.Check, null, tint = OnAccent, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)) }
        Text(text, color = ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        if (figure != null) {
            Spacer(Modifier.width(6.dp))
            Text(figure, color = if (selected) OnAccent.copy(alpha = 0.7f) else Faint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** The round tick that says a tag is chosen. With [onClick] it is a button of its own. */
@Composable
private fun Tick(on: Boolean, color: Color, label: String?, small: Boolean = false, onClick: (() -> Unit)?) {
    val size = if (small) 22.dp else 32.dp
    Box(
        Modifier.size(size).clip(CircleShape)
            .background(if (on) color else Color.Transparent)
            .border(1.5.dp, if (on) color else Color.White.copy(alpha = 0.35f), CircleShape)
            .then(if (onClick != null) Modifier.semantics { if (label != null) contentDescription = label; selected = on }.clickable(role = Role.Checkbox, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (on) Icon(Icons.Rounded.Check, null, tint = Ink, modifier = Modifier.size(if (small) 15.dp else 20.dp))
    }
}

@Composable
private fun DueBadge(due: Int) = Text(
    num(due), color = OnAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold,
    modifier = Modifier.clip(CircleShape).background(Accent).padding(horizontal = 7.dp, vertical = 1.dp),
)

/** Share answered right, red when the topic is going badly. */
@Composable
private fun Score(weakness: Float) = Text(percent(1f - weakness), color = if (weakness > 0.3f) Bad else Good, fontSize = 12.sp, fontWeight = FontWeight.Medium)

private fun percent(v: Float) = "⁦${num((v * 100).roundToInt())}%⁩"
