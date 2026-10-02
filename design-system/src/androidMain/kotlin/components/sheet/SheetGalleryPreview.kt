package components.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import theme.LexiconTheme
import theme.Theme

/*
 * Sheet kit gallery. Check every component at default size, at fontScale 1.3 and at 360dp width
 * before shipping a change to `components.sheet`.
 */

@Composable
private fun SheetFrame(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    LexiconTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(Theme.spacing.md)) { content() }
        }
    }
}

@Composable
private fun SampleFormPage() {
    SheetPage(
        title = "Edit word",
        subtitle = "Changes sync to all your devices.",
        onBack = {},
        onClose = {},
        footer = {
            SheetFooterRow(
                secondary = { SheetTonalButton(text = "Cancel", onClick = {}, modifier = it) },
                primary = { SheetPrimaryButton(text = "Save changes", onClick = {}, modifier = it) },
            )
        },
    ) {
        SheetField(label = "Word", value = "die Verspätung", onValueChange = {})
        SheetField(label = "Note", value = "", onValueChange = {}, optionalSuffix = "optional", placeholder = "Example")
        SheetField(
            label = "Email",
            value = "rh@example.com",
            onValueChange = {},
            readOnly = true,
            supportingText = "Signed in with Google",
        )
    }
}

@Composable
private fun SampleRowsPage() {
    SheetPage(title = "Theme", eyebrow = "Appearance", subtitle = "Choose how Lexicon looks.", onClose = {}) {
        SheetGroup {
            SheetRadioRow("Light", selected = true, onClick = {}, showDivider = true, icon = Icons.Default.LightMode)
            SheetRadioRow("Dark", selected = false, onClick = {}, showDivider = false, icon = Icons.Default.DarkMode)
        }
        SheetGroup {
            SheetSwitchRow(
                "Study reminders",
                checked = true,
                onCheckedChange = {},
                showDivider = true,
                subtitle = "At your best study time",
                icon = Icons.Default.Notifications,
            )
            SheetOptionRow(
                Icons.Default.Edit,
                "Edit profile",
                onClick = {},
                showDivider = false,
                subtitle = "Name and photo",
            )
        }
        StepProgressBar(current = 2, total = 5)
    }
}

@Preview(name = "Form")
@Composable
private fun FormPreview() = SheetFrame { SampleFormPage() }

@Preview(name = "Form · 360dp · font 1.3", widthDp = 360, fontScale = 1.3f)
@Composable
private fun FormLargeFontPreview() = SheetFrame { SampleFormPage() }

@Preview(name = "Rows")
@Composable
private fun RowsPreview() = SheetFrame { SampleRowsPage() }

@Preview(name = "Rows · dark")
@Composable
private fun RowsDarkPreview() = SheetFrame(darkTheme = true) { SampleRowsPage() }

@Preview(name = "Confirm · danger")
@Composable
private fun ConfirmDangerPreview() = SheetFrame {
    ConfirmSheetContent(
        icon = Icons.Default.DeleteOutline,
        title = "Delete word?",
        message = "\"die Verspätung\" will be permanently removed. This cannot be undone.",
        confirmText = "Delete",
        onConfirm = {},
        dismissText = "Cancel",
        onDismiss = {},
        tone = ConfirmTone.Danger,
        onClose = {},
    )
}

@Preview(name = "Confirm · brand · 360dp · font 1.3", widthDp = 360, fontScale = 1.3f)
@Composable
private fun ConfirmBrandLargeFontPreview() = SheetFrame {
    ConfirmSheetContent(
        icon = Icons.AutoMirrored.Filled.Logout,
        title = "Logout",
        message = "Are you sure you want to logout?",
        confirmText = "Logout",
        onConfirm = {},
        dismissText = "Cancel",
        onDismiss = {},
    )
}
