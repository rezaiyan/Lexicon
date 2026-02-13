import SwiftUI
import FirebaseCore
import FirebaseAnalytics
import FirebaseCrashlytics
import FirebaseMessaging
import UserNotifications
import GoogleSignIn
import AuthenticationServices
import ComposeApp

// MARK: - Notification Category Constants

enum NotificationCategory: String {
    case accountDeleted = "ACCOUNT_DELETED"
    case streakReminder = "STREAK_REMINDER"
    case reviewReminder = "REVIEW_REMINDER"
    case achievementUnlocked = "ACHIEVEMENT_UNLOCKED"
    case generic = "GENERIC"
    
    var identifier: String {
        return rawValue
    }
    
    var actions: [UNNotificationAction] {
        switch self {
        case .accountDeleted:
            return []
        case .streakReminder:
            return [
                UNNotificationAction(
                    identifier: "VIEW_PROGRESS",
                    title: "View Progress",
                    options: [.foreground]
                ),
                UNNotificationAction(
                    identifier: "DISMISS",
                    title: "Dismiss",
                    options: []
                )
            ]
        case .reviewReminder:
            return [
                UNNotificationAction(
                    identifier: "START_REVIEW",
                    title: "Start Review",
                    options: [.foreground]
                ),
                UNNotificationAction(
                    identifier: "REMIND_LATER",
                    title: "Remind Later",
                    options: []
                )
            ]
        case .achievementUnlocked:
            return [
                UNNotificationAction(
                    identifier: "VIEW_ACHIEVEMENT",
                    title: "View Achievement",
                    options: [.foreground]
                )
            ]
        case .generic:
            return []
        }
    }
    
    var options: UNNotificationCategoryOptions {
        switch self {
        case .accountDeleted:
            return []
        case .streakReminder, .reviewReminder, .achievementUnlocked:
            return [.customDismissAction]
        case .generic:
            return []
        }
    }
    
    static func from(userInfo: [AnyHashable: Any]) -> NotificationCategory {
        if let type = userInfo["type"] as? String {
            switch type {
            case "account_deleted":
                return .accountDeleted
            case "streak_reminder":
                return .streakReminder
            case "review_reminder":
                return .reviewReminder
            case "achievement_unlocked":
                return .achievementUnlocked
            default:
                return .generic
            }
        }
        
        // If no type, check category field - if nil or SYSTEM, treat as SYSTEM (display)
        let categoryString = userInfo["category"] as? String
        if categoryString == nil || categoryString == "SYSTEM" {
            return .generic  // Use generic category for system notifications
        }
        
        return .generic
    }
}

// MARK: - Notification Category Manager

class NotificationCategoryManager {
    static let shared = NotificationCategoryManager()
    
    private init() {}
    
    func registerCategories() {
        let categories = NotificationCategory.allCases.map { category in
            UNNotificationCategory(
                identifier: category.identifier,
                actions: category.actions,
                intentIdentifiers: [],
                options: category.options
            )
        }
        
        UNUserNotificationCenter.current().setNotificationCategories(Set(categories))
        print("✅ Registered \(categories.count) notification categories")
    }
}

