# 온디바이스 AI 기반 갤러리 자동 분류 시스템 설계 문서

---

# 1. 개요 (Overview)

이 문서는 스마트폰 내부의 사진을 온디바이스(On-device) AI로 분석하여
자동으로 카테고리/폴더를 생성하고 정리하는 앱의 전체 기술 구조를 정의한다.

**Android와 iOS를 모두 지원**하며, KMM(Kotlin Multiplatform Mobile)을 활용하여
비즈니스 로직을 공유하고 플랫폼별 네이티브 API를 각각 구현한다.

**개발 전략**: Android를 우선 개발하여 완성한 후, iOS를 추가 개발한다.
Phase 1(Android)에서 공유 로직(commonMain)을 함께 완성하여
Phase 2(iOS)에서는 플랫폼 연결과 UI만 구현한다.

## 목적

- 갤러리에 있는 수천 장의 사진을 자동으로 정리
- 사용자 프라이버시 100% 보호
- AI를 활용한 동적 카테고리 생성
- 장소, 시간, 이미지 특징 등을 조합한 고품질 분류
- **Android 우선 개발 → iOS 순차 지원**

## 핵심 기능 요약

- 이미지 임베딩 추출 (Android: TFLite / iOS: Core ML)
- 점진적 클러스터링 (Incremental) — 공유 모듈
- 자동 폴더명 생성 (라벨링 + 메타데이터) — 공유 모듈
- 플랫폼별 사진 식별자 관리 (Android: MediaStore / iOS: PHAsset)
- SQLDelight + 임베딩 캐시 기반 로컬 저장소 설계 — 공유 모듈

---

# 2. 기술 스택 (Tech Stack)

## 2.1 크로스플랫폼 전략: KMM

| 구분 | 기술 | 비고 |
| --- | --- | --- |
| 공유 로직 | Kotlin Multiplatform (KMM) | 클러스터링, DB, 비즈니스 로직 |
| Android UI | Jetpack Compose | 네이티브 |
| iOS UI | SwiftUI | 네이티브 |
| 공유 DB | SQLDelight | KMM 호환 SQLite ORM |
| Android AI | TensorFlow Lite | NNAPI / GPU delegate |
| iOS AI | Core ML | Metal Performance Shaders |
| Android 백그라운드 | WorkManager | OS 보장 |
| iOS 백그라운드 | BGTaskScheduler | BGProcessingTask |
| 빌드 | Gradle (KMM Plugin) | Android + iOS 통합 빌드 |

## 2.2 KMM 모듈 분리 전략

```
┌──────────────────────────────────────────────────┐
│                   shared (commonMain)             │
│  ┌────────────┐ ┌────────────┐ ┌───────────────┐ │
│  │ Clustering │ │ SQLDelight │ │ Name Generator│ │
│  │ Engine     │ │ Database   │ │               │ │
│  └────────────┘ └────────────┘ └───────────────┘ │
│  ┌────────────────────────────────────────┐       │
│  │ expect declarations (플랫폼 인터페이스) │       │
│  │ - PhotoScanner                         │       │
│  │ - EmbeddingExtractor                   │       │
│  │ - BackgroundTaskScheduler              │       │
│  │ - FileStorage                          │       │
│  └────────────────────────────────────────┘       │
├──────────────────────────────────────────────────┤
│ androidMain (actual)  │ iosMain (actual)         │
│ - MediaStore Scanner  │ - PHAsset Scanner        │
│ - TFLite Extractor    │ - Core ML Extractor      │
│ - WorkManager         │ - BGTaskScheduler        │
│ - Android FileStorage │ - iOS FileStorage        │
├──────────────────────────────────────────────────┤
│ androidApp            │ iosApp                   │
│ - Jetpack Compose UI  │ - SwiftUI                │
└──────────────────────────────────────────────────┘
```

---

# 3. 전체 아키텍처 (Architecture)

## 3.1 시스템 구성 요소

