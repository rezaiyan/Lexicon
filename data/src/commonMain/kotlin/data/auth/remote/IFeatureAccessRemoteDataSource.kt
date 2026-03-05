package data.auth.remote

import domain.auth.model.FeatureAccessResponse
import kotlinx.coroutines.flow.Flow

interface IFeatureAccessRemoteDataSource {
    fun getFeatureAccessAsFlow(): Flow<FeatureAccessResponse>
}
