package io.github.fvory.psdk

/**
 * Entry point and metadata for the SDK.
 *
 * The viewer itself (`PdfSignatureViewer`) is added in milestone M1.
 */
public object PdfSignatureSdk {
    /** Semantic version of this SDK. Kept in sync with the git tag. */
    public val version: String = BuildConfig.SDK_VERSION

    /** Version of the web SDK (`pdf-signature-esign-sdk`) whose behavior this SDK mirrors. */
    public const val WEB_PARITY_VERSION: String = "1.1.2"
}
