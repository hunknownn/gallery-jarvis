package com.hunknownn.galleryjarvis.labeling

/**
 * ImageNet-1000 클래스 인덱스를 한국어 카테고리로 매핑한다.
 *
 * MobileNetV3 classification 모델의 출력(1001 클래스, index 0 = background)에서
 * argmax 인덱스를 받아 한국어 라벨을 반환한다.
 * 신뢰도가 임계값 미만이면 null을 반환하여 불확실한 라벨을 방지한다.
 */
object LabelCategory {

    private const val CONFIDENCE_THRESHOLD = 0.15f

    /**
     * 분류 결과를 한국어 카테고리 라벨로 변환한다.
     *
     * @param classIndex ImageNet 클래스 인덱스 (0~1000, 0=background)
     * @param confidence softmax 확률값 (0.0~1.0)
     * @return 한국어 카테고리 라벨. 신뢰도 부족 또는 매핑 불가 시 null.
     */
    fun classify(classIndex: Int, confidence: Float): String? {
        if (confidence < CONFIDENCE_THRESHOLD) return null
        return categoryOf(classIndex)
    }

    /**
     * ImageNet 클래스 인덱스를 카테고리로 매핑한다.
     *
     * ImageNet-1000 주요 클래스 범위 (index 0 = background):
     * - 1~397: 동물 (어류, 조류, 파충류, 곤충, 개/고양이, 포유류)
     * - 924~969: 음식 (과일, 채소, 요리)
     * - 970~980: 풍경 (산, 바다, 계곡 등 자연 지형)
     * - 981~998: 식물 (꽃, 버섯, 나무)
     * - 그 외 (398~923): 사물/차량/건물이 혼재하여 개별 매핑
     */
    private fun categoryOf(index: Int): String? {
        return when (index) {
            0 -> null // background

            // 동물: 어류, 조류, 파충류, 양서류, 곤충, 갑각류, 개, 고양이, 곰, 포유류
            in 1..397 -> "동물"

            // 음식: 과일, 채소, 요리, 디저트, 음료
            in 924..969 -> "음식"

            // 풍경: 산, 절벽, 산호초, 간헐천, 호수, 해변, 계곡, 화산
            in 970..980 -> "풍경"

            // 식물: 꽃, 버섯, 나무, 과일(식물 관점)
            in 981..998 -> "식물"

            // 차량 (개별 인덱스)
            in vehicleIndices -> "차량"

            // 건물/구조물 (개별 인덱스)
            in buildingIndices -> "건물"

            // 그 외 사물: 매핑하지 않고 null 반환 (불확실한 라벨 방지)
            else -> null
        }
    }

    /** 차량 관련 ImageNet 클래스 인덱스 */
    private val vehicleIndices = setOf(
        407, // ambulance
        436, // beach wagon
        444, // bicycle-built-for-two
        468, // cab
        479, // car wheel
        511, // convertible
        555, // fire engine
        569, // garbage truck
        573, // go-kart
        586, // half-track
        609, // jeep
        627, // limousine
        654, // minibus
        656, // minivan
        665, // motor scooter
        670, // mountain bike
        675, // moving van
        705, // passenger car
        717, // pickup
        734, // police van
        751, // racer
        779, // school bus
        817, // sports car
        820, // steam locomotive
        864, // tow truck
        867, // trailer truck
        870, // trolleybus
    )

    /** 건물/구조물 관련 ImageNet 클래스 인덱스 */
    private val buildingIndices = setOf(
        483, // castle
        497, // church
        498, // cinema
        536, // dock
        663, // monastery
        668, // mosque
        698, // palace
        725, // planetarium
        730, // pier
        833, // stupa
    )
}
