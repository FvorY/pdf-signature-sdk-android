package io.github.fvory.psdk.sample

import io.github.fvory.psdk.model.PageSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CoordinatePdfMetadataTest {
    @Test fun fractionalDimensionsRemainPdfPoints() {
        assertEquals(PageSize(198.0, 255.12), CoordinatePdfMetadata.displaySize(198f, 255.12f, 0))
    }

    @Test fun swapsDimensionsForNinetyAndTwoHundredSeventyDegrees() {
        for (rotation in 0..3) {
            val expected = if (rotation % 2 == 0) PageSize(198.0, 255.12) else PageSize(255.12, 198.0)
            assertEquals(expected, CoordinatePdfMetadata.displaySize(198f, 255.12f, rotation))
        }
    }

    @Test fun rejectsInvalidMetadata() {
        assertThrows(IllegalArgumentException::class.java) { CoordinatePdfMetadata.displaySize(Float.NaN, 255f, 0) }
        assertThrows(IllegalArgumentException::class.java) { CoordinatePdfMetadata.displaySize(198f, 0f, 0) }
        assertThrows(IllegalArgumentException::class.java) { CoordinatePdfMetadata.displaySize(198f, 255f, 4) }
    }
}
