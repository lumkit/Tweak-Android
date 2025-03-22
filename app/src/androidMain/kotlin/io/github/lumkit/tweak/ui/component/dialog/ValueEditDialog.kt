package io.github.lumkit.tweak.ui.component.dialog

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.lumkit.tweak.R

@SuppressLint("DefaultLocale")
@Composable
fun ValueEditDialog(
    visible: Boolean,
    title: @Composable () -> Unit,
    value: String,
    unit: String = "dp",
    onDismissRequest: (String) -> Unit,
) {
    if (visible) {
        var text by rememberSaveable { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { onDismissRequest("") },
            title = title,
            text = {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = text,
                    onValueChange = {
                        text = it
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.text_please_input_value)
                        )
                    },
                    placeholder = {
                        Text(
                            text = buildString {
                                append(value)
                                append(unit)
                            }
                        )
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissRequest(text)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.text_confirm)
                    )
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = {
                        onDismissRequest("")
                    }
                ) {
                    Text(
                        text = stringResource(R.string.text_cancel)
                    )
                }
            }
        )
    }
}