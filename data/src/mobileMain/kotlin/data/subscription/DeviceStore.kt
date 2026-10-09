package data.subscription

import domain.subscription.model.SubscriptionStore

/** The store purchases on this device go through, and whose management page it can open. */
internal expect val deviceStore: SubscriptionStore
