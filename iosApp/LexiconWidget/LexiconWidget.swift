// LexiconWidget.swift
// WidgetKit extension for the Lexicon vocabulary app.
//
// SETUP REQUIRED (Xcode):
// 1. In Xcode, File > New > Target > Widget Extension
//    - Product Name: LexiconWidget
//    - Uncheck "Include Configuration App Intent"
// 2. Replace the generated Swift files with this file
// 3. Add an App Group (e.g., "group.com.alirezaiyan.vokab") to both
//    the main app target and this widget target under Signing & Capabilities
// 4. In the widget target's General tab, set the minimum deployment target
//    to match the main app (iOS 17.0+)
// 5. Ensure the widget target's Bundle Identifier is:
//    com.alirezaiyan.vokab.LexiconWidget

import WidgetKit
import SwiftUI

// MARK: - Data Model

struct DailyWordEntry: TimelineEntry {
    let date: Date
    let word: String
    let translation: String
    let streakCount: Int
    let dueCardCount: Int
    let isPlaceholder: Bool

    static var placeholder: DailyWordEntry {
        DailyWordEntry(
            date: Date(),
            word: "Wanderlust",
            translation: "Love of travel",
            streakCount: 7,
            dueCardCount: 5,
            isPlaceholder: true
        )
    }

    static var empty: DailyWordEntry {
        DailyWordEntry(
            date: Date(),
            word: "",
            translation: "",
            streakCount: 0,
            dueCardCount: 0,
            isPlaceholder: false
        )
    }
}

// MARK: - Timeline Provider

struct DailyWordProvider: TimelineProvider {

    // App Group identifier for sharing data between the main app and widget.
    // The main app writes widget data to UserDefaults in this suite.
    private let suiteName = "group.com.alirezaiyan.vokab"

    func placeholder(in context: Context) -> DailyWordEntry {
        .placeholder
    }

    func getSnapshot(in context: Context, completion: @escaping (DailyWordEntry) -> Void) {
        let entry = loadEntry()
        completion(entry)
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<DailyWordEntry>) -> Void) {
        let entry = loadEntry()

        // Refresh at midnight so the word changes daily
        let calendar = Calendar.current
        let tomorrow = calendar.startOfDay(for: calendar.date(byAdding: .day, value: 1, to: Date())!)

        let timeline = Timeline(entries: [entry], policy: .after(tomorrow))
        completion(timeline)
    }

    /// Reads widget data from the shared App Group UserDefaults.
    /// The Kotlin/KMP side writes this data whenever the app opens or
    /// syncs word/streak data.
    private func loadEntry() -> DailyWordEntry {
        guard let defaults = UserDefaults(suiteName: suiteName) else {
            return .empty
        }

        let word = defaults.string(forKey: "widget_word") ?? ""
        let translation = defaults.string(forKey: "widget_translation") ?? ""
        let streak = defaults.integer(forKey: "widget_streak")
        let dueCount = defaults.integer(forKey: "widget_due_count")

        if word.isEmpty {
            return .empty
        }

        return DailyWordEntry(
            date: Date(),
            word: word,
            translation: translation,
            streakCount: streak,
            dueCardCount: dueCount,
            isPlaceholder: false
        )
    }
}

// MARK: - Widget View

struct DailyWordWidgetView: View {
    var entry: DailyWordEntry

    @Environment(\.widgetFamily) var family

    private let primaryColor = Color(red: 0.424, green: 0.129, blue: 0.863) // #6C21DC

    var body: some View {
        if entry.word.isEmpty && !entry.isPlaceholder {
            emptyStateView
        } else {
            contentView
        }
    }

    private var contentView: some View {
        VStack(alignment: .leading, spacing: 4) {
            // Header
            HStack {
                Text("Lexicon")
                    .font(.caption2)
                    .fontWeight(.bold)
                    .foregroundColor(primaryColor)
                Spacer()
                Label("\(entry.streakCount)", systemImage: "flame.fill")
                    .font(.caption)
                    .fontWeight(.bold)
                    .foregroundColor(.orange)
            }

            Spacer().frame(height: 4)

            Text("Word of the Day")
                .font(.caption2)
                .foregroundColor(.secondary)

            Text(entry.word)
                .font(.title2)
                .fontWeight(.bold)
                .lineLimit(1)
                .minimumScaleFactor(0.7)

            Text(entry.translation)
                .font(.callout)
                .foregroundColor(.secondary)
                .lineLimit(1)

            Spacer()

            // Footer
            if entry.dueCardCount > 0 {
                Text("\(entry.dueCardCount) cards due for review")
                    .font(.caption2)
                    .foregroundColor(primaryColor)
            } else {
                Text("All caught up!")
                    .font(.caption2)
                    .foregroundColor(.green)
            }
        }
        .padding()
        .redacted(reason: entry.isPlaceholder ? .placeholder : [])
    }

    private var emptyStateView: some View {
        VStack(spacing: 8) {
            Text("Lexicon")
                .font(.headline)
                .fontWeight(.bold)
                .foregroundColor(primaryColor)
            Text("Open app to set up your words")
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
    }
}

// MARK: - Widget Configuration

struct LexiconDailyWordWidget: Widget {
    let kind: String = "LexiconDailyWordWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: DailyWordProvider()) { entry in
            DailyWordWidgetView(entry: entry)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("Daily Word")
        .description("Shows your daily vocabulary word and study streak.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

// MARK: - Widget Bundle

@main
struct LexiconWidgetBundle: WidgetBundle {
    var body: some Widget {
        LexiconDailyWordWidget()
    }
}

// MARK: - Previews

#Preview(as: .systemSmall) {
    LexiconDailyWordWidget()
} timeline: {
    DailyWordEntry.placeholder
    DailyWordEntry(
        date: Date(),
        word: "Schadenfreude",
        translation: "Pleasure from another's misfortune",
        streakCount: 12,
        dueCardCount: 3,
        isPlaceholder: false
    )
}

#Preview(as: .systemMedium) {
    LexiconDailyWordWidget()
} timeline: {
    DailyWordEntry.placeholder
}
