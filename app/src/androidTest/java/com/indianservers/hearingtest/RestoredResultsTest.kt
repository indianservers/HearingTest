package com.indianservers.hearingtest

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

class RestoredResultsTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun invalidSavedDataDoesNotCrashStartupOrResults() {
        val prefs = InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("hearing", 0)
        val previous = prefs.all
        prefs.edit().clear().putInt("0:250", 42).putString("1:bad", "0.1")
            .putString("1:8000", "NaN").putString("2:8000", "0.08").commit()
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                compose.onNodeWithText("Results", useUnmergedTree = true).performClick()
                compose.onNodeWithText("Your Hearing Range").assertIsDisplayed()
                scenario.recreate()
                compose.onNodeWithText("Your Hearing Range").assertIsDisplayed()
            }
        } finally {
            val editor = prefs.edit().clear()
            previous.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
            editor.commit()
        }
    }
}
