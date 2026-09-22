import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/// Wraps a single, already-created UIViewController instance so it can be reused as the content
/// of multiple `Tab`s without SwiftUI creating a second, independent Compose composition.
struct EmbeddedViewController: UIViewControllerRepresentable {
    let controller: UIViewController

    func makeUIViewController(context: Context) -> UIViewController { controller }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    // Created once and shared across both native tabs so Compose's navigation graph, Koin
    // view models, and app state stay singular regardless of which tab is selected.
    @State private var composeController = MainViewControllerKt.MainViewController()
    @State private var selection: String = "study"
    // Native tab chrome only makes sense once Compose is past auth/onboarding/splash and
    // showing Study/Settings — Kotlin reports this via NativeTabBarBridge.
    @State private var showTabBar: Bool = false

    var body: some View {
        if #available(iOS 26.0, *), showTabBar {
            TabView(selection: $selection) {
                Tab("Study", systemImage: "book.fill", value: "study") {
                    EmbeddedViewController(controller: composeController)
                        .ignoresSafeArea(.all)
                }
                Tab("Settings", systemImage: "gearshape.fill", value: "settings") {
                    EmbeddedViewController(controller: composeController)
                        .ignoresSafeArea(.all)
                }
            }
            .onChange(of: selection) { _, newValue in
                MainViewControllerKt.onNativeTabSelected(tab: newValue)
            }
            .onAppear {
                MainViewControllerKt.setOnCurrentTabChanged { tab in
                    if selection != tab { selection = tab }
                }
            }
        } else {
            EmbeddedViewController(controller: composeController)
                .ignoresSafeArea(.all) // Edge-to-edge - extends under status bar and home indicator
                .background(Color.black) // Fallback background
                .onAppear {
                    MainViewControllerKt.setOnTabBarVisibilityChanged { visible in
                        showTabBar = visible.boolValue
                    }
                    MainViewControllerKt.setOnCurrentTabChanged { tab in
                        if selection != tab { selection = tab }
                    }
                }
        }
    }
}