```
 ┌──────────────────────────┐       ┌──────────────────────────┐
 │ Photo Scanner            │──────▶│ Embedding Extractor      │
 │ Android: MediaStore      │       │ Android: TFLite          │
 │ iOS: PHAsset (Photos.fw) │       │ iOS: Core ML             │
 └──────────────────────────┘       └──────────────────────────┘
                                           │
                                           ▼
                              ┌──────────────────────────┐
                              │ Embedding Cache (binary)  │
                              │ (플랫폼별 캐시 디렉토리)   │
                              └──────────────────────────┘
                                           │
                                           ▼
                              ┌──────────────────────────┐
                              │ Clustering Engine         │
                              │ (shared/commonMain)       │
                              └──────────────────────────┘
                                  │                 │
                                  ▼                 ▼
                  ┌────────────────────┐   ┌───────────────────────┐
                  │ SQLDelight DB      │   │ Auto Name Generator   │
                  │ (shared/commonMain)│   │ (shared/commonMain)   │
                  └────────────────────┘   └───────────────────────┘
                                  │
                                  ▼
                         ┌──────────────────┐
                         │     UI Layer      │
                         │ Android: Compose  │
                         │ iOS: SwiftUI      │
                         └──────────────────┘
```

---

## 3.2 전체 동작 흐름

1. 플랫폼별 Photo Scanner로 사진 ID/URI/메타데이터 스캔
   - Android: MediaStore ContentResolver
   - iOS: PHFetchResult<PHAsset>
2. 사진마다 embedding 추출
   - Android: TFLite Interpreter
   - iOS: Core ML VNCoreMLRequest
3. embedding 캐싱 (플랫폼별 캐시 디렉토리)
4. 기존 클러스터와 거리 비교 → incremental clustering (공유 로직)
5. 폴더명 생성 규칙 적용 (공유 로직)
6. SQLDelight에 사진/클러스터/매핑 저장 (공유 로직)
7. UI에서 분류된 폴더 목록 제공 (플랫폼별 네이티브 UI)

---

# 4. 데이터 모델 (Data Model)

## 4.1 저장해야 할 정보

### 사진 데이터

