package com.dutchman.resumeiq.domain.util

import com.dutchman.resumeiq.domain.models.Interviewer

class UserFactory(
    private val sharedPref: SharedPref
) {
    companion object {
        private const val PREF_IS_SKIP = "pref_is_skip"
        private const val PREF_MODEL_ID = "pref_model_id"
        private const val PREF_IS_MODEL_DOWNLOADED = "pref_is_model_downloaded"
        private const val PREF_LAST_QUESTION_INDEX = "pref_last_question_index"
        private const val PREF_INTERVIEWER_NAME = "pref_interviewer_name"
        private const val PREF_INTERVIEWER_DESIGNATION = "pref_interviewer_designation"
        private const val PREF_INTERVIEWER_MOBILE = "pref_interviewer_mobile"
    }

    val isSkip: Boolean
        get() = sharedPref.read(PREF_IS_SKIP, false)

    fun saveIsSkip(isSkip: Boolean) {
        sharedPref.write(PREF_IS_SKIP, isSkip)
    }

    val modelId: String
        get() = sharedPref.read(PREF_MODEL_ID, "")

    fun saveModelId(modelId: String) {
        sharedPref.write(PREF_MODEL_ID, modelId)
    }

    val isModelDownloaded: Boolean
        get() = sharedPref.read(PREF_IS_MODEL_DOWNLOADED, false)

    fun saveIsModelDownloaded(isDownloaded: Boolean) {
        sharedPref.write(PREF_IS_MODEL_DOWNLOADED, isDownloaded)
    }

    fun getDownloadId(fileName: String): Long {
        return sharedPref.read("download_id_$fileName", -1L)
    }

    fun saveDownloadId(fileName: String, downloadId: Long) {
        sharedPref.write("download_id_$fileName", downloadId)
    }

    fun removeDownloadId(fileName: String) {
        sharedPref.write("download_id_$fileName", -1L) // Assuming -1 means no download
    }

    val lastQuestionIndex: Int
        get() = sharedPref.read(PREF_LAST_QUESTION_INDEX, 0)

    fun saveLastQuestionIndex(index: Int) {
        sharedPref.write(PREF_LAST_QUESTION_INDEX, index)
    }

    val interviewer: Interviewer?
        get() {
            val name = sharedPref.read(PREF_INTERVIEWER_NAME, "")
            val designation = sharedPref.read(PREF_INTERVIEWER_DESIGNATION, "")
            val mobile = sharedPref.read(PREF_INTERVIEWER_MOBILE, "")
            return if (name.isNotEmpty()) Interviewer(name, designation, mobile) else null
        }

    fun saveInterviewer(interviewer: Interviewer) {
        sharedPref.write(PREF_INTERVIEWER_NAME, interviewer.name)
        sharedPref.write(PREF_INTERVIEWER_DESIGNATION, interviewer.designation)
        sharedPref.write(PREF_INTERVIEWER_MOBILE, interviewer.mobile)
    }
}