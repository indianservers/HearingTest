package com.indianservers.hearingtest

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedResponsesTest {
    @Test fun keepsValidResultsAndRejectsUnsafeRestoredValues() {
        val entries = mapOf(
            "0:250" to "0.08", "1:8000" to "no", "2:22000" to "0.5",
            "0:500" to 42, "0:1000" to "NaN", "0:2000" to "Infinity",
            "0:3000" to "-0.1", "0:4000" to "1.0", "1:6000" to "invalid",
            "x:8000" to "0.1", "1:bad" to "0.1", "4:8000" to "no",
            "1:1234" to "no", "1:8000:extra" to "no", "settings" to true
        )
        assertEquals(mapOf("0:250" to "0.08", "1:8000" to "no", "2:22000" to "0.5"),
            validResponses(entries))
    }
}
