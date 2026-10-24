plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

// 워치 앱은 휴대폰 앱과 별도로 빌드되지만, Wearable Data Layer 는
// **두 앱의 applicationId 가 같아야** 동작합니다. 그래서 여기서도
// com.chargesound.app 을 씁니다. namespace 만 달리해서 R 클래스가
// 휴대폰 모듈과 섞이지 않게 합니다.
val wearVersionCode =
    ((project.findProperty("WEAR_VERSION_CODE") as String?)
        ?: (System.getenv("WEAR_VERSION_CODE") ?: "")).toIntOrNull() ?: 1

val wearVersionName =
    (project.findProperty("WEAR_VERSION_NAME") as String?)
        ?: (System.getenv("WEAR_VERSION_NAME") ?: "1.0.0")

android {
  namespace = "com.chargesound.wear"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.chargesound.app"
    // Wear OS 3 (Galaxy Watch4 이후) 가 API 30 입니다. 그 아래는 Compose 가 사실상 불가능합니다.
    minSdk = 30
    targetSdk = 36
    versionCode = wearVersionCode
    versionName = wearVersionName
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // 워치 저장공간이 작고 미니파이를 막는 리플렉션 사용이 없으므로 R8 을 켭니다.
      // (휴대폰 모듈은 Room/Moshi 코드젠 때문에 끈 상태를 유지합니다)
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

dependencies {
  // 워치는 화면이 작아서 Wear 전용 compose 머티리얼 대신 표준 Compose 로
  // 직접 배치합니다. 의존성 버전 충돌 위험이 없고, APK 도 가볍습니다.
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.ktx)
  implementation(libs.kotlinx.coroutines.android)
  // Data Layer (휴대폰 <-> 워치 상태/명령)
  implementation(libs.play.services.wearable)
}
