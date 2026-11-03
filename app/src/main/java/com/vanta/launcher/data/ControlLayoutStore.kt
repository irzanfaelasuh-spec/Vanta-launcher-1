package com.vanta.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

data class ControlButton(
    val id: String,
    val label: String,
    val action: String,
    val x: Float,
    val y: Float,
    val size: Float = 52f
)

data class ControlLayout(
    val name: String,
    val author: String,
    val version: Int,
    val buttons: List<ControlButton>
)

/** VANTA's portable JSON format. It is intentionally validated and versioned. */
object ControlJsonCodec {
    private const val SCHEMA = "vanta.control-layout"
    private val ACTION_PATTERN = Regex("^[A-Z0-9_]{1,24}$")

    fun encode(layout: ControlLayout): String {
        validate(layout)
        val buttons = JSONArray()
        layout.buttons.forEach { button ->
            buttons.put(JSONObject().apply {
                put("id", button.id)
                put("label", button.label)
                put("action", button.action)
                put("x", button.x.toDouble())
                put("y", button.y.toDouble())
                put("size", button.size.toDouble())
            })
        }
        return JSONObject().apply {
            put("schema", SCHEMA)
            put("version", 1)
            put("name", layout.name)
            put("author", layout.author)
            put("buttons", buttons)
        }.toString(2)
    }

    fun decode(raw: String): ControlLayout {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_JSON_BYTES) { "Control JSON is larger than 256 KB." }
        val root = JSONObject(raw)
        require(root.optString("schema") == SCHEMA) { "This file is not a VANTA control layout." }
        require(root.optInt("version", -1) == 1) { "Unsupported control layout version." }
        val buttonsJson = root.optJSONArray("buttons") ?: throw IllegalArgumentException("Missing buttons array.")
        require(buttonsJson.length() in 1..MAX_BUTTONS) { "A layout must contain 1–64 controls." }
        val buttons = buildList {
            for (i in 0 until buttonsJson.length()) {
                val item = buttonsJson.getJSONObject(i)
                add(
                    ControlButton(
                        id = item.getString("id"),
                        label = item.getString("label"),
                        action = item.getString("action"),
                        x = item.getDouble("x").toFloat(),
                        y = item.getDouble("y").toFloat(),
                        size = item.optDouble("size", 52.0).toFloat()
                    )
                )
            }
        }
        val layout = ControlLayout(
            name = root.optString("name", "Imported Layout"),
            author = root.optString("author", "Unknown"),
            version = 1,
            buttons = buttons
        )
        validate(layout)
        return layout
    }

    fun validate(layout: ControlLayout) {
        require(layout.name.isNotBlank() && layout.name.length <= 48) { "Layout name must contain 1–48 characters." }
        require(layout.author.length <= 48) { "Author name is too long." }
        require(layout.version == 1) { "Unsupported layout version." }
        require(layout.buttons.size in 1..MAX_BUTTONS) { "A layout must contain 1–64 controls." }
        require(layout.buttons.map { it.id }.distinct().size == layout.buttons.size) { "Control IDs must be unique." }
        layout.buttons.forEach { button ->
            require(button.id.length in 1..64) { "Invalid control ID." }
            require(button.label.isNotBlank() && button.label.length <= 12) { "Control labels must contain 1–12 characters." }
            require(ACTION_PATTERN.matches(button.action)) { "Invalid action '${button.action}'. Use uppercase key names such as SPACE or KEY_W." }
            require(button.x in 0f..0.92f && button.y in 0f..0.84f) { "Control position is outside the safe preview area." }
            require(button.size in 32f..96f) { "Control size must be between 32 and 96 dp." }
        }
    }

    const val MAX_JSON_BYTES = 256 * 1024
    const val MAX_BUTTONS = 64
}

class ControlLayoutStore(context: Context) {
    private val directory = File(context.filesDir, "controls").apply { mkdirs() }
    private val layoutFile = File(directory, "default.json")

    fun load(): ControlLayout {
        if (!layoutFile.exists()) {
            val layout = defaultLayout()
            save(layout)
            return layout
        }
        return ControlJsonCodec.decode(layoutFile.readText(Charsets.UTF_8))
    }

    fun save(layout: ControlLayout) {
        val json = ControlJsonCodec.encode(layout)
        val temp = File(directory, "default.json.tmp")
        temp.outputStream().use { stream ->
            stream.write(json.toByteArray(Charsets.UTF_8))
            stream.fd.sync()
        }
        // Same-directory rename is atomic on Android's app-private filesystem.
        if (temp.renameTo(layoutFile)) return

        // Fallback for filesystems that refuse replacement of an existing target.
        val backup = File(directory, "default.json.bak")
        if (backup.exists()) backup.delete()
        val hadPrevious = layoutFile.exists()
        if (hadPrevious && !layoutFile.renameTo(backup)) {
            temp.delete()
            throw IllegalStateException("Could not preserve the previous control layout.")
        }
        if (!temp.renameTo(layoutFile)) {
            if (hadPrevious) backup.renameTo(layoutFile)
            temp.delete()
            throw IllegalStateException("Could not save the new control layout.")
        }
        backup.delete()
    }

    fun importFrom(input: InputStream): ControlLayout {
        val bytes = input.use { stream ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                require(total <= ControlJsonCodec.MAX_JSON_BYTES) { "Control JSON is larger than 256 KB." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        val parsed = ControlJsonCodec.decode(bytes.toString(Charsets.UTF_8))
        save(parsed)
        return parsed
    }

    fun exportTo(output: OutputStream, layout: ControlLayout) {
        output.use { it.write(ControlJsonCodec.encode(layout).toByteArray(Charsets.UTF_8)) }
    }

    fun defaultLayout() = ControlLayout(
        name = "VANTA Touch",
        author = "VANTA",
        version = 1,
        buttons = listOf(
            ControlButton(UUID.randomUUID().toString(), "W", "KEY_W", 0.08f, 0.55f, 56f),
            ControlButton(UUID.randomUUID().toString(), "A", "KEY_A", 0.02f, 0.65f, 56f),
            ControlButton(UUID.randomUUID().toString(), "S", "KEY_S", 0.08f, 0.75f, 56f),
            ControlButton(UUID.randomUUID().toString(), "D", "KEY_D", 0.14f, 0.65f, 56f),
            ControlButton(UUID.randomUUID().toString(), "JUMP", "SPACE", 0.84f, 0.68f, 64f),
            ControlButton(UUID.randomUUID().toString(), "SPRINT", "CTRL", 0.74f, 0.78f, 52f),
            ControlButton(UUID.randomUUID().toString(), "SNEAK", "SHIFT", 0.65f, 0.78f, 48f),
            ControlButton(UUID.randomUUID().toString(), "ATTACK", "MOUSE_LEFT", 0.88f, 0.43f, 58f),
            ControlButton(UUID.randomUUID().toString(), "USE", "MOUSE_RIGHT", 0.78f, 0.48f, 58f),
            ControlButton(UUID.randomUUID().toString(), "INV", "KEY_E", 0.91f, 0.18f, 48f),
            ControlButton(UUID.randomUUID().toString(), "CHAT", "KEY_T", 0.04f, 0.12f, 48f)
        )
    )
}
