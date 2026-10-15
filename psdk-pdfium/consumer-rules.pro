# Rules applied to apps that depend on psdk-pdfium.
# psdk-core finds the Pdfium engine by class name (EngineRegistry), so R8 must not rename or
# remove it, or password-protected PDFs stop opening on Android 14 and lower in release builds.
-keep class io.github.fvory.psdk.pdfium.PdfiumEngine {
    public static io.github.fvory.psdk.engine.DocumentEngine open(java.io.File, java.lang.String);
}
-keepnames class io.github.fvory.psdk.pdfium.PdfiumPasswordRequiredException
# JNI rules for io.legere:pdfiumandroid ship inside that library.
