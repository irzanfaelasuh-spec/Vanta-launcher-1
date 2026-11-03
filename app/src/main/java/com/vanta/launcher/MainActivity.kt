package com.vanta.launcher

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.vanta.launcher.data.ControlLayoutStore
import com.vanta.launcher.data.LauncherDatabase
import com.vanta.launcher.ui.VantaApp
import com.vanta.launcher.ui.theme.VantaTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var database: LauncherDatabase
    private lateinit var controls: ControlLayoutStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        database = LauncherDatabase(this)
        controls = ControlLayoutStore(this)
        setContent {
            VantaTheme {
                VantaApp(
                    context = this,
                    database = database,
                    controls = controls,
                    bootstrap = { prepareLocalEnvironment() }
                )
            }
        }
    }

    private fun prepareLocalEnvironment(): List<String> {
        val directories = listOf("versions", "instances", "assets", "libraries", "runtime", "skins", "controls", "downloads", "logs", "backups")
        directories.forEach { name ->
            val directory = File(filesDir, name)
            if (!directory.exists() && !directory.mkdirs()) {
                throw IllegalStateException("Could not create app directory: $name")
            }
            if (!directory.isDirectory || !directory.canWrite()) {
                throw IllegalStateException("App directory is not writable: $name")
            }
        }
        database.writableDatabase.close()
        controls.load()
        return listOf(
            "Private launcher folders verified",
            "SQLite profile database opened",
            "Control layout storage initialized",
            "Device ABI: ${android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"}",
            "Android ${android.os.Build.VERSION.RELEASE} detected",
            "Minecraft engine: not attached in this foundation build"
        )
    }

    override fun onDestroy() {
        if (::database.isInitialized) database.close()
        super.onDestroy()
    }
}
