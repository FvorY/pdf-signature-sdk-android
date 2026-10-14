package io.github.fvory.psdk.error

/** Errors thrown by the SDK. */
public sealed class PsdkException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** The source cannot be read: missing file, unsupported URL, unreadable URI. */
    public class InvalidSource(
        message: String,
        cause: Throwable? = null,
    ) : PsdkException(message, cause)

    /** Downloading a [io.github.fvory.psdk.model.PdfSource.UrlSource] failed. [code] is the HTTP status, if any. */
    public class Network(
        public val code: Int?,
        message: String,
        cause: Throwable? = null,
    ) : PsdkException(message, cause)

    /** The data does not start with a PDF header. */
    public class NotAPdf : PsdkException("The document is not a PDF file.")

    /** The PDF is damaged or uses features the renderer cannot open. */
    public class Corrupted(
        cause: Throwable? = null,
    ) : PsdkException("The PDF file is damaged or cannot be opened.", cause)

    /** The user cancelled the password prompt. */
    public class PasswordCancelled : PsdkException("Opening the document was cancelled at the password prompt.")

    /** The PDF is password protected and no engine on this device can open it. */
    public class PasswordNotSupported :
        PsdkException(
            "This PDF is password protected. Android 14 and lower need the optional psdk-pdfium module to open it.",
        )

    /** A page number outside `1..totalPages` was passed. */
    public class InvalidPage(
        public val page: Int,
    ) : PsdkException("Page $page does not exist.")

    /** The call needs a loaded document. */
    public class NotLoaded : PsdkException("No document is loaded.")

    /** The viewer was destroyed. */
    public class Destroyed : PsdkException("The viewer was destroyed.")
}
