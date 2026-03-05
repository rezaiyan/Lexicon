package presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base class for ViewModel tests. Sets [Dispatchers.Main] to an [UnconfinedTestDispatcher]
 * so that `viewModelScope.launch` executes eagerly in tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTestBase {

    @BeforeTest
    fun setUpDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDownDispatcher() {
        Dispatchers.resetMain()
    }
}
