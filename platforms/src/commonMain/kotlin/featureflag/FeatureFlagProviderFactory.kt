package featureflag

import domain.featureflag.IFeatureFlagProvider

/**
 * KAN-20: Factory function to create platform-specific feature flag provider.
 */
expect fun createFeatureFlagProvider(): IFeatureFlagProvider
