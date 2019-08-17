package ru.melodin.fast.api

import org.jetbrains.annotations.Contract
import java.util.*

object Scopes {

    private const val NOTIFY = "notify"
    private const val FRIENDS = "friends"
    private const val PHOTOS = "photos"
    private const val AUDIO = "audio"
    private const val VIDEO = "video"
    private const val DOCS = "docs"
    private const val NOTES = "notes"
    private const val PAGES = "pages"
    private const val STATUS = "status"
    private const val WALL = "wall"
    private const val GROUPS = "conversation_groups"
    private const val MESSAGES = "messages"
    private const val NOTIFICATIONS = "notifications"
    private const val STATS = "stats"
    private const val ADS = "ads"
    private const val OFFLINE = "offline"
    private const val EMAIL = "email"

    fun parse(permissions: Int): ArrayList<String> {
        val res = ArrayList<String>(16)
        if (permissions and 1 > 0) res.add(NOTIFY)
        if (permissions and 2 > 0) res.add(FRIENDS)
        if (permissions and 4 > 0) res.add(PHOTOS)
        if (permissions and 8 > 0) res.add(AUDIO)
        if (permissions and 16 > 0) res.add(VIDEO)
        if (permissions and 128 > 0) res.add(PAGES)
        if (permissions and 1024 > 0) res.add(STATUS)
        if (permissions and 2048 > 0) res.add(NOTES)
        if (permissions and 4096 > 0) res.add(MESSAGES)
        if (permissions and 8192 > 0) res.add(WALL)
        if (permissions and 32768 > 0) res.add(ADS)
        if (permissions and 65536 > 0) res.add(OFFLINE)
        if (permissions and 131072 > 0) res.add(DOCS)
        if (permissions and 262144 > 0) res.add(GROUPS)
        if (permissions and 524288 > 0) res.add(NOTIFICATIONS)
        if (permissions and 1048576 > 0) res.add(STATS)
        if (permissions and 4194304 > 0) res.add(EMAIL)
        return res
    }

    @Contract(pure = true)
    fun all(): String {
        return "$NOTIFY,$FRIENDS,$PHOTOS,$AUDIO,$VIDEO,$PAGES,$STATUS,$NOTES,$WALL,$ADS,$OFFLINE,$DOCS,$GROUPS,$NOTIFICATIONS,$MESSAGES,$STATS"
    }

    fun allInt(): String {
        return 136297695.toString()
    }
}
