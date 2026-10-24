package com.example.diagnostics

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 로컬 크래시 기록기.
 *
 * ## 왜 Crashlytics 가 아닌가
 * 이 프로젝트에는 `google-services.json` 이 없습니다. Firebase Crashlytics 는
 * 그 파일(= Firebase 프로젝트) 없이는 초기화되지 않아서 아무것도 보고하지 못합니다.
 * 서버·계정 없이도 **최소한 다음 실행 때 원인을 볼 수 있게** 하는 것이 목표라
 * 스택 트레이스를 기기에 파일로 남기는 방식으로 처리했습니다.
 *
 * ## 동작
 * 1. [install] 이 기존 핸들러를 감싸서, 크래시 직전에 리포트를 파일로 씁니다.
 * 2. 파일은 `files/crash_reports/crash-YYYYMMDD-HHmmss.txt`.
 * 3. 앱을 다시 열면 오버플로 메뉴에 "오류 기록" 항목이 나타나고, 공유 시트로
 *    개발자에게 그대로 보낼 수 있습니다.
 *
 * 나중에 Firebase 프로젝트를 만들면 `google-services.json` 만 넣고
 * Crashlytics 로 교체하면 됩니다. 리포트 형식은 그대로 쓸 수 있습니다.
 */
object CrashReporter {

    private const val TAG = "CrashReporter"
    private const val DIR_NAME = "crash_reports"
    private const val MAX_REPORTS = 5

    /** 기존 핸들러를 체이닝해서 설치합니다. 여러 번 호출해도 안전합니다. */
    fun install(context: Context) {
        val app = context.applicationContext
        if (installed) return
        installed = true

        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                writeReport(app, thread, error)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to write crash report", t)
            }
            // 원래 핸들러를 반드시 불러야 시스템이 앱을 정상 종료 처리합니다.
            previous?.uncaughtException(thread, error)
        }
        Log.d(TAG, "Crash handler installed")
    }

    @Volatile
    private var installed = false

    private fun dir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    private fun writeReport(context: Context, thread: Thread, error: Throwable) {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(dir(context), "crash-$stamp.txt")
        file.writeText(buildReport(context, thread, error), Charsets.UTF_8)

        // 최근 것만 남깁니다 — 사용자 저장 공간을 계속 먹지 않도록.
        dir(context).listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_REPORTS)
            ?.forEach { it.delete() }
    }

    @Suppress("DEPRECATION")
    private fun buildReport(context: Context, thread: Thread, error: Throwable): String {
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "?"
        }
        val versionCode = try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
        } catch (e: Exception) {
            0L
        }

        return buildString {
            appendLine("ChargeSound crash report")
            appendLine("time     : " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            appendLine("version  : $versionName ($versionCode)")
            appendLine("device   : ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("android  : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("thread   : ${thread.name}")
            appendLine()
            appendLine(Log.getStackTraceString(error))
        }
    }

    /** 최신순 리포트 파일 목록 */
    fun reports(context: Context): List<File> =
        dir(context).listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun count(context: Context): Int = reports(context).size

    fun latestText(context: Context): String? =
        reports(context).firstOrNull()?.let {
            try {
                it.readText(Charsets.UTF_8)
            } catch (e: Exception) {
                null
            }
        }

    /**
     * 최근 리포트를 공유 시트로 넘깁니다.
     * @return 리포트가 없으면 false
     */
    fun share(context: Context): Boolean {
        val text = latestText(context) ?: return false
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "ChargeSound crash report")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, null)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.w(TAG, "share failed", e)
            return false
        }
        return true
    }
}
