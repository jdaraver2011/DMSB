package com.proteksolutions.spamshield.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Small SQLite store for rules (block/allow lists) and the blocked-call history. */
class SpamDb private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "spamshield.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                value TEXT NOT NULL,
                label TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                UNIQUE(type, value) ON CONFLICT REPLACE)"""
        )
        db.execSQL(
            """CREATE TABLE blocked_calls (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                number TEXT,
                reason TEXT NOT NULL,
                ts INTEGER NOT NULL,
                reported_7726 INTEGER NOT NULL DEFAULT 0,
                reported_ftc INTEGER NOT NULL DEFAULT 0)"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    // ---- Rules ----

    fun addRule(rule: Rule): Long {
        val value = when (rule.type) {
            RuleType.BLOCK_NUMBER, RuleType.ALLOW_NUMBER -> PhoneNumbers.normalize(rule.value)
            RuleType.BLOCK_PREFIX, RuleType.BLOCK_AREA_CODE -> PhoneNumbers.digits(rule.value)
        }
        if (value.isEmpty()) return -1
        // A number can't be both blocked and allowed: adding one removes the other.
        when (rule.type) {
            RuleType.BLOCK_NUMBER -> deleteRule(RuleType.ALLOW_NUMBER, value)
            RuleType.ALLOW_NUMBER -> deleteRule(RuleType.BLOCK_NUMBER, value)
            else -> Unit
        }
        return writableDatabase.insert("rules", null, ContentValues().apply {
            put("type", rule.type.name)
            put("value", value)
            put("label", rule.label)
            put("created_at", rule.createdAt)
        })
    }

    fun deleteRule(id: Long) {
        writableDatabase.delete("rules", "id = ?", arrayOf(id.toString()))
    }

    fun deleteRule(type: RuleType, value: String) {
        writableDatabase.delete("rules", "type = ? AND value = ?", arrayOf(type.name, value))
    }

    fun rules(type: RuleType? = null): List<Rule> {
        val sel = type?.let { "type = ?" }
        val args = type?.let { arrayOf(it.name) }
        readableDatabase.query("rules", null, sel, args, null, null, "created_at DESC").use { c ->
            val out = ArrayList<Rule>(c.count)
            while (c.moveToNext()) {
                val t = runCatching { RuleType.valueOf(c.getString(c.getColumnIndexOrThrow("type"))) }
                    .getOrNull() ?: continue
                out += Rule(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    type = t,
                    value = c.getString(c.getColumnIndexOrThrow("value")),
                    label = c.getString(c.getColumnIndexOrThrow("label")),
                    createdAt = c.getLong(c.getColumnIndexOrThrow("created_at")),
                )
            }
            return out
        }
    }

    // ---- Blocked call log ----

    fun logBlocked(number: String?, reason: String): Long =
        writableDatabase.insert("blocked_calls", null, ContentValues().apply {
            put("number", number)
            put("reason", reason)
            put("ts", System.currentTimeMillis())
        })

    fun blockedCalls(limit: Int = 500): List<BlockedCall> {
        readableDatabase.query(
            "blocked_calls", null, null, null, null, null, "ts DESC", limit.toString()
        ).use { c ->
            val out = ArrayList<BlockedCall>(c.count)
            while (c.moveToNext()) {
                out += BlockedCall(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    number = c.getString(c.getColumnIndexOrThrow("number")),
                    reason = c.getString(c.getColumnIndexOrThrow("reason")),
                    timestamp = c.getLong(c.getColumnIndexOrThrow("ts")),
                    reported7726 = c.getInt(c.getColumnIndexOrThrow("reported_7726")) == 1,
                    reportedFtc = c.getInt(c.getColumnIndexOrThrow("reported_ftc")) == 1,
                )
            }
            return out
        }
    }

    /** Marks every logged call from [number] as reported via the given channel. */
    fun markReported(number: String, via7726: Boolean) {
        val col = if (via7726) "reported_7726" else "reported_ftc"
        writableDatabase.update(
            "blocked_calls", ContentValues().apply { put(col, 1) },
            "number = ?", arrayOf(PhoneNumbers.normalize(number))
        )
    }

    fun clearBlockedLog() {
        writableDatabase.delete("blocked_calls", null, null)
    }

    companion object {
        @Volatile private var instance: SpamDb? = null
        fun get(context: Context): SpamDb =
            instance ?: synchronized(this) { instance ?: SpamDb(context).also { instance = it } }
    }
}
