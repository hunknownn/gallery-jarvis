package com.hunknownn.galleryjarvis.naming

import com.hunknownn.galleryjarvis.model.Cluster

class NameGenerator {

    fun generateName(
        label: String?,
        dateRange: String?,
        location: String?
    ): String {
        val parts = listOfNotNull(dateRange, location, label)
        return if (parts.isNotEmpty()) parts.joinToString(" ") else "그룹"
    }
}
