package fakes

import domain.featureflag.IFeatureFlagProvider

class FakeFeatureFlagProvider : IFeatureFlagProvider {
    private val booleans = mutableMapOf<String, Boolean>()
    private val strings = mutableMapOf<String, String>()
    private val longs = mutableMapOf<String, Long>()
    private val doubles = mutableMapOf<String, Double>()
    var fetchAndActivateCalled = false

    fun setBoolean(key: String, value: Boolean) { booleans[key] = value }

    override suspend fun fetchAndActivate() { fetchAndActivateCalled = true }
    override fun getBoolean(key: String, default: Boolean): Boolean = booleans[key] ?: default
    override fun getString(key: String, default: String): String = strings[key] ?: default
    override fun getLong(key: String, default: Long): Long = longs[key] ?: default
    override fun getDouble(key: String, default: Double): Double = doubles[key] ?: default
}
