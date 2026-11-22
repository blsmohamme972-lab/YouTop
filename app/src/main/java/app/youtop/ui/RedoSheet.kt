package app.youtop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.youtop.R
import app.youtop.data.Feedback

private val PartLabels = mapOf(
    "picture" to R.string.part_picture,
    "spotlight" to R.string.part_spotlight,
    "circles" to R.string.part_circles,
    "story" to R.string.part_story,
    "voice" to R.string.part_voice,
    "explanation" to R.string.part_explanation,
    "question" to R.string.part_question,
    "answer" to R.string.part_answer,
    "tags" to R.string.part_tags,
    "symbols" to R.string.part_symbols,
    "english" to R.string.part_english,
    "other" to R.string.part_other,
)

/**
 * "Redo": the owner ticks what is wrong in this lesson and writes what should change.
 * [onSubmit] gets the ticked part codes (see [Feedback.Parts]) and the text.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RedoSheet(title: String, where: String, onSubmit: (List<String>, String) -> Unit, onDismiss: () -> Unit) {
    var parts by rememberSaveable { mutableStateOf(listOf<String>()) }
    var text by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Surface1.copy(alpha = 0.98f),
        contentColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.45f),
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.redo_title), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(title, color = Faint, fontSize = 13.sp, maxLines = 2)
            if (where.isNotBlank()) Text(stringResource(R.string.redo_where, where), color = Faint, fontSize = 12.sp, maxLines = 2)
            Label(stringResource(R.string.redo_parts))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Feedback.Parts.forEach { code ->
                    Chip(stringResource(PartLabels.getValue(code)), code in parts) {
                        parts = if (code in parts) parts - code else Feedback.Parts.filter { it in parts || it == code }
                    }
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.redo_text)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Accent, unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = Accent, unfocusedLabelColor = Faint,
                    cursorColor = Accent,
                ),
            )
            Text(stringResource(R.string.redo_hint), color = Faint, fontSize = 12.sp, lineHeight = 17.sp)
            PrimaryButton(
                stringResource(R.string.redo_send),
                icon = Icons.AutoMirrored.Rounded.Send,
                modifier = Modifier.fillMaxWidth(),
                enabled = parts.isNotEmpty() || text.isNotBlank(),
            ) { onSubmit(parts, text.trim()) }
        }
    }
}
