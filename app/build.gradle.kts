import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

// versionCode 는 스토어에 제출할 때마다 반드시 증가시켜야 합니다.
// OneStore / Google Play 는 "이전 파일보다 낮다"고 거부합니다.
// 여기 값을 매번 고치지 않도록 gradle 속성 또는 환경변수로 받습니다:
//   gradle :app:assembleRelease -PVERSION_CODE=5
//   $env:VERSION_CODE = "5"; gradle :app:assembleRelease
val appVersionCode =
    ((project.findProperty("VERSION_CODE") as String?) ?: (System.getenv("VERSION_CODE") ?: ""))
        .toIntOrNull() ?: 3

val appVersionName =
    (project.findProperty("VERSION_NAME") as String?) ?: (System.getenv("VERSION_NAME") ?: "1.2.0")

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  // ─────────────────────────────────────────────────────────────
  // Android 개발자 인증 (ADI)
  //
  // Play 에 올릴 계획이 아니라면 아무것도 하지 않아도 됩니다.
  // OneStore 는 assets/adi-registration.properties 없��� 정보성 팝업만 띄우고
  // 등록/검수/출시에 영향을 주지 않습니다.
  //
  // Play 배포 시에만 아래 절차가 필요합니다 (OneStore 서명키를 쓰는 경우):
  //   1) https://developer.android.com/developer-verification 에서 인증 시작
  //   2) 화면에 표시된 스니펫을 아래 경로에 그대로 저장
  //        app/src/main/assets/adi-registration.properties
  //   3) 이 파일이 포함된 APK 를 OneStore 에 올림 (OneStore 가 자체 키로 재서명)
  //   4) OneStore 에서 내려받은 최종 서명 APK 를 Play Console 에 올림
  //
  // 이 파일은 인증 재질량 등 민감하므로 커밋하지 않습니다 (.gitignore 처리됨).
  // ─────────────────────────────────────────────────────────────
  defaultConfig {
    // 스토어 등록용 앱 식별자. 한 번 등록하면 변경 불가하므로 신중히 결정할 것.
    // namespace(위)는 내부 R/BuildConfig 패키지라 스토어에 노출되지 않습니다.
    applicationId = "com.chargesound.app"
    minSdk = 24
    targetSdk = 36
    versionCode = appVersionCode
    versionName = appVersionName

    // AdMob 앱 ID. 광고 단위 ID 와 같은 앱의 것이어야 합니다.
    // 실 광고로 바꿀 때는 이 값과 app/src/main/java/com/example/ad/AdUnits.kt 의
    // USE_TEST_ADS 를 **함께** 바꿔야 합니다.
    //   테스트: ca-app-pub-3940256099942544~3347511713
    //   실제  : AdMob 콘솔 → 앱 → 앱 설정에 표시되는 ca-app-pub-4709628417690363~XXXXXXXXXX
    // ADMOB_APP_ID 프로퍼티나 환경변수로 덮어쓸 수 있어 테스트 빌드를 뽑을 때 편합니다.
    manifestPlaceholders["admobAppId"] =
      (project.findProperty("ADMOB_APP_ID") as String?)
        ?: (System.getenv("ADMOB_APP_ID") ?: "ca-app-pub-4709628417690363~4678252154")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      // CI에서 임의의 alias를 쓸 수 있게 env로 받고, 없으면 "upload"로 대체
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
      isMinifyEnabled = false
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
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // appcompat 은 UI 에 쓰지 않습니다(LaunchActivity 는 ComponentActivity).
  // 다만 OnboardingActivity 가 FragmentActivity + registerForActivityResult 를 쓰는데,
  // appcompat 을 빼면 androidx.fragment 가 1.2.x 로 내려가 lintVital 이 실패합니다.
  // 그래서 fragment 버전을 끌어올리는 용도로만 유지합니다.
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  // Google Mobile Ads SDK (배너 / 네이티브 / 보상형)
  //
  // ironSource(Unity LevelPlay) 에서 AdMob 으로 전환했습니다.
  // 광고 단위 ID 는 app/src/main/java/com/example/ad/AdUnits.kt 에 있습니다.
  // 전면·앱오픈 광고는 정책상 사용하지 않습니다.
  implementation(libs.play.services.ads)
  // Wear OS 동반 앱과의 상태/명령 교환 (Wearable Data Layer)
  implementation(libs.play.services.wearable)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
