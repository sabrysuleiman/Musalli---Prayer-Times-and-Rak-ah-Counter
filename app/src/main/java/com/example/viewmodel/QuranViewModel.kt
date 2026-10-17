package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.PageContent
import com.example.data.QuranPreferences
import com.example.data.QuranTextProvider
import com.example.model.QuranMetadata
import com.example.model.QuranWardProgress
import com.example.model.SurahInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = QuranPreferences(application)

    private val _progress = MutableStateFlow(preferences.getProgress())
    val progress: StateFlow<QuranWardProgress> = _progress.asStateFlow()

    private val _currentPageContent = MutableStateFlow(QuranTextProvider.getPage(preferences.currentPage))
    val currentPageContent: StateFlow<PageContent> = _currentPageContent.asStateFlow()

    private val _readerFontSize = MutableStateFlow(22f)
    val readerFontSize: StateFlow<Float> = _readerFontSize.asStateFlow()

    fun nextPage() {
        val next = (_progress.value.currentPage + 1).coerceAtMost(604)
        setPage(next)
    }

    fun previousPage() {
        val prev = (_progress.value.currentPage - 1).coerceAtLeast(1)
        setPage(prev)
    }

    fun setPage(pageNumber: Int) {
        val clamped = pageNumber.coerceIn(1, 604)
        val delta = (clamped - preferences.currentPage).coerceAtLeast(0)

        preferences.currentPage = clamped
        if (delta > 0) {
            preferences.pagesReadToday = preferences.pagesReadToday + delta
        }

        updateState()
    }

    fun saveBookmark() {
        val curPage = _progress.value.currentPage
        val surah = QuranMetadata.getSurahForPage(curPage)
        preferences.bookmarkPage = curPage
        preferences.bookmarkSurahName = surah.nameArabic
        updateState()
    }

    fun goToBookmark() {
        setPage(preferences.bookmarkPage)
    }

    fun setDailyGoal(pages: Int) {
        preferences.dailyGoalPages = pages
        updateState()
    }

    fun markTodayWardCompleted() {
        preferences.markTodayCompleted()
        updateState()
    }

    fun adjustFontSize(delta: Float) {
        val newSize = (_readerFontSize.value + delta).coerceIn(16f, 36f)
        _readerFontSize.value = newSize
    }

    fun resetKhatmah() {
        preferences.currentPage = 1
        preferences.pagesReadToday = 0
        updateState()
    }

    private fun updateState() {
        _progress.value = preferences.getProgress()
        _currentPageContent.value = QuranTextProvider.getPage(preferences.currentPage)
    }
}