extension NotificationCategory: CaseIterable {}

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        // Configure Firebase
        FirebaseApp.configure()
        
        // Configure Google Sign-In
        if let clientID = FirebaseApp.app()?.options.clientID {
            let config = GIDConfiguration(clientID: clientID)
            GIDSignIn.sharedInstance.configuration = config
            print("🔐 Google Sign-In configured with client ID: \(clientID)")
        } else {
            print("⚠️ Firebase client ID not found")
        }
        
        // Enable Analytics
        Analytics.setAnalyticsCollectionEnabled(true)
        print("📊 Firebase Analytics initialized")
        
        // Enable Crashlytics
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(true)
        print("🔥 Firebase Crashlytics initialized")
        
        // Set up notification delegate (but don't request permission yet)
        UNUserNotificationCenter.current().delegate = self
        
        // Register notification categories with actions
        NotificationCategoryManager.shared.registerCategories()
        
        // Register for remote notifications (APNs token)
        // This doesn't require permission, just gets the device token
        application.registerForRemoteNotifications()
        
        // Set Firebase Messaging delegate
        Messaging.messaging().delegate = self
        
        // Clear the app icon badge on startup
        application.applicationIconBadgeNumber = 0
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
        print("✅ Badge cleared on app launch")
        
        // Log app start event
        Analytics.logEvent("app_start", parameters: nil)
        
        // Initialize Apple Sign In notification handler (deferred to avoid build order issues)
        DispatchQueue.main.async {
            if #available(iOS 13.0, *) {
                _ = AppleSignInNotificationHandler.shared
                print("🍎 Apple Sign In handler initialized")
            }
        }
        
        return true
    }
    
    func applicationWillEnterForeground(_ application: UIApplication) {
        // Clear badge when app comes to foreground
        application.applicationIconBadgeNumber = 0
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
        print("✅ Badge cleared on foreground")
    }
    
    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        // Pass device token to Firebase Messaging
        Messaging.messaging().apnsToken = deviceToken
        
        let tokenParts = deviceToken.map { data in String(format: "%02.2hhx", data) }
        let token = tokenParts.joined()
        print("📱 Device Token: \(token)")
    }
    
    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("❌ Failed to register for remote notifications: \(error)")
    }
    
    // Handle remote notifications when app is in background or terminated
    // This method is REQUIRED for iOS to receive push notifications in background
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        Messaging.messaging().appDidReceiveMessage(userInfo)
        
        let category = NotificationCategory.from(userInfo: userInfo)
        let categoryString = userInfo["category"] as? String

        // Check if notification should be shown based on category
        if !MainViewControllerKt.shouldShowNotification(categoryValue: categoryString) {
            print("⏭️ Skipping background notification - user not authenticated. Category: \(category.identifier)")
            completionHandler(.newData)
            return
        }

        // Extract notification title and body from userInfo or aps payload
        var title = "Lexicon"
        var body = ""
        
        // Check for notification payload in aps
        if let aps = userInfo["aps"] as? [AnyHashable: Any],
           let alert = aps["alert"] as? [AnyHashable: Any] {
            title = (alert["title"] as? String) ?? title
            body = (alert["body"] as? String) ?? body
        } else if let aps = userInfo["aps"] as? [AnyHashable: Any],
                  let alert = aps["alert"] as? String {
            // Alert can be a string instead of dictionary
            body = alert
        } else {
            // Fallback: try to get title and body from Firebase notification payload
            title = userInfo["gcm.notification.title"] as? String ?? userInfo["title"] as? String ?? title
            body = userInfo["gcm.notification.body"] as? String ?? userInfo["body"] as? String ?? body
        }
        
        // Only display notification if we have a body
        if !body.isEmpty {
            // Create and display local notification to ensure it's shown
            let content = UNMutableNotificationContent()
            content.title = title
            content.body = body
            content.sound = .default
            content.categoryIdentifier = category.identifier
            content.userInfo = userInfo
            
            let request = UNNotificationRequest(
                identifier: UUID().uuidString,
                content: content,
                trigger: nil
            )
            
            UNUserNotificationCenter.current().add(request) { error in
                if let error = error {
                    print("❌ Failed to display notification: \(error.localizedDescription)")
                } else {
                    print("✅ Notification displayed: \(title) - \(body)")
                }
            }
        }

        // Handle account deletion notification
        if category == .accountDeleted {
            print("🗑️ Account deletion notification received in background - clearing local data")
            handleAccountDeletion()
        }
        
        // Notify completion handler
        completionHandler(.newData)
    }
    
    // Handle URL callbacks (for Google Sign-In)
    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey : Any] = [:]
    ) -> Bool {
        print("🔗 [AppDelegate] Handling URL: \(url)")
        return GIDSignIn.sharedInstance.handle(url)
    }
}


extension AppDelegate: UNUserNotificationCenterDelegate {
    // Handle notification when app is in foreground
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo
        print("📬 Notification received (foreground): \(userInfo)")
        
        let category = NotificationCategory.from(userInfo: userInfo)
        var categoryString = userInfo["category"] as? String
        
        // If category is nil, treat as SYSTEM (should be displayed)
        if categoryString == nil {
            categoryString = "SYSTEM"
            print("🔍 Category is nil, defaulting to SYSTEM")
        }
        
        print("🔍 Checking notification - Category: \(category.identifier), categoryString: \(categoryString ?? "nil")")
        
