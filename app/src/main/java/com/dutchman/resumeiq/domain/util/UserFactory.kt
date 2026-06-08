package com.dutchman.resumeiq.domain.util

class UserFactory(
    private val sharedPref: SharedPref
) {
    companion object {
        private const val PREF_IS_SKIP = "pref_is_skip"
        private const val PREF_MODEL_ID = "pref_model_id"
        private const val PREF_IS_MODEL_DOWNLOADED = "pref_is_model_downloaded"
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
}