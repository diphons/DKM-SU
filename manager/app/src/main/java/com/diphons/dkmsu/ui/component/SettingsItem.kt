package com.diphons.dkmsu.ui.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun SwitchItem(
    icon: ImageVector? = null,
    title: String? = null,
    summary: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    color: Color,
    fontSize: TextUnit,
    fontSizeSum: TextUnit,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    ListItem(
        modifier = Modifier
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                role = Role.Switch,
                enabled = enabled,
                indication = LocalIndication.current,
                onValueChange = onCheckedChange
            ),
        colors = ListItemDefaults.colors(color),
        headlineContent = {
            if (title != null) {
                Text(
                    text = title,
                    fontSize = fontSize
                )
            }
        },
        leadingContent = icon?.let {
            { Icon(icon, title) }
        },
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
                interactionSource = interactionSource
            )
        },
        supportingContent = {
            if (summary != null) {
                Text(
                    text = summary,
                    fontSize = fontSizeSum
                )
            }
        }
    )
}

@Composable
fun SwitchItem(
    icon: ImageVector? = null,
    title: String? = null,
    summary: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    color: Color? = null,
    fontSize: TextUnit? = null,
    fontSizeSum: TextUnit? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    val color: Color = color ?: Color.Transparent
    val checkfontsize: TextUnit = fontSize ?: 16.sp
    val checkfontsizesum: TextUnit = fontSizeSum ?: 14.sp
    SwitchItem(icon, title, summary, checked, enabled, color, checkfontsize, checkfontsizesum, onCheckedChange)
}

@Composable
fun RadioItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(title)
        },
        leadingContent = {
            RadioButton(selected = selected, onClick = onClick)
        }
    )
}