        // Check if notification should be shown based on category
        // If categoryString is nil, we already set it to SYSTEM above
        let shouldShow = MainViewControllerKt.shouldShowNotification(categoryValue: categoryString)
        print("🔍 shouldShowNotification result: \(shouldShow)")
        
        if !shouldShow {
            print("⏭️ Skipping foreground notification - user not authenticated. Category: \(category.identifier)")
            // Don't show notification if user is not authenticated
            completionHandler([])
            return
        }
        
        // Handle account deletion notification even when app is in foreground
        if category == .accountDeleted {
            print("🗑️ Account deletion notification received in foreground - clearing local data")
            handleAccountDeletion()
        }
        
        // Check notification authorization status
        center.getNotificationSettings { settings in
            print("🔔 Notification authorization status: \(settings.authorizationStatus.rawValue)")
            
            // Only display notification if authorized
            // Permission must be granted manually elsewhere
            if settings.authorizationStatus == .authorized {
                print("✅ Requesting foreground notification display - Category: \(category.identifier)")
                let options: UNNotificationPresentationOptions
                if #available(iOS 14.0, *) {
                    options = [.banner, .list, .badge, .sound]
                } else {
                    options = [.alert, .badge, .sound]
                }
                completionHandler(options)
            } else {
                print("⚠️ Notifications not authorized (status: \(settings.authorizationStatus.rawValue)) - skipping display")
                completionHandler([])
            }
        }
    }
    
    // Handle notification tap and action buttons
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        let category = NotificationCategory.from(userInfo: userInfo)
        let actionIdentifier = response.actionIdentifier
        
        print("🔔 Notification tapped - Category: \(category.identifier), Action: \(actionIdentifier)")
        
        // Handle action buttons
        if actionIdentifier != UNNotificationDefaultActionIdentifier && actionIdentifier != UNNotificationDismissActionIdentifier {
            handleNotificationAction(category: category, actionIdentifier: actionIdentifier, userInfo: userInfo)
            completionHandler()
            return
        }
        
        // Handle notification tap (default action)
        handleNotificationTap(category: category, userInfo: userInfo)
        
        completionHandler()
    }
    
    private func handleNotificationTap(category: NotificationCategory, userInfo: [AnyHashable: Any]) {
        switch category {
        case .accountDeleted:
            print("🗑️ Account deletion notification tapped - clearing local data")
            handleAccountDeletion()
        case .streakReminder:
            print("📅 Streak reminder tapped")
            postNotification(name: "NavigateToProgress", userInfo: userInfo)
        case .reviewReminder:
            print("📚 Review reminder tapped")
            postNotification(name: "StartReviewSession", userInfo: userInfo)
        case .achievementUnlocked:
            print("🏆 Achievement tapped")
            if let achievementId = userInfo["achievement_id"] as? String {
                postNotification(name: "ShowAchievementDetails", userInfo: ["achievement_id": achievementId])
            }
        case .generic:
            print("ℹ️ Generic notification tapped")
        }
    }
    
    private func handleNotificationAction(
        category: NotificationCategory,
        actionIdentifier: String,
        userInfo: [AnyHashable: Any]
    ) {
        switch category {
        case .streakReminder:
            switch actionIdentifier {
            case "VIEW_PROGRESS":
                print("📅 View Progress action tapped")
                postNotification(name: "NavigateToProgress", userInfo: userInfo)
            case "DISMISS":
                print("📅 Dismiss action tapped")
            default:
                break
            }
        case .reviewReminder:
            switch actionIdentifier {
            case "START_REVIEW":
                print("📚 Start Review action tapped")
                postNotification(name: "StartReviewSession", userInfo: userInfo)
            case "REMIND_LATER":
                print("📚 Remind Later action tapped")
                postNotification(name: "ScheduleReviewReminder", userInfo: ["delay_minutes": 60])
            default:
                break
            }
        case .achievementUnlocked:
            if actionIdentifier == "VIEW_ACHIEVEMENT" {
                print("🏆 View Achievement action tapped")
                if let achievementId = userInfo["achievement_id"] as? String {
                    postNotification(name: "ShowAchievementDetails", userInfo: ["achievement_id": achievementId])
                }
            }
        default:
            print("ℹ️ Action \(actionIdentifier) for category \(category.identifier)")
        }
    }
    
    private func postNotification(name: String, userInfo: [AnyHashable: Any] = [:]) {
        NotificationCenter.default.post(
            name: NSNotification.Name(name),
            object: nil,
            userInfo: userInfo
        )
    }
    
    // MARK: - Account Deletion Handling
    
    private func handleAccountDeletion() {
        print("🗑️ [iOS] Account deletion notification received")
        
        // CRITICAL FIX: Clear local data immediately when account deletion notification is received
        // This provides immediate protection against data persistence after account deletion
        
        // Call Kotlin function to clear all user data
        MainViewControllerKt.clearUserData()
        
        print("✅ [iOS] Account deletion notification handled - local data cleared immediately")
    }
}


