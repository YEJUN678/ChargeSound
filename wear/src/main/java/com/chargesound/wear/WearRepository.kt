package com.chargesound.wear

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 휴대폰이 보내준 마지막 배터리 상태.
 *
 * 워치는 스스로 휴대폰 배터리를 읽을 수 없으므로, Data Layer 로 받은 값만
 * 보여줍니다. 값이 없으면 null 이고, UI 는 "연결 대기 중" 을 표시합니다.
 */
data class PhoneSnapshot(
    val level: Int,
    val charging: Boolean,
    /** 와트. 휴대폰이 측정하지 못하면 null (0 이 아닙니다). */
    val watts: Float?,
    val tempC: Int,
    val serviceEnabled: Boolean,
    /** 휴대폰이 값을 만든 시각 (epoch millis) */
    val updatedAt: Long
)

/**
 * Wearable Data Layer 클라이언트.
 *
 * - 상태: `/chargesound/battery` (DataItem — 워치가 꺼져 있어도 마지막 값 유지)
 * - 명령: `/chargesound/command` (Message — 즉시 전달, 저장 안 함)
 */
class WearRepository(context: Context) {

    companion object {
        private const val TAG = "WearRepository"

        const val PATH_STATE = "/chargesound/battery"
        const val PATH_COMMAND = "/chargesound/command"

        const val CMD_TOGGLE = "toggle_service"
        const val CMD_OPEN_APP = "open_app"
        const val CMD_REFRESH = "refresh"

        private const val KEY_LEVEL = "level"
        private const val KEY_CHARGING = "charging"
        private const val KEY_WATTS = "watts"
        private const val KEY_TEMP = "temp"
        private const val KEY_SERVICE = "service"
        private const val KEY_TS = "ts"
    }

    private val appContext = context.applicationContext

    private val dataClient by lazy { Wearable.getDataClient(appContext) }
    private val messageClient by lazy { Wearable.getMessageClient(appContext) }
    private val nodeClient by lazy { Wearable.getNodeClient(appContext) }

    private val _snapshot = MutableStateFlow<PhoneSnapshot?>(null)
    val snapshot: StateFlow<PhoneSnapshot?> = _snapshot.asStateFlow()

    private var listener: DataClient.OnDataChangedListener? = null

    fun start() {
        if (listener == null) {
            val l = DataClient.OnDataChangedListener { events ->
                events.lastOrNull { it.dataItem.uri.path == PATH_STATE }?.let {
                    parse(it.dataItem)
                }
            }
            listener = l
            dataClient.addListener(l)
                .addOnFailureListener { Log.w(TAG, "addListener failed", it) }
        }
        loadLatest()
    }

    fun stop() {
        listener?.let { dataClient.removeListener(it) }
        listener = null
    }

    /** 워치를 새로 켰을 때 마지막으로 저장된 값을 한 번 읽습니다. */
    private fun loadLatest() {
        dataClient.getDataItems(Uri.parse("wear://*$PATH_STATE"))
            .addOnSuccessListener { buffer ->
                try {
                    var newest: PhoneSnapshot? = null
                    for (item in buffer) {
                        val parsed = parse(item)
                        if (parsed != null && (newest == null || parsed.updatedAt > newest.updatedAt)) {
                            newest = parsed
                        }
                    }
                    if (newest != null) _snapshot.value = newest
                } catch (e: Exception) {
                    Log.w(TAG, "loadLatest failed", e)
                } finally {
                    buffer.release()
                }
            }
            .addOnFailureListener { Log.w(TAG, "getDataItems failed", it) }
    }

    private fun parse(item: com.google.android.gms.wearable.DataItem): PhoneSnapshot? {
        return try {
            val map = DataMapItem.fromDataItem(item).dataMap
            val wattsRaw = map.getFloat(KEY_WATTS, -1f)
            val snapshot = PhoneSnapshot(
                level = map.getInt(KEY_LEVEL, 0),
                charging = map.getBoolean(KEY_CHARGING, false),
                watts = if (wattsRaw > 0f) wattsRaw else null,
                tempC = map.getInt(KEY_TEMP, 0),
                serviceEnabled = map.getBoolean(KEY_SERVICE, true),
                updatedAt = map.getLong(KEY_TS, 0L)
            )
            _snapshot.value = snapshot
            snapshot
        } catch (e: Exception) {
            Log.w(TAG, "parse failed", e)
            null
        }
    }

    /**
     * 휴대폰에 명령을 보냅니다.
     *
     * 연결된 노드가 하나도 없으면 조용히 실패합니다. 워치는 휴대폰이
     * 없어도 "연결 대기 중" 상태로 계속 동작해야 하기 때문입니다.
     */
    fun sendCommand(command: String) {
        val payload = command.toByteArray(Charsets.UTF_8)
        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    Log.i(TAG, "no connected node, command dropped: $command")
                    return@addOnSuccessListener
                }
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, PATH_COMMAND, payload)
                }
            }
            .addOnFailureListener { Log.w(TAG, "connectedNodes failed", it) }
    }
}
