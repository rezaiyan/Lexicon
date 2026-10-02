package feature.profile.ui

internal sealed interface ProfileSheetPage {
    data object EditProfile : ProfileSheetPage
    data object DeleteConfirm : ProfileSheetPage
    data object DeleteCooling : ProfileSheetPage
    data object Logout : ProfileSheetPage
}
