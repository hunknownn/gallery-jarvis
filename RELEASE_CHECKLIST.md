# Gallery Jarvis v1.0.0 출시 순서

## 1. Release APK 빌드

```bash
./gradlew clean :androidApp:assembleRelease
```

## 2. 실기기 테스트

- [ ] APK 설치 후 앱 정상 실행
- [ ] 자동 분류 ON → 사진 스캔/클러스터링 동작 확인
- [ ] 같은 장소/시간대 사진이 같은 그룹으로 묶이는지 확인
- [ ] GPS/날짜 없는 사진도 오류 없이 처리되는지 확인

## 3. Git 커밋 & 태그

```bash
git add -A
git commit -m "feat(clustering): L2 정규화 + 복합 거리 기반 클러스터링 품질 개선"
git tag v1.0.0
```

## 4. Play Store 배포

```bash
./gradlew :androidApp:bundleRelease
```

1. Google Play Console → 프로덕션 → 새 릴리스
2. `androidApp/build/outputs/bundle/release/androidApp-release.aab` 업로드
3. 출시 노트 작성 후 검토 제출
