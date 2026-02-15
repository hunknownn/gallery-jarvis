package com.hunknownn.galleryjarvis.naming

import com.hunknownn.galleryjarvis.model.Cluster

/**
 * 클러스터에 자동으로 폴더명을 부여하는 생성기.
 *
 * 라벨, 날짜 범위, 위치 정보를 조합하여 이름을 생성한다.
 * 예: "2023 제주 여행", "2022 강아지", 정보 없으면 "그룹"
 */
class NameGenerator {

    /**
     * 주어진 메타데이터를 조합하여 클러스터 이름을 생성한다.
     *
     * @param label 대표 이미지 라벨 (예: "음식", "풍경")
     * @param dateRange 날짜 범위 (예: "2023.06")
     * @param location 위치 정보 (예: "제주")
     * @return 조합된 이름. 모든 값이 null이면 "그룹" 반환.
     */
    fun generateName(
        label: String?,
        dateRange: String?,
        location: String?
    ): String {
        val parts = listOfNotNull(dateRange, location, label)
        return if (parts.isNotEmpty()) parts.joinToString(" ") else "그룹"
    }
}
