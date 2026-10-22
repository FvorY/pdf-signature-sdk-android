package io.github.fvory.psdk.sample

import io.github.fvory.psdk.model.InitSignDocument
import org.json.JSONObject

internal object CoordinatePayload {
    fun build(
        document: InitSignDocument,
        name: String,
        reason: String,
        location: String,
        otp: String,
    ): JSONObject =
        document.toJson().apply {
            put("pdfName", name)
            put("pdfPassword", JSONObject.NULL)
            put("pdfNewPassword", JSONObject.NULL)
            put("reason", reason)
            put("otp", otp)
            put("otpType", "otp")
            put("signMoveable", false)
            put("signingType", "single")
            put("location", location)
            put("userList", JSONObject.NULL)
            put("paidAllSign", false)
            put("putQR", true)
        }
}
