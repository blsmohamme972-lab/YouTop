package app.youtop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.KeyboardArrowRight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.youtop.MainViewModel
import app.youtop.R
import app.youtop.data.TagNode
import app.youtop.data.flatten
import app.youtop.study.Tags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt


private class TagRow(val node: TagNode, val depth: Int, val hasChildren: Boolean)

/**
 * The tag system as a tree, with how many lessons sit under each tag, how many are due and how
 * well they are going. Tapping a tag limits the feed to it ("focus"); several can be combined.
 */
@Composable
fun TopicsTab(vm: MainViewModel) {
    val taxonomy by vm.taxonomy.collectAsStateWithLifecycle()
    val books by vm.books.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val study = settings.study
    val focus = study.focus
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val setFocus: (Set<String>) -> Unit = { vm.setStudy(study.copy(focus = it)) }

    val stats by produceState(emptyMap<String, Tags.Stat>(), books, progress.cards, study.excludedBooks) {
        value = withContext(Dispatchers.Default) {
            val lessons = books.filter { it.id !in study.excludedBooks }.flatten().map { it.key to it.lesson.tags }
            Tags.stats(lessons, progress.cards, System.currentTimeMillis())
        }
    }
    // Topics with the highest share of wrong answers, most specific first.
    val weakest = remember(stats) {
        stats.entries.filter { ':' !in it.key && it.value.weakness != null && it.value.weakness!! > 0f }
            .sortedWith(compareByDescending<Map.Entry<String, Tags.Stat>> { it.value.weakness }.thenByDescending { it.key.count { c -> c == '/' } })
            .take(6).map { it.key }
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp), modifier = Modifier.navigationBarsPadding()) {
        item(key = "focus") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (focus.isEmpty()) stringResource(R.string.focus_all) else stringResource(R.string.focus_some, focus.size),
                    color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                )
                if (focus.isNotEmpty()) {
                    Text(
                        stringResource(R.string.focus_clear), color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.clip(CircleShape).background(Color.White).clickable { setFocus(emptySet()) }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            }
        }
        if (weakest.isNotEmpty()) {
            item(key = "weak") {
                Text(stringResource(R.string.weakest), color = Faint, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    weakest.forEach { tag ->
                        val selected = tag in focus
                        Text(
                            (taxonomy.titles[tag] ?: tag) + "  " + percent(1f - (stats[tag]?.weakness ?: 0f)),
                            color = if (selected) Color.Black else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.clip(CircleShape)
                                .background(if (selected) Accent else Color(0x33F87171))
                                .clickable { setFocus(if (selected) focus - tag else focus + tag) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
        taxonomy.facets.forEach { facet ->
            item(key = "facet:${facet.id}") {
                Text(
                    facet.title.uppercase(), color = Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 4.dp),
                )
            }
            val rows = ArrayList<TagRow>()
            fun walk(nodes: List<TagNode>, depth: Int) {
                for (n in nodes) {
                    rows += TagRow(n, depth, n.children.isNotEmpty())
                    if (expanded[n.id] == true) walk(n.children, depth + 1)
                }
            }
            walk(facet.nodes, 0)
            items(rows.size, key = { rows[it].node.id }) { i ->
                val row = rows[i]
                val tag = row.node.id
                val stat = stats[tag]
                val selected = tag in focus
                val open = expanded[tag] == true
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { setFocus(if (selected) focus - tag else focus + tag) }
                        .padding(start = (12 + row.depth * 18).dp, end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).clickable(enabled = row.hasChildren) { expanded[tag] = !open },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (row.hasChildren) {
                            Icon(if (open) Icons.Rounded.ExpandMore else Icons.Rounded.KeyboardArrowRight, null, tint = Faint, modifier = Modifier.size(20.dp))
                        }
                    }
                    Text(
                        row.node.title,
                        color = when {
                            selected -> Accent
                            stat == null -> Color.White.copy(alpha = 0.45f)
                            else -> Color.White
                        },
                        fontSize = 15.sp, fontWeight = if (row.depth == 0 || selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                    )
                    if (stat != null) {
                        Spacer(Modifier.width(8.dp))
                        stat.weakness?.let {
                            Text(percent(1f - it), color = if (it > 0.3f) Color(0xFFF87171) else Color(0xFF4ADE80), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(8.dp))
                        }
                        if (stat.due > 0) {
                            Text(
                                "${stat.due}", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(CircleShape).background(Accent).padding(horizontal = 7.dp, vertical = 1.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("${stat.seen}/${stat.lessons}", color = Faint, fontSize = 12.sp)
                    }
                    if (selected) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.CheckCircle, null, tint = Accent, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

private fun percent(v: Float) = "${(v * 100).roundToInt()}%"
