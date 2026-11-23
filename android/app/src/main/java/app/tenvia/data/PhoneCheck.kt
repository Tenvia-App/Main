package app.tenvia.data

import android.app.ActivityManager
import android.content.Context
import kotlin.math.ceil

/**
 * TEST BUILD: is this phone's memory enough for the on-phone AI? The AI uses about 1 GB while it
 * is loaded (plus Android and other apps). So far it has only been tried on an 8 GB phone.
 * Phones report a little less than their labelled size (a "4 GB" phone reports about 3.7 GB).
 */
object PhoneCheck {
    enum class Level { Ok, Low, TooLow }

    /** 6 GB phones and up: Ok. 4-5 GB: Low (may be slow or fail). 3 GB and less: TooLow (not started on its own). */
    fun levelFor(totalBytes: Long): Level = when {
        totalBytes <= 0L -> Level.Ok // unknown: don't block
        totalBytes < 3_300_000_000L -> Level.TooLow
        totalBytes < 5_300_000_000L -> Level.Low
        else -> Level.Ok
    }

    /** "4" for a phone that reports 3.7 GB: the size people know from the box. */
    fun gbText(totalBytes: Long): String = ceil(totalBytes / 1_000_000_000.0).toInt().toString()

    fun totalBytes(context: Context): Long = runCatching {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        info.totalMem
    }.getOrDefault(0L)

    fun level(context: Context): Level = levelFor(totalBytes(context))

    /** The line the chat screen shows, or null when the memory is fine. */
    fun message(context: Context): String? {
        val bytes = totalBytes(context)
        return when (levelFor(bytes)) {
            Level.Ok -> null
            Level.Low -> "This phone has about ${gbText(bytes)} GB of memory, so the AI may be slow or may not start. The checked guides always work."
            Level.TooLow -> "This phone has about ${gbText(bytes)} GB of memory. The AI needs about 4 GB or more, so Tenvia didn't start it. You can tap Try anyway, but the phone may get slow. The checked guides always work."
        }
    }
}
