package ph.nyxsys.vcastplayv2.Network

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

object MediaServerManager {
    @SuppressLint("StaticFieldLeak")
    var server: LocalMediaServer? = null
    var port: Int = 4200

    fun start(context: Context) {
        port = 8080
        while (true) {
            try {
                val s = LocalMediaServer(context, port)
                s.start()
                server = s
                break
            } catch (e: java.net.BindException) {
                port++
            }
        }
    }

    fun stop() {
        server?.stop()
        server = null
    }

    fun getMediaUrl(filename: String): String {
        return "http://127.0.0.1:$port/medias/$filename"
    }
}

