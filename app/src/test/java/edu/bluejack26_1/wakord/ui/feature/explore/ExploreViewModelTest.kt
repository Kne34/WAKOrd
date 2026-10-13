package edu.bluejack26_1.wakord.ui.feature.explore

import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.data.fake.FakeExploreRepository
import edu.bluejack26_1.wakord.data.fake.SampleDiscover
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val repository = FakeExploreRepository()

    private fun viewModel() = ExploreViewModel(repository, computeDispatcher = main.dispatcher)

    @Test
    fun `loads trending sections`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        val s = vm.uiState.value
        assertFalse(s.isLoading)
        assertEquals("subtle-resonance", s.trendingSongs.first().id)
        assertEquals(ExploreViewModel.FETCH_LIMIT, s.trendingProgressions.size)
        assertEquals("p25", s.trendingProgressions.first().id)
        assertEquals(2, s.topThisWeek.size)
        assertTrue(s.endReached)
    }

    @Test
    fun `load more grows top songs this week until the end`() = runTest(main.dispatcher) {
        repository.weeklyTop = (1..25).map { n -> SampleDiscover.songs.first().copy(id = "w$n") }
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(ExploreViewModel.PAGE_SIZE, vm.uiState.value.topThisWeek.size)
        assertFalse(vm.uiState.value.endReached)
        vm.onAction(ExploreAction.LoadMore)
        advanceUntilIdle()
        assertEquals(20, vm.uiState.value.topThisWeek.size)
        vm.onAction(ExploreAction.LoadMore)
        advanceUntilIdle()
        assertEquals(25, vm.uiState.value.topThisWeek.size)
        assertTrue(vm.uiState.value.endReached)
        assertEquals(ExploreViewModel.FETCH_LIMIT, vm.uiState.value.trendingProgressions.size)
    }

    @Test
    fun `search waits for typing to stop and loads the catalog once`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(ExploreAction.QueryChanged("mid"))
        advanceTimeBy(100)
        vm.onAction(ExploreAction.QueryChanged("midnight"))
        advanceUntilIdle()
        assertEquals(listOf("midnight-waves"), vm.uiState.value.results.songs.map { it.id })
        assertEquals(1, vm.uiState.value.songCount)
        vm.onAction(ExploreAction.QueryChanged("coffee"))
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.results.lyricMatches.size)
        assertEquals(1, repository.catalogLoads)
    }

    @Test
    fun `no match gives empty results for the empty state`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(ExploreAction.QueryChanged("zzzz"))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showResults)
        assertTrue(vm.uiState.value.results.isEmpty)
    }

    @Test
    fun `genre and level filter trending and search`() = runTest(main.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAction(ExploreAction.SelectGenre("Jazz"))
        vm.onAction(ExploreAction.SelectLevel(Difficulty.EASY))
        advanceUntilIdle()
        val s = vm.uiState.value
        assertTrue(s.trendingProgressions.isNotEmpty())
        assertTrue(s.trendingProgressions.all { it.genre == "Jazz" && it.difficulty == Difficulty.EASY })
        assertTrue(s.trendingSongs.isEmpty())

        vm.onAction(ExploreAction.SelectGenre("Jazz"))
        advanceUntilIdle()
        assertEquals(null, vm.uiState.value.selectedGenre)
    }

    @Test
    fun `offline shows a load error that refresh clears`() = runTest(main.dispatcher) {
        repository.offline = true
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(R.string.error_network, vm.uiState.value.loadError)
        assertTrue(vm.uiState.value.trendingSongs.isEmpty())

        repository.offline = false
        vm.onAction(ExploreAction.Refresh)
        advanceUntilIdle()
        assertEquals(null, vm.uiState.value.loadError)
        assertTrue(vm.uiState.value.trendingSongs.isNotEmpty())
    }
}