- platformId (Android: MediaStore ID / iOS: PHAsset localIdentifier)
- platformUri (Android: content:// URI / iOS: PHAsset localIdentifier)
- dateTaken
- latitude / longitude
- mimeType
- hash (optional)
- embeddingPath

### AI 데이터

- embedding vector (파일로 저장)
- clusterId
- label
- confidence

### 클러스터 데이터

- clusterId
- center vector
- 대표 이미지 ID
- 이름
- 생성/업데이트 시간

### 사용자 설정

- 숨김 카테고리
- 사용자 지정 폴더명
- 분류 제외 폴더 경로 등

---

## 4.2 SQLDelight 스키마 (shared/commonMain)

### photos.sq

```sql
CREATE TABLE photos (
    photo_id TEXT PRIMARY KEY,
    platform_uri TEXT NOT NULL,
    date_taken INTEGER,
    latitude REAL,
    longitude REAL,
    mime_type TEXT,
    hash TEXT,
    embedding_path TEXT
);

selectAll:
SELECT * FROM photos;

findById:
SELECT * FROM photos WHERE photo_id = ?;

insert:
INSERT OR REPLACE INTO photos VALUES (?, ?, ?, ?, ?, ?, ?, ?);

deleteById:
DELETE FROM photos WHERE photo_id = ?;
```

### clusters.sq

```sql
CREATE TABLE clusters (
    cluster_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    representative_photo_id TEXT,
    center_embedding_path TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

selectAll:
SELECT * FROM clusters;

insert:
INSERT INTO clusters (name, representative_photo_id, center_embedding_path, created_at, updated_at)
VALUES (?, ?, ?, ?, ?);

updateName:
UPDATE clusters SET name = ?, updated_at = ? WHERE cluster_id = ?;
```

### photo_cluster_map.sq

```sql
CREATE TABLE photo_cluster_map (
    photo_id TEXT NOT NULL,
    cluster_id INTEGER NOT NULL,
    PRIMARY KEY (photo_id, cluster_id)
);

findByPhotoId:
SELECT * FROM photo_cluster_map WHERE photo_id = ?;

findByClusterId:
SELECT * FROM photo_cluster_map WHERE cluster_id = ?;

insert:
INSERT OR REPLACE INTO photo_cluster_map VALUES (?, ?);
```

> **변경 사항**: photo_id를 INTEGER에서 TEXT로 변경.
> Android의 MediaStore ID(Long)와 iOS의 PHAsset localIdentifier(String) 모두 수용하기 위함.

---

## 4.3 임베딩 캐시 구조

### Android

```
/data/data/<app-id>/cache/embeddings/
    ├── photo_123.bin
    ├── photo_124.bin
    └── ...
```

### iOS

```
<App Sandbox>/Library/Caches/embeddings/
    ├── photo_abc123.bin
    ├── photo_def456.bin
    └── ...
```

### 공유 인터페이스 (expect/actual)

```kotlin
// shared/commonMain
expect class FileStorage {
    fun getEmbeddingCacheDir(): String
    fun saveFile(path: String, data: ByteArray)
    fun loadFile(path: String): ByteArray?
    fun deleteFile(path: String)
}
```

---

# 5. AI 모델 (Embedding & Classification)

## 5.1 임베딩 모델 선정 사유

- 모바일 친화성
- 1~5MB 수준으로 최적
- cosine distance 기반 클러스터링에 적합
- **Android/iOS 모두 변환 가능한 모델 우선 선정**

## 5.2 모델 크기 / 성능

| 모델 | 크기 | 추론 속도 | Android | iOS | 설명 |
| --- | --- | --- | --- | --- | --- |
| MobileNetV3 | 3~5MB | 빠름 | TFLite | Core ML | 온디바이스 최적 |
| EfficientNet-lite | 5~10MB | 중간 | TFLite | Core ML | 품질 좋음 |
| CLIP Mobile | 8~15MB | 중간 | TFLite | Core ML | 의미 기반 분류 강함 |

> **모델 변환 파이프라인**: PyTorch/TF 원본 → TFLite(Android용) + Core ML(iOS용) 동시 변환

---

## 5.3 플랫폼별 AI 최적화

### Android (TFLite)

- quantization (int8/full-int)
- GPU delegate (optional)
- NNAPI delegate 사용 가능

### iOS (Core ML)

- Core ML 모델 최적화 (FP16 / INT8)
- Metal Performance Shaders 가속
- Neural Engine 활용 (A11 Bionic 이상)

---

## 5.4 임베딩 추출 인터페이스 (expect/actual)

```kotlin
// shared/commonMain
expect class EmbeddingExtractor {
    fun extractEmbedding(imageData: ByteArray): FloatArray
    fun close()
}
```

```kotlin
// shared/androidMain
actual class EmbeddingExtractor {
    private val interpreter = Interpreter(loadModelFile("model.tflite"))

    actual fun extractEmbedding(imageData: ByteArray): FloatArray {
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        val input = preprocessBitmap(bitmap)
        val output = Array(1) { FloatArray(EMBEDDING_DIM) }
        interpreter.run(input, output)
        return output[0]
    }

    actual fun close() { interpreter.close() }
}
```

```swift
// iosApp (Swift)
class EmbeddingExtractorIOS {
    private let model: VNCoreMLModel

    init() {
        let mlModel = try! EmbeddingModel(configuration: .init()).model
        self.model = try! VNCoreMLModel(for: mlModel)
    }

    func extractEmbedding(imageData: Data) -> [Float] {
        let request = VNCoreMLRequest(model: model)
        let handler = VNImageRequestHandler(data: imageData)
        try! handler.perform([request])
        guard let result = request.results?.first as? VNCoreMLFeatureValueObservation,
              let multiArray = result.featureValue.multiArrayValue else { return [] }
        return (0..<multiArray.count).map { Float(truncating: multiArray[$0]) }
    }
}
```

---

## 5.5 대표 이미지 라벨링 모델

- lightweight classifier
- CLIP text similarity 기반 폴더명 후보 생성
- 라벨 예시: 사람 / 음식 / 반려동물 / 풍경 / 도시 / 문서

---

# 6. 클러스터링 (Clustering) — 공유 모듈

> 클러스터링 로직은 전체가 shared/commonMain에 위치하며, 플랫폼 의존성 없음.

## 6.1 알고리즘 비교

| 알고리즘 | 장점 | 단점 | 모바일 적합 |
| --- | --- | --- | --- |
| K-means | 빠름 | k 필요 | ○ |
| MiniBatch K-means | 고속 | k 필요 | ◎ |
| DBSCAN | k 불필요 | 느림 | △ |
| HDBSCAN | 최고 품질 | 매우 느림 | ✕ |

---

## 6.2 점진적(incremental) 클러스터링

전체 클러스터링을 매번 재계산하지 않고,
**새 사진만 기존 cluster에 할당**하는 방식.

### 흐름

1. 새 임베딩 생성
2. cluster center 목록 조회
3. 거리 비교
4. threshold 이하면 기존 cluster에 추가
5. 아니면 새 cluster 생성
6. center 업데이트

---

## 6.3 center 업데이트 공식

```
new_center = old_center * (n/(n+1)) + embedding * (1/(n+1))
```

---

## 6.4 대표 사진 선택 규칙

- center와 가장 가까운 embedding
- 가장 선명한 이미지
- 사용자가 대표 선택 가능

---

# 7. 폴더명 자동 생성 — 공유 모듈

## 7.1 대표 이미지 기반 라벨링

- CLIP Mobile 또는 TinyClassifier
- 라벨 후보: 사람, 음식, 반려동물, 풍경, 도시, 문서

## 7.2 메타데이터 조합

- 날짜 범위
- 위치(GPS)
- 라벨 + 날짜 조합
    - 예: `2023 제주 여행`, `2022 강아지`

## 7.3 네이밍 규칙

1. 대표 라벨 우선
2. 날짜 정보 포함
3. 위치 정보 포함
4. fallback: "그룹 1/2/3…"

---

# 8. 식별자 관리

## 8.1 플랫폼별 사진 식별자

| 구분 | Android | iOS |
| --- | --- | --- |
| 식별자 | MediaStore ID (Long) | PHAsset localIdentifier (String) |
| URI/참조 | content:// URI | PHAsset localIdentifier |
| 안정성 | 사진 이동해도 ID 유지 | identifier 영구 유지 |
| 삭제 추적 | ContentObserver | PHPhotoLibraryChangeObserver |
| 권한 모델 | READ_MEDIA_IMAGES | PHPhotoLibrary.requestAuthorization |

## 8.2 공유 식별자 인터페이스

```kotlin
// shared/commonMain
data class PhotoIdentifier(
    val id: String,        // Android: MediaStore ID를 String 변환 / iOS: localIdentifier
    val platformUri: String
)

expect class PhotoScanner {
    suspend fun scanAllPhotos(): List<PhotoIdentifier>
    suspend fun getPhotoMetadata(id: String): PhotoMetadata?
    fun observeChanges(callback: (List<String>) -> Unit)
}
```

---

## 8.3 삭제 감지

### Android

- MediaStore `IS_PENDING`, `DATE_MODIFIED` 기반 추적
- ContentObserver로 변경 감지

### iOS

- PHPhotoLibraryChangeObserver 프로토콜 구현
- PHChange 기반 삭제/추가/수정 감지

### 공통

- DB에서 orphan 제거 (공유 로직)

## 8.4 해시 기반 보조

- SHA-1 등으로 중복 감지
- 필요시 사용

---

# 9. 백그라운드 처리

## 9.1 플랫폼별 백그라운드 전략

| 구분 | Android | iOS |
| --- | --- | --- |
| API | WorkManager | BGTaskScheduler |
| 장시간 작업 | CoroutineWorker | BGProcessingTask |
| 조건부 실행 | Constraints (충전, 네트워크 등) | earliestBeginDate 설정 |
| OS 보장 | 높음 | 제한적 (약 30초~수분) |
| 반복 스케줄 | PeriodicWorkRequest | BGAppRefreshTask |

### expect/actual 인터페이스

```kotlin
// shared/commonMain
expect class BackgroundTaskScheduler {
    fun scheduleEmbeddingExtraction()
    fun scheduleBatchClustering()
    fun cancelAll()
}
```

## 9.2 연산 부담 관리

### Android

- 배터리 15% 이하 중지 (BatteryManager)
- 발열 감지 시 pause (PowerManager.THERMAL_STATUS)
- Night-time batch 처리 가능

### iOS

- 배터리 잔량 확인 (UIDevice.current.batteryLevel)
- 발열 감지 시 pause (ProcessInfo.thermalState)
- BGProcessingTask는 주로 야간/충전 중 실행
- **iOS 백그라운드 시간 제한 고려**: 대량 처리 시 여러 세션으로 분할

---

## 9.3 신규 사진 처리 흐름

1. 사진 추가 이벤트 감지
   - Android: ContentObserver
   - iOS: PHPhotoLibraryChangeObserver
2. 임베딩 즉시 생성 (foreground) 또는 백그라운드 큐 등록
3. 기존 cluster에 할당 (공유 로직)
4. UI 업데이트

---

# 10. 성능 최적화

## 10.1 임베딩 캐싱

- 최초 1회 생성 후 파일로 저장
- 로딩 비용 절감

## 10.2 점진적 클러스터링

- 전체 재계산 제거
- 수천~1만 장까지 처리 가능

## 10.3 UI 최적화

- 썸네일 lazy load
  - Android: Coil/Glide
  - iOS: PHCachingImageManager
- pagination

## 10.4 대량 처리 전략

- 초기: MiniBatch K-means
- 이후: incremental 방식
- **iOS 메모리 제한 고려**: 배치 크기를 플랫폼별로 조정 (iOS는 더 작은 배치)

---

# 11. 보안 및 프라이버시

## 11.1 공통

- 모든 작업은 온디바이스
- 외부 네트워크 요청 없음
- 파일 경로도 private directory 사용
- 앱 삭제 시 전체 데이터 삭제

## 11.2 Android

- `READ_MEDIA_IMAGES` 권한 (Android 13+)
- `READ_EXTERNAL_STORAGE` (Android 12 이하)
- Scoped Storage 정책 준수

## 11.3 iOS

- `NSPhotoLibraryUsageDescription` (Info.plist 필수)
- PHPhotoLibrary.requestAuthorization(.readWrite)
- 사용자에게 사진 접근 목적 명확히 안내
- App Privacy Nutrition Label 작성 필요 (App Store 심사)

---

# 12. 코드 구조

```
project-root/
├── shared/                          # KMM 공유 모듈
│   └── src/
│       ├── commonMain/kotlin/
│       │   ├── clustering/          # 클러스터링 엔진
│       │   │   ├── ClusteringEngine.kt
│       │   │   └── IncrementalClustering.kt
│       │   ├── database/            # SQLDelight
│       │   │   ├── AppDatabase.kt
│       │   │   └── *.sq
│       │   ├── model/               # 공유 데이터 모델
│       │   │   ├── PhotoIdentifier.kt
│       │   │   ├── PhotoMetadata.kt
│       │   │   └── Cluster.kt
│       │   ├── naming/              # 폴더명 생성
│       │   │   └── NameGenerator.kt
│       │   └── platform/            # expect 선언
│       │       ├── PhotoScanner.kt
│       │       ├── EmbeddingExtractor.kt
│       │       ├── BackgroundTaskScheduler.kt
│       │       └── FileStorage.kt
│       ├── androidMain/kotlin/      # Android actual 구현
│       │   └── platform/
│       │       ├── AndroidPhotoScanner.kt
│       │       ├── TFLiteEmbeddingExtractor.kt
│       │       ├── WorkManagerScheduler.kt
│       │       └── AndroidFileStorage.kt
│       └── iosMain/kotlin/          # iOS actual 구현
│           └── platform/
│               ├── IOSPhotoScanner.kt
│               ├── CoreMLEmbeddingExtractor.kt
│               ├── BGTaskSchedulerImpl.kt
│               └── IOSFileStorage.kt
├── androidApp/                      # Android 앱
│   └── src/main/
│       ├── kotlin/ui/               # Jetpack Compose UI
│       └── res/
├── iosApp/                          # iOS 앱
│   └── Sources/
│       ├── Views/                   # SwiftUI
│       ├── Bridge/                  # KMM ↔ Swift 브릿지
│       └── Info.plist
├── build.gradle.kts                 # KMM 프로젝트 설정
└── settings.gradle.kts
```

---

# 13. 개발 로드맵 (Development Roadmap)

## Phase 1: Android 개발 (우선)

Android 앱을 완성하면서 공유 모듈(commonMain)도 함께 완성한다.

### Phase 1에서 개발하는 범위

| 모듈 | 내용 | 위치 |
| --- | --- | --- |
| 클러스터링 엔진 | IncrementalClustering, cosineDistance 등 | shared/commonMain |
| DB 스키마 & 쿼리 | SQLDelight .sq 파일, AppDatabase | shared/commonMain |
| 폴더명 생성 | NameGenerator | shared/commonMain |
| 데이터 모델 | PhotoIdentifier, PhotoMetadata, Cluster | shared/commonMain |
| expect 선언 | PhotoScanner, EmbeddingExtractor, FileStorage 등 | shared/commonMain |
| Android actual | MediaStore, TFLite, WorkManager, FileStorage | shared/androidMain |
| Android UI | Jetpack Compose 갤러리 뷰어 | androidApp |

### Phase 1 완료 기준

- [ ] Android 앱이 Google Play 출시 가능 수준
- [ ] commonMain의 공유 로직이 플랫폼 독립적으로 완성
- [ ] expect 인터페이스가 확정되어 iOS actual만 구현하면 되는 상태

### Phase 1 주의사항: commonMain 순수성 유지

Phase 2에서 iOS를 원활히 붙이려면, commonMain에 Android 전용 코드가 섞이지 않아야 한다.

```kotlin
// commonMain에서 금지
import android.graphics.Bitmap      // Android 전용
import android.content.Context      // Android 전용
import java.io.File                 // JVM 전용 (okio 등 멀티플랫폼 라이브러리 사용)

// commonMain에서 허용
import kotlin.math.sqrt             // Kotlin 표준 라이브러리
import kotlinx.coroutines.*         // 멀티플랫폼 지원
```

**체크리스트**:
- [ ] commonMain에 `import android.*` 또는 `import java.*`가 없는지 확인
- [ ] 파일 I/O는 expect/actual 또는 okio 멀티플랫폼으로 처리
- [ ] 이미지 데이터는 `ByteArray`로 주고받기 (Bitmap 등 플랫폼 타입 사용 금지)

---

## Phase 1.5: Android 출시 준비

Phase 1에서 핵심 기능 구조가 완성된 후, Google Play 출시를 위한 품질/정책 요구사항을 충족한다.

### 1.5.1 기능 완성

| 작업 | 설명 | 우선순위 |
| --- | --- | --- |
| 초기 처리 전략 개선 | 전체 사진 일괄 처리 → 배치 분할 + 백그라운드 처리로 변경. 앱 진입 시 자동 시작, 포그라운드에서는 처리된 결과부터 표시 | 높음 |
| NameGenerator 연동 | 대표 이미지 라벨 + 날짜/위치 메타데이터 조합으로 클러스터 자동 이름 생성. 라벨링 모델(CLIP Mobile 등) 추가 검토 | 높음 |
| 사진 변경 감지 | ContentObserver 연동으로 새 사진 추가/삭제 시 자동 반영, orphan 레코드 정리 | 중간 |
| 클러스터 이름 편집 | 사용자가 클러스터 이름을 직접 수정할 수 있는 UI | 중간 |
| 대량 처리 안정성 | 배치 크기 제한 (50~100장 단위), Bitmap recycle 보장, 처리 중 앱 종료 시 이어가기 | 높음 |

### 1.5.2 에러 핸들링

| 작업 | 설명 |
| --- | --- |
| 모델 로드 실패 | TFLite 모델 파일 누락/손상 시 fallback 안내 |
| 권한 거부 처리 | 영구 거부 시 설정 화면 이동 안내, 부분 권한(선택된 사진만) 대응 |
| 메모리 부족 | 대량 처리 중 OOM 방지, 배치 간 GC 유도 |
| 네트워크 불필요 명시 | INTERNET 권한 미사용 확인 (온디바이스 앱 신뢰도) |

### 1.5.3 UI/UX 개선

| 작업 | 설명 |
| --- | --- |
| 온보딩 화면 | 첫 실행 시 앱 사용법 1~3페이지 안내 |
| 처리 진행률 | 프로그레스 바 + 처리된 사진 수 / 전체 수 표시 |
| 빈 상태 화면 | 클러스터 없을 때, 사진 없을 때 각각 안내 문구 |
| Pull-to-refresh | 클러스터 목록에서 당겨서 새로고침 |
| 스플래시 화면 | Android 12+ SplashScreen API 적용 |
| 다크모드 | Material3 동적 색상 (Dynamic Color) 지원 |

### 1.5.4 출시 필수 요구사항

| 작업 | 설명 | 필수 여부 |
| --- | --- | --- |
| 앱 아이콘 | Adaptive Icon (foreground + background 레이어) | 필수 |
| 서명 키 생성 | release용 keystore 생성 및 signing config 설정 | 필수 |
| ProGuard/R8 | 코드 난독화 및 shrink 설정, TFLite 관련 keep rule 추가 | 필수 |
| release 빌드 | `assembleRelease` 빌드 성공 및 APK/AAB 생성 확인 | 필수 |
| 개인정보처리방침 | 사진 접근 권한 사용 앱은 개인정보처리방침 페이지 URL 필수 (Google Play 정책) | 필수 |
| 스토어 등록 정보 | 앱 이름, 설명, 스크린샷 (최소 2장), 카테고리, 콘텐츠 등급 설문 | 필수 |
| targetSdk 정책 | Google Play 최신 targetSdk 요구사항 충족 확인 (현재 36 → OK) | 필수 |
| 64비트 지원 | AAB에 arm64-v8a 포함 확인 (TFLite native lib) | 필수 |

### 1.5.5 권장 (출시 후 안정성)

| 작업 | 설명 |
| --- | --- |
| Crashlytics 연동 | Firebase Crashlytics로 런타임 크래시 수집 |
| ANR 방지 검증 | 메인 스레드 블로킹 없는지 StrictMode 검증 |
| 실기기 테스트 | 저사양 ~ 고사양 기기 3종 이상 테스트 |
| 배터리 소모 프로파일링 | 대량 처리 시 배터리 소모량 측정 |
| 접근성 | TalkBack 대응, contentDescription 설정 |

### Phase 1.5 완료 기준

- [ ] release APK/AAB 빌드 성공
- [ ] 실기기 3종 이상에서 E2E 동작 확인 (권한 → 스캔 → 분류 → 표시)
- [ ] 1000장 이상 사진에서 OOM 없이 처리 완료
- [ ] 개인정보처리방침 페이지 URL 준비
- [ ] Google Play Console에 앱 등록 가능한 모든 에셋 준비

---

## Phase 2: iOS 개발 (후속)

Phase 1에서 완성된 공유 모듈을 그대로 사용하고, iOS 플랫폼 레이어와 UI만 구현한다.

### Phase 2에서 개발하는 범위

| 모듈 | 내용 | 위치 |
| --- | --- | --- |
| iOS actual | PHAsset Scanner, Core ML Extractor, BGTaskScheduler, FileStorage | shared/iosMain |
| iOS UI | SwiftUI 갤러리 뷰어 | iosApp |
| KMM 브릿지 | shared 모듈 ↔ Swift 연결 코드 | iosApp/Bridge |
| Core ML 모델 | TFLite 모델을 Core ML로 변환 | 모델 파이프라인 |
| iOS 권한 처리 | NSPhotoLibraryUsageDescription, Privacy Label | iosApp/Info.plist |

### Phase 2 완료 기준

- [ ] iOS 앱이 App Store 출시 가능 수준
- [ ] Android와 동일한 분류 품질 (같은 공유 로직 사용)
- [ ] iOS 고유 기능 대응 (백그라운드 시간 제한, 메모리 제한 등)

### Phase 2 예상 작업 비율

```
공유 로직 수정    ░░░░░░░░░░░░░░░░░░░░░  거의 없음 (0~5%)
iOS actual 구현   ████████████░░░░░░░░░  약 40%
SwiftUI UI        ████████████░░░░░░░░░  약 40%
Core ML 모델 변환  ████░░░░░░░░░░░░░░░░░  약 10%
테스트 & QA       ██░░░░░░░░░░░░░░░░░░░  약 10%
```

---

# 14. 향후 확장성 (Phase 3 이후)

- 얼굴 그룹화
- OCR 문서 자동 태그
- 중복 사진 정리
- Cloud + On-device hybrid
- **Compose Multiplatform UI 통합** (UI 레이어도 공유)
- **Wear OS / watchOS 연동**

---

# 15. Appendix – 코드 스니펫

## 임베딩 저장/로드 (shared/commonMain)

```kotlin
// 순수 Kotlin으로 작성하여 양 플랫폼에서 공유
object EmbeddingSerializer {
    fun serialize(embedding: FloatArray): ByteArray {
        val buffer = ByteArray(embedding.size * 4)
        for (i in embedding.indices) {
            val bits = embedding[i].toBits()
            buffer[i * 4] = (bits and 0xFF).toByte()
            buffer[i * 4 + 1] = ((bits shr 8) and 0xFF).toByte()
            buffer[i * 4 + 2] = ((bits shr 16) and 0xFF).toByte()
            buffer[i * 4 + 3] = ((bits shr 24) and 0xFF).toByte()
        }
        return buffer
    }

    fun deserialize(bytes: ByteArray): FloatArray {
        val result = FloatArray(bytes.size / 4)
        for (i in result.indices) {
            val bits = (bytes[i * 4].toInt() and 0xFF) or
                       ((bytes[i * 4 + 1].toInt() and 0xFF) shl 8) or
                       ((bytes[i * 4 + 2].toInt() and 0xFF) shl 16) or
                       ((bytes[i * 4 + 3].toInt() and 0xFF) shl 24)
            result[i] = Float.fromBits(bits)
        }
        return result
    }
}
```

## 점진적 클러스터링 핵심 로직 (shared/commonMain)

```kotlin
class IncrementalClustering(
    private val db: AppDatabase,
    private val fileStorage: FileStorage,
    private val threshold: Float = 0.5f
) {
    suspend fun assignPhoto(photoId: String, embedding: FloatArray) {
        val clusters = db.clusterQueries.selectAll().executeAsList()
        val nearest = clusters.minByOrNull { cluster ->
            val center = loadCenter(cluster.center_embedding_path)
            cosineDistance(embedding, center)
        }

        if (nearest != null && cosineDistance(embedding, loadCenter(nearest.center_embedding_path)) < threshold) {
            db.photoClusterMapQueries.insert(photoId, nearest.cluster_id)
            updateCenter(nearest, embedding)
        } else {
            createNewCluster(photoId, embedding)
        }
    }

    private fun cosineDistance(a: FloatArray, b: FloatArray): Float {
        var dot = 0f; var normA = 0f; var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return 1f - (dot / (sqrt(normA) * sqrt(normB)))
    }
}
```

## iOS 사진 스캔 (Swift)

```swift
import Photos

class PhotoScannerIOS {
    func scanAllPhotos() -> [PhotoIdentifier] {
        let fetchOptions = PHFetchOptions()
        fetchOptions.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]

        let assets = PHAsset.fetchAssets(with: .image, options: fetchOptions)
        var results: [PhotoIdentifier] = []

        assets.enumerateObjects { asset, _, _ in
            results.append(PhotoIdentifier(
                id: asset.localIdentifier,
                platformUri: asset.localIdentifier
            ))
        }
        return results
    }

    func observeChanges(callback: @escaping ([String]) -> Void) {
        PHPhotoLibrary.shared().register(self) // PHPhotoLibraryChangeObserver
    }
}
```

---