package feature.profile.navigation

import androidx.compose.runtime.Composable
import feature.profile.ProfileViewModel
import feature.profile.ui.components.DeleteAccountCoolingContent
import feature.profile.ui.components.DeleteAccountHiddenContent
import org.koin.compose.viewmodel.koinViewModel
import overlay.OverlayHost
import overlay.OverlayNavigator
import overlay.bottomsheet.BottomSheetPageConfig
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.BottomSheetProperties
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import overlay.bottomsheet.showSizeToFitBottomSheet

/** Confirm, then a cooling-off countdown, then delete. Opened from Settings so it stays out of the way. */
fun OverlayHost.showDeleteAccountSheet() {
    showSizeToFitBottomSheet(tag = "delete-account") { sheetNav ->
        DeleteAccountSheetContent(sheetNav)
    }
}

private sealed interface DeleteAccountPage {
    data object Confirm : DeleteAccountPage
    data object Cooling : DeleteAccountPage
}

@Composable
private fun DeleteAccountSheetContent(sheetNav: OverlayNavigator) {
    val pages = rememberBottomSheetPageNavigator<DeleteAccountPage>(DeleteAccountPage.Confirm)
    val profileViewModel = koinViewModel<ProfileViewModel>()

    BottomSheetPages(
        navigator = pages,
        onClose = { sheetNav.dismiss() },
        pageConfig = { page ->
            BottomSheetPageConfig(
                showBackButton = page == DeleteAccountPage.Cooling,
                showCloseButton = false,
                properties = BottomSheetProperties(),
            )
        },
    ) { page ->
        when (page) {
            DeleteAccountPage.Confirm -> DeleteAccountHiddenContent(
                onConfirm = { pages.navigateTo(DeleteAccountPage.Cooling) },
                onDismiss = { sheetNav.dismiss() },
            )

            DeleteAccountPage.Cooling -> DeleteAccountCoolingContent(
                onConfirm = {
                    sheetNav.dismiss()
                    profileViewModel.deleteAccount()
                },
                onDismiss = { sheetNav.dismiss() },
            )
        }
    }
}
