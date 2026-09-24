@file:Suppress("unused")

// Swift bridging functions - must be in default package for MainViewControllerKt naming
fun MainViewController() = com.alirezaiyan.vokab.MainViewController()

fun clearUserData() = com.alirezaiyan.vokab.clearUserData()

fun notifyPushTokenReceived(token: String) = com.alirezaiyan.vokab.notifyPushTokenReceived(token)

fun shouldShowNotification(categoryValue: String?): Boolean =
    com.alirezaiyan.vokab.shouldShowNotification(categoryValue)

fun warmup() = com.alirezaiyan.vokab.warmup()

