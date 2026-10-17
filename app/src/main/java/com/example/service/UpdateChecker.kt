package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 앱 업데이트 확인.
 *
 * ## 왜 스토어 페이지를 직접 파싱하지 않는가
 * OneStore 웹 페이지(`https://m.onestore.co.kr/...`)는 Next.js 로 렌더링되고,
 * 서버가 보내는 HTML 에 버전 문자열이 **포함되어 있지 않습니다** (확인함).
 * 날짜(리뷰 작성일 등)만 있고 versionCode/versionName 은 없습니다.
 * 따라서 스토어 페이지를 긁어 파싱하는 방식은 언젠가 반드시 깨집니다.
 *
 * ## 실제 동작
 * 별도로 호스팅한 `version.json` 을 읽어 버전을 비교합니다.
 * 이 파일은 앱 소스와 같은 GitHub Pages 에 있으므로 항상 같은 주소입니다.
 *   https://<계정>.github.io/ChargeSound/version.json
 *   { "versionCode": 3, "versionName": "1.2.0" }
 *
 * 파일을 못 읽거나 형식이 틀리면 **조용히 실패**합니다. 업데이트 확인은
 * 선택적 편의 기능이므로 사용자를 막거나 오류를 노출하지 않습니다.
 * 수동 확인(앱 정보 화면)은 언제든 스토어 페이지를 엽니다.
 */
object UpdateChecker {

    private const val TAG = "UpdateChecker"

    /** 앱 판매 페이지 */
    const val STORE_URL = "https://m.onestore.co.kr/v2/ko-kr/app/0001010178"

    /** 호스팅된 버전 정보 파일 (GitHub Pages) */
    const val VERSION_ENDPOINT = "https://YEJUN678.github.io/ChargeSound/version.json"

    private const val CONNECT_TIMEOUT_MS = 6000
    private const val READ_TIMEOUT_MS = 6000
    private const val TOTAL_TIMEOUT_MS = 8000L

    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String
    )

    /** 최신 버전 정보를 읽습니다. 실패 시 null (사용자에게 오류를 노출하지 않습니다). */
    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(TOTAL_TIMEOUT_MS) {
            try {
                val conn = URL(VERSION_ENDPOINT).openConnection() as HttpURLConnection
                conn.connectTimeout = CONNECT_TIMEOUT_MS
                conn.readTimeout = READ_TIMEOUT_MS
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Cache-Control", "no-cache")

                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "version endpoint returned ${conn.responseCode}")
                    conn.disconnect()
                    return@withTimeoutOrNull null
                }

                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = JSONObject(body)
                UpdateInfo(
                    versionCode = json.optInt("versionCode", -1),
                    versionName = json.optString("versionName", "")
                ).takeIf { it.versionCode > 0 }
            } catch (e: Exception) {
                Log.w(TAG, "update check failed: ${e.message}")
                null
            }
        }
    }

    /** 업데이트가 필요한지 판단합니다. */
    suspend fun isUpdateAvailable(): UpdateInfo? {
        val latest = fetchLatest() ?: return null
        return if (latest.versionCode > BuildConfig.VERSION_CODE) latest else null
    }

    /** 스토어 페이지를 브라우저로 엽니다. */
    fun openStorePage(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(STORE_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "failed to open store page", e)
        }
    }

    fun openUpdatePage(context: Context) = openStorePage(context)
}