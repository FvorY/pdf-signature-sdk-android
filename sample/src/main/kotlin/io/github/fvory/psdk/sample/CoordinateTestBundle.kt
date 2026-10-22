package io.github.fvory.psdk.sample

import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal object CoordinateTestBundle {
    fun write(
        output: OutputStream,
        source: ByteArray,
        payload: JSONObject,
        audit: JSONObject,
    ) {
        val hash = MessageDigest.getInstance("SHA-256").digest(source).joinToString("") { "%02x".format(it) }
        audit.put("sourceSha256", hash)
        val body = JSONObject(payload.toString())
        if (body.optString("otp").isBlank()) body.put("otp", "{{otp}}")
        val collection =
            JSONObject()
                .put(
                    "info",
                    JSONObject()
                        .put("name", "ezSign coordinate test")
                        .put("schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"),
                ).put(
                    "variable",
                    JSONArray()
                        .put(JSONObject().put("key", "accessToken").put("value", ""))
                        .put(JSONObject().put("key", "otp").put("value", "")),
                ).put(
                    "item",
                    JSONArray().put(
                        JSONObject().put("name", "Sign captured source PDF").put(
                            "request",
                            JSONObject()
                                .put("method", "POST")
                                .put("url", "https://gtm.myesign.id/api/signing/initsign")
                                .put(
                                    "auth",
                                    JSONObject().put("type", "bearer").put(
                                        "bearer",
                                        JSONArray().put(
                                            JSONObject()
                                                .put("key", "token")
                                                .put("value", "{{accessToken}}")
                                                .put("type", "string"),
                                        ),
                                    ),
                                ).put("header", JSONArray().put(JSONObject().put("key", "Content-Type").put("value", "application/json")))
                                .put(
                                    "body",
                                    JSONObject()
                                        .put("mode", "raw")
                                        .put("raw", body.toString(2))
                                        .put("options", JSONObject().put("raw", JSONObject().put("language", "json"))),
                                ),
                        ),
                    ),
                )
        ZipOutputStream(output).use { zip ->
            fun add(
                name: String,
                bytes: ByteArray,
            ) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
            add("source.pdf", source)
            add("request.json", payload.toString(2).toByteArray())
            add("audit.json", audit.toString(2).toByteArray())
            add("postman_collection.json", collection.toString(2).toByteArray())
            add(
                "README.txt",
                (
                    "Import postman_collection.json into Postman. Set collection variables accessToken and otp. " +
                        "The request contains the same PDF bytes as source.pdf. Keep the body intact. " +
                        "Compare source.pdf with the SDK screenshot and inspect audit.json for the captured item coordinates. " +
                        "Source SHA-256: " + hash
                ).toByteArray(),
            )
        }
    }
}
