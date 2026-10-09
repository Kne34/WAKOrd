package edu.bluejack26_1.wakord.ui.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.discover.SearchEngine
import edu.bluejack26_1.wakord.core.error.messageRes
import edu.bluejack26_1.wakord.domain.model.Catalog
import edu.bluejack26_1.wakord.domain.model.ExploreFilter
import edu.bluejack26_1.wakord.domain.model.SearchResults
import edu.bluejack26_1.wakord.domain.repository.ExploreRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExploreViewModel(
    private val exploreRepository: ExploreRepository,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val searchRequests = MutableStateFlow(SearchRequest("", ExploreFilter()))
    private var catalog: Catalog? = null
    private var topLimit = PAGE_SIZE
    private var loadJob: Job? = null

    init {
        loadTrending()
        observeSearch()
    }

    fun onAction(action: ExploreAction) {
        when (action) {
            is ExploreAction.QueryChanged -> {
                _uiState.update { it.copy(query = action.query) }
                requestSearch()
            }
            ExploreAction.ClearQuery -> {
                _uiState.update { it.copy(query = "", results = SearchResults()) }
                requestSearch()
            }
            is ExploreAction.SelectGenre -> {
                _uiState.update { it.copy(selectedGenre = action.genre.takeIf { g -> g != it.selectedGenre }) }
                reload()
            }
            is ExploreAction.SelectLevel -> {
                _uiState.update { it.copy(selectedLevel = action.level.takeIf { l -> l != it.selectedLevel }) }
                reload()
            }
            is ExploreAction.SelectTab -> _uiState.update { it.copy(selectedTab = action.tab) }
            ExploreAction.LoadMore -> loadMore()
            ExploreAction.Refresh -> {
                catalog = null
                reload()
            }
            ExploreAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun filter() = _uiState.value.let { ExploreFilter(it.selectedGenre, it.selectedLevel) }

    private fun reload() {
        topLimit = PAGE_SIZE
        loadTrending()
        requestSearch()
    }

    private fun requestSearch() {
        searchRequests.value = SearchRequest(_uiState.value.query, filter())
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            searchRequests.debounce(SEARCH_DEBOUNCE_MS).collectLatest { request ->
                if (request.query.isBlank()) {
                    _uiState.update { it.copy(isSearching = false) }
                    return@collectLatest
                }
                _uiState.update { it.copy(isSearching = true) }
                val source = catalog ?: exploreRepository.catalog().getOrElse { e ->
                    _uiState.update { it.copy(isSearching = false, errorMessage = e.messageRes()) }
                    return@collectLatest
                }.also { catalog = it }
                val results = withContext(computeDispatcher) { SearchEngine.search(request.query, source, request.filter) }
                _uiState.update { it.copy(isSearching = false, results = results) }
            }
        }
    }

    private fun loadTrending() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, endReached = false) }
            val f = filter()
            val songs = async { exploreRepository.trendingSongs(fetchSizeFor(FETCH_LIMIT)) }
            val weekly = async { exploreRepository.topSongsThisWeek(fetchSizeFor(topLimit)) }
            val progressions = async { exploreRepository.trendingProgressions(fetchSizeFor(FETCH_LIMIT)) }
            val error = listOf(songs.await(), weekly.await(), progressions.await()).firstNotNullOfOrNull { it.exceptionOrNull() }
            val weeklySongs = weekly.await().getOrDefault(emptyList())
            _uiState.update { s ->
                s.copy(
                    isLoading = false,
                    trendingSongs = SearchEngine.applyFilter(songs.await().getOrDefault(emptyList()), f).take(FETCH_LIMIT),
                    topThisWeek = SearchEngine.applyFilter(weeklySongs, f).take(topLimit),
                    trendingProgressions = SearchEngine.applyFilter(progressions.await().getOrDefault(emptyList()), f).take(FETCH_LIMIT),
                    endReached = weeklySongs.size < fetchSizeFor(topLimit),
                    errorMessage = error?.messageRes() ?: s.errorMessage,
                )
            }
        }
    }

    private fun loadMore() {
        val s = _uiState.value
        if (s.isLoading || s.isLoadingMore || s.endReached) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextLimit = topLimit + PAGE_SIZE
            exploreRepository.topSongsThisWeek(fetchSizeFor(nextLimit))
                .onSuccess { list ->
                    topLimit = nextLimit
                    _uiState.update {
                        it.copy(
                            topThisWeek = SearchEngine.applyFilter(list, filter()).take(nextLimit),
                            endReached = list.size < fetchSizeFor(nextLimit),
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.messageRes()) } }
            _uiState.update { it.copy(isLoadingMore = false) }
        }
    }

    private fun fetchSizeFor(limit: Int): Int = if (filter() == ExploreFilter()) limit else limit * FILTER_OVERFETCH

    private data class SearchRequest(val query: String, val filter: ExploreFilter)

    companion object {
        const val PAGE_SIZE = 10
        const val SECTION_SIZE = 5
        const val FETCH_LIMIT = 20
        private const val FILTER_OVERFETCH = 3
        private const val SEARCH_DEBOUNCE_MS = 300L

        val Factory = viewModelFactory {
            initializer {
                ExploreViewModel((this[APPLICATION_KEY] as WAKordApp).container.exploreRepository)
            }
        }
    }
}
