package com.alirezaiyan.vokab.test.utils

import core.common.NoParamUseCase
import core.common.Try
import domain.settings.model.ReviewSettings
import domain.settings.usecase.GetReviewSettingsUseCase

/**
 * Helper to create GetReviewSettingsUseCase for testing.
 *
 * Since GetReviewSettingsUseCase always returns ReviewSettings.BALANCED,
 * for custom settings in tests, use [createCustomReviewSettingsUseCase].
 */
fun createTestReviewSettingsUseCase(): GetReviewSettingsUseCase {
    return GetReviewSettingsUseCase()
}

/**
 * Creates a NoParamUseCase<ReviewSettings> with custom settings for testing.
 */
fun createCustomReviewSettingsUseCase(
    settings: ReviewSettings = TestUtils.DEFAULT_TEST_SETTINGS
): NoParamUseCase<ReviewSettings> {
    return NoParamUseCase { Try.success(settings) }
}
