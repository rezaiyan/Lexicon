package feature.profile.ui

internal sealed interface ProfileSheetPage {
    data object EditProfile : ProfileSheetPage
    data object Logout : ProfileSheetPage
}
