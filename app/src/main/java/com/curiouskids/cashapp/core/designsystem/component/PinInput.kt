package com.curiouskids.cashapp.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PinInput(
    pin: String,
    onPinChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    pinLength: Int = 6,
    isError: Boolean = false
) {
    BasicTextField(
        value = pin,
        onValueChange = {
            if (it.length <= pinLength) {
                onPinChange(it)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = modifier.fillMaxWidth()
            ) {
                repeat(pinLength) { index ->
                    val char = when {
                        index >= pin.length -> ""
                        else -> "•" // Masked character
                    }
                    val borderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    Box(
                        modifier = Modifier
                            .size(48.dp) // Minimum touch target size
                            .border(1.dp, borderColor, MaterialTheme.shapes.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
}
