package com.vanta.launcher.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class LocalAccount(
    val id: Long,
    val username: String,
    val skinPath: String,
    val createdAt: Long
)

/**
 * Private, on-device account database. No network account service and no Firebase.
 * Passwords are never stored in plaintext; only salted PBKDF2 hashes are persisted.
 */
class LauncherDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    "vanta-local.db",
    null,
    1
) {
    private val prefs = context.applicationContext.getSharedPreferences("vanta-session", Context.MODE_PRIVATE)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL COLLATE NOCASE UNIQUE,
                password_hash BLOB NOT NULL,
                password_salt BLOB NOT NULL,
                skin_path TEXT NOT NULL DEFAULT 'builtin:steve',
                created_at INTEGER NOT NULL
            )""".trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future schema migrations must preserve user profiles. Never drop this table here.
    }

    @Synchronized
    fun createAccount(usernameInput: String, password: String): LocalAccount {
        val username = usernameInput.trim()
        require(username.matches(Regex("^[A-Za-z0-9_]{3,16}$"))) {
            "Username must be 3–16 characters and use letters, numbers, or underscores."
        }
        require(password.length >= 6) { "Password must contain at least 6 characters." }
        require(findByUsername(username) == null) { "That username already exists on this device." }

        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derivePassword(password, salt)
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("username", username)
            put("password_hash", hash)
            put("password_salt", salt)
            put("skin_path", "builtin:steve")
            put("created_at", now)
        }
        val id = writableDatabase.insertOrThrow("accounts", null, values)
        prefs.edit().putLong(KEY_ACTIVE_ACCOUNT, id).apply()
        return LocalAccount(id, username, "builtin:steve", now)
    }

    @Synchronized
    fun authenticate(usernameInput: String, password: String): LocalAccount? {
        val accountRow = findCredentials(usernameInput.trim()) ?: return null
        val calculated = derivePassword(password, accountRow.salt)
        if (!MessageDigest.isEqual(calculated, accountRow.hash)) return null
        prefs.edit().putLong(KEY_ACTIVE_ACCOUNT, accountRow.account.id).apply()
        return accountRow.account
    }

    @Synchronized
    fun activate(accountId: Long): Boolean {
        val account = getAccount(accountId) ?: return false
        prefs.edit().putLong(KEY_ACTIVE_ACCOUNT, account.id).apply()
        return true
    }

    fun activeAccount(): LocalAccount? {
        val id = prefs.getLong(KEY_ACTIVE_ACCOUNT, INVALID_ACCOUNT)
        return if (id == INVALID_ACCOUNT) null else getAccount(id)
    }

    fun listAccounts(): List<LocalAccount> {
        val result = mutableListOf<LocalAccount>()
        readableDatabase.query(
            "accounts",
            arrayOf("id", "username", "skin_path", "created_at"),
            null, null, null, null,
            "created_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) result += cursor.toAccount()
        }
        return result
    }

    fun getAccount(id: Long): LocalAccount? {
        readableDatabase.query(
            "accounts",
            arrayOf("id", "username", "skin_path", "created_at"),
            "id = ?", arrayOf(id.toString()), null, null, null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toAccount() else null
        }
    }

    @Synchronized
    fun updateSkin(accountId: Long, skinPath: String): Boolean {
        val values = ContentValues().apply { put("skin_path", skinPath) }
        return writableDatabase.update("accounts", values, "id = ?", arrayOf(accountId.toString())) == 1
    }

    @Synchronized
    fun deleteAccount(accountId: Long) {
        writableDatabase.delete("accounts", "id = ?", arrayOf(accountId.toString()))
        if (prefs.getLong(KEY_ACTIVE_ACCOUNT, INVALID_ACCOUNT) == accountId) {
            val next = listAccounts().firstOrNull()
            if (next == null) prefs.edit().remove(KEY_ACTIVE_ACCOUNT).apply()
            else prefs.edit().putLong(KEY_ACTIVE_ACCOUNT, next.id).apply()
        }
    }

    private fun findByUsername(username: String): LocalAccount? {
        readableDatabase.query(
            "accounts",
            arrayOf("id", "username", "skin_path", "created_at"),
            "username = ?", arrayOf(username), null, null, null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toAccount() else null
        }
    }

    private data class CredentialRow(val account: LocalAccount, val hash: ByteArray, val salt: ByteArray)

    private fun findCredentials(username: String): CredentialRow? {
        readableDatabase.query(
            "accounts",
            arrayOf("id", "username", "skin_path", "created_at", "password_hash", "password_salt"),
            "username = ?", arrayOf(username), null, null, null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val account = cursor.toAccount()
            val hash = cursor.getBlob(cursor.getColumnIndexOrThrow("password_hash"))
            val salt = cursor.getBlob(cursor.getColumnIndexOrThrow("password_salt"))
            return CredentialRow(account, hash, salt)
        }
    }

    private fun android.database.Cursor.toAccount() = LocalAccount(
        id = getLong(getColumnIndexOrThrow("id")),
        username = getString(getColumnIndexOrThrow("username")),
        skinPath = getString(getColumnIndexOrThrow("skin_path")) ?: "builtin:steve",
        createdAt = getLong(getColumnIndexOrThrow("created_at"))
    )

    private fun derivePassword(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        private const val KEY_ACTIVE_ACCOUNT = "active_account_id"
        private const val INVALID_ACCOUNT = -1L
        private const val PBKDF2_ITERATIONS = 160_000
        private const val HASH_BITS = 256
    }
}
