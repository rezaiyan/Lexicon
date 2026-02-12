package utils

/**
 * String formatting utilities for Kotlin Multiplatform
 * Provides .format() extension that works across all platforms
 */

/**
 * Formats a string with the given arguments
 * Replaces %s and %d placeholders with provided arguments
 * Example: "Hello %s!".format("World") -> "Hello World!"
 * Example: "Count: %d".format(5) -> "Count: 5"
 */
fun String.format(vararg args: Any?): String {
    var result = this
    
    // Replace each argument in order
    for (arg in args) {
        // Try %d first (for integers)
        if (result.contains("%d")) {
            result = result.replaceFirst("%d", arg.toString())
        }
        // Then try %s (for strings)
        else if (result.contains("%s")) {
            result = result.replaceFirst("%s", arg.toString())
        }
    }
    
    return result
}

