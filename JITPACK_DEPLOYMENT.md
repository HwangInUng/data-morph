# JitPack을 통한 DataMorph 라이브러리 배포 가이드

## 🚀 JitPack이란?

JitPack은 GitHub, GitLab, Bitbucket 저장소를 자동으로 Maven/Gradle 의존성으로 변환해주는 서비스입니다.
- **장점**: 별도 계정 생성 불필요, 자동 빌드, 즉시 배포
- **단점**: 공식 Maven Central보다 신뢰성이 낮을 수 있음

## 📋 배포 단계

### 1. GitHub 저장소 준비
- [x] 코드를 GitHub에 푸시
- [x] `build.gradle.kts`에 JitPack 설정 완료
- [ ] Release 태그 생성

### 2. Release 태그 생성
```bash
# 현재 변경사항 커밋
git add .
git commit -m "feat: JitPack 배포 준비 완료"

# 태그 생성 및 푸시
git tag v1.0.0
git push origin v1.0.0

# 또는 GitHub 웹에서 Release 생성
```

### 3. JitPack 빌드 확인
1. https://jitpack.io 방문
2. `HwangInUng/data-morph` 입력
3. `v1.0.0` 태그 선택
4. "Get It" 버튼 클릭하여 빌드 시작

## 📦 사용자 의존성 설정

### Gradle (Kotlin DSL)
```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.HwangInUng:data-morph:1.0.0")
}
```

### Gradle (Groovy)
```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.HwangInUng:data-morph:1.0.0'
}
```

### Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.HwangInUng</groupId>
        <artifactId>data-morph</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

## 🔄 버전 관리

### 1. 새 버전 배포
```bash
# 새 버전 태그 생성
git tag v1.1.0
git push origin v1.1.0
```

### 2. 지원되는 버전 형식
- **특정 태그**: `v1.0.0`, `1.0.0`
- **특정 커밋**: `a1b2c3d` (커밋 해시)
- **브랜치**: `main-SNAPSHOT`, `develop-SNAPSHOT`

### 3. 브랜치 기반 개발 버전
```kotlin
// 개발 중인 main 브랜치 사용
implementation("com.github.HwangInUng:data-morph:main-SNAPSHOT")
```

## 🎯 JitPack vs Maven Central 비교

| 기능 | JitPack | Maven Central |
|------|---------|---------------|
| 설정 복잡도 | ⭐⭐ (간단) | ⭐⭐⭐⭐⭐ (복잡) |
| 배포 속도 | ⚡ 즉시 | 🐌 수시간~수일 |
| 계정 필요 | ❌ 불필요 | ✅ 필요 |
| 신뢰성 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 검색 가능성 | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| 기업 사용 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

## 🛠️ JitPack 최적화 팁

### 1. 빌드 시간 단축
```kotlin
// jitpack.yml 파일 생성 (프로젝트 루트)
jdk:
  - openjdk17
before_install:
  - sdk install java 17.0.1-open
  - sdk use java 17.0.1-open
```

### 2. README에 배지 추가
```markdown
[![](https://jitpack.io/v/HwangInUng/data-morph.svg)](https://jitpack.io/#HwangInUng/data-morph)
```

### 3. 빌드 상태 확인
- JitPack 웹사이트에서 빌드 로그 확인
- 빌드 실패 시 상세 로그 제공

## 🐛 트러블슈팅

### 1. 빌드 실패
- JitPack 로그 확인: https://jitpack.io/builds/HwangInUng/data-morph
- Java 버전 호환성 확인
- 의존성 문제 해결

### 2. 캐시 문제
```bash
# Gradle 캐시 정리
./gradlew clean --refresh-dependencies

# 또는 JitPack에서 "Look up" 재실행
```

### 3. 태그 인식 문제
- 태그가 올바르게 푸시되었는지 확인
- GitHub에서 Release 생성 시 자동 태그 생성 확인

## 📈 배포 후 확인사항

1. **JitPack 빌드 성공 확인**
   - https://jitpack.io/#HwangInUng/data-morph

2. **의존성 다운로드 테스트**
   ```bash
   # 새 프로젝트에서 테스트
   ./gradlew dependencies --configuration compileClasspath
   ```

3. **README 업데이트**
   - 설치 방법 추가
   - JitPack 배지 추가

## 🎉 완료!

이제 사용자들이 다음과 같이 라이브러리를 사용할 수 있습니다:

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.HwangInUng:data-morph:1.0.0")
}
```

```java
// Java 코드에서 사용
import com.datamorph.core.DataMorph;

DataSource data = DataMorph.from("data.csv");
```

JitPack을 사용하면 복잡한 Maven Central 설정 없이도 라이브러리를 쉽게 배포하고 공유할 수 있습니다!
