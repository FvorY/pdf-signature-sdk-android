package io.github.fvory.psdk.engine

import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Copies of opened documents live in `cache/psdk/<viewer id>/` (spec §15.4).
 * Each viewer owns its folder; folders left behind by a crash are removed after a day.
 */
internal class TempFileManager(
    cacheDir: File,
) {
    private val root = File(cacheDir, "psdk")
    private val dir = File(root, UUID.randomUUID().toString())

    init {
        removeStaleFolders()
    }

    fun newFile(): File {
        dir.mkdirs()
        return File(dir, "${UUID.randomUUID()}.pdf")
    }

    fun isTemp(file: File): Boolean = file.parentFile == dir

    fun deleteAll() {
        dir.deleteRecursively()
    }

    private fun removeStaleFolders() {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        root.listFiles()?.filter { it.isDirectory && it.lastModified() < cutoff }?.forEach { it.deleteRecursively() }
    }
}
