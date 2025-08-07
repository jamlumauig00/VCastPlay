package ph.nyxsys.vcastplayv2.Network

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream

class LocalMediaServer(
    private val context: Context,
    port: Int
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        if (uri.startsWith("/medias/")) {
            val filename = uri.removePrefix("/medias/")
            val file = File(context.getExternalFilesDir(null), "vcastplay 2.0/assets/medias/$filename")

            return if (file.exists()) {
                newChunkedResponse(Response.Status.OK, getMimeType(filename), file.inputStream())
            } else {
                newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found")
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Invalid path")
    }

    private fun getMimeType(filename: String): String {
        return when {
            filename.endsWith(".mp4") -> "video/mp4"
            filename.endsWith(".jpg") || filename.endsWith(".jpeg") -> "image/jpeg"
            filename.endsWith(".png") -> "image/png"
            filename.endsWith(".mp3") -> "audio/mpeg"
            else -> "application/octet-stream"
        }
    }
}
