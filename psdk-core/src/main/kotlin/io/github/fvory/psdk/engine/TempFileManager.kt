package io.github.fvory.psdk.engine

import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Copies of opened documents and item images live in `cache/psdk/<id>/` (spec §15.4). Each viewer
 * owns one folder; a viewer restored after process death passes its old [existingId] to keep
 * using the same files. Folders left behind by a crash are removed after a day.
 */
internal class TempFileManager(
    cacheDir: File,
    existingId: String? = null,
) {
    private val root = File(cacheDir, "psdk")

    val id: String = existingId?.takeIf { File(root, it).isDirectory } ?: UUID.randomUUID().toString()

    private val dir = File(root, id)

    init {
        removeStaleFolders()
        if (dir.isDirectory) dir.setLastModified(System.currentTimeMillis())
    }

    fun newFile(): File {
        dir.mkdirs()
        return File(dir, "${UUID.randomUUID()}.pdf")
    }

    /** Where the PNG of an item's image is kept. */
    fun imageFile(itemId: String): File = File(File(dir, "images"), "${itemId.replace(Regex("[^A-Za-z0-9_-]"), "_")}.png")

    fun isTemp(file: File): Boolean = file.absoluteFile.startsWith(dir.absoluteFile)

    /** Deletes [file] only when it is one of this viewer's temp files. */
    fun delete(file: File) {
        if (isTemp(file)) file.delete()
    }

    fun deleteAll() {
        dir.deleteRecursively()
    }

    private fun removeStaleFolders() {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        root.listFiles()?.filter { it.isDirectory && it != dir && it.lastModified() < cutoff }?.forEach { it.deleteRecursively() }
    }
}
