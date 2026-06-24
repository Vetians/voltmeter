package org.ukrida.voltmeter.data.model

data class UploadResponse(
    val success: Boolean = false,
    val photo_path: String = "",
    val message: String = ""
)