extension AppDelegate: MessagingDelegate {
    func messaging(
        _ messaging: Messaging,
        didReceiveRegistrationToken fcmToken: String?
    ) {
        guard let fcmToken = fcmToken else { return }
        print("🔑 [iOS] FCM Token received: \(fcmToken)")
        
        // Send token to Kotlin push token manager
        // This will trigger the token registration with the backend
        MainViewControllerKt.notifyPushTokenReceived(token: fcmToken)
        
        // Also post notification for backward compatibility
        let dataDict: [String: String] = ["token": fcmToken]
        NotificationCenter.default.post(
            name: Notification.Name("FCMToken"),
            object: nil,
            userInfo: dataDict
        )
    }
}

@main
struct iOSApp: App {
    // Register app delegate for Firebase setup
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Handle Google Sign-In callback URL
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}

// MARK: - Apple Sign In Support

/**
 * Notification handler for Apple Sign In
 * Listens for requests from Kotlin and handles the Apple Sign In flow
 */
@available(iOS 13.0, *)
class AppleSignInNotificationHandler: NSObject {
    static let shared = AppleSignInNotificationHandler()
    
    private override init() {
        super.init()
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleSignInRequest),
            name: NSNotification.Name("StartAppleSignIn"),
            object: nil
        )
        print("🍎 [AppleSignInHandler] Initialized and listening")
    }
    
    @objc private func handleSignInRequest() {
        print("🍎 [AppleSignInHandler] Sign in request received")
        
        let provider = ASAuthorizationAppleIDProvider()
        let request = provider.createRequest()
        request.requestedScopes = [.fullName, .email]
        
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }
}

@available(iOS 13.0, *)
extension AppleSignInNotificationHandler: ASAuthorizationControllerDelegate {
    func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let tokenData = credential.identityToken,
              let token = String(data: tokenData, encoding: .utf8) else {
            NotificationCenter.default.post(
                name: NSNotification.Name("AppleSignInFailure"),
                object: nil,
                userInfo: ["error": "Failed to get identity token"]
            )
            return
        }
        
        var fullName: String? = nil
        if let given = credential.fullName?.givenName, let family = credential.fullName?.familyName {
            fullName = "\(given) \(family)"
        } else if let given = credential.fullName?.givenName {
            fullName = given
        }
        
        let appleUserId = credential.user
        let email = credential.email
        
        UserDefaults.standard.set(appleUserId, forKey: "appleUserIdentifier")
        
        var userInfo: [String: Any] = ["idToken": token]
        if let fullName = fullName {
            userInfo["fullName"] = fullName
        }
        userInfo["appleUserId"] = appleUserId
        if let email = email {
            userInfo["email"] = email
        }
        
        print("✅ [AppleSignInHandler] Success - posting notification to Kotlin")
        NotificationCenter.default.post(
            name: NSNotification.Name("AppleSignInSuccess"),
            object: nil,
            userInfo: userInfo
        )
    }
    
    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        print("❌ [AppleSignInHandler] Error: \(error.localizedDescription)")
        NotificationCenter.default.post(
            name: NSNotification.Name("AppleSignInFailure"),
            object: nil,
            userInfo: ["error": error.localizedDescription]
        )
    }
}

@available(iOS 13.0, *)
extension AppleSignInNotificationHandler: ASAuthorizationControllerPresentationContextProviding {
    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let window = scene.windows.first {
            return window
        }
        return UIWindow()
    }
}
