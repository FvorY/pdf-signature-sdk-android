package io.github.fvory.psdk.sample

import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.ZipInputStream

class CoordinateTestBundleTest {
    @Test fun bundleContainsIdenticalSourceAndPostmanBody() {
        val bytes = "%PDF-test-source".toByteArray()
        val encoded = Base64.getEncoder().encodeToString(bytes)
        val request = JSONObject().put("base64Pdf", encoded).put("otp", "")
        val output = ByteArrayOutputStream()
        CoordinateTestBundle.write(output, bytes, request, JSONObject())
        val files = mutableMapOf<String, ByteArray>()
        ZipInputStream(output.toByteArray().inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                files[entry.name] = zip.readBytes()
            }
        }
        assertArrayEquals(bytes, files.getValue("source.pdf"))
        assertEquals(encoded, JSONObject(files.getValue("request.json").toString(Charsets.UTF_8)).getString("base64Pdf"))
        val collection = JSONObject(files.getValue("postman_collection.json").toString(Charsets.UTF_8))
        val body =
            collection
                .getJSONArray("item")
                .getJSONObject(0)
                .getJSONObject("request")
                .getJSONObject("body")
        assertEquals(encoded, JSONObject(body.getString("raw")).getString("base64Pdf"))
        assertEquals("{{otp}}", JSONObject(body.getString("raw")).getString("otp"))
        assertEquals(64, JSONObject(files.getValue("audit.json").toString(Charsets.UTF_8)).getString("sourceSha256").length)
    }
}
