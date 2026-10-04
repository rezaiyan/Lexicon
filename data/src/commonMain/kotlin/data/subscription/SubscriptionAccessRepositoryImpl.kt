package data.subscription

import core.common.Try
import core.common.map
import data.auth.remote.IFeatureAccessRemoteDataSource
import domain.subscription.ISubscriptionAccessRepository

class SubscriptionAccessRepositoryImpl(
    private val featureAccessRemoteDataSource: IFeatureAccessRemoteDataSource,
) : ISubscriptionAccessRepository {

    override suspend fun syncWithServer(): Try<Unit> =
        featureAccessRemoteDataSource.syncWithStore().map { }

    override suspend fun refresh(force: Boolean): Try<Unit> = featureAccessRemoteDataSource.refresh(force)
}
