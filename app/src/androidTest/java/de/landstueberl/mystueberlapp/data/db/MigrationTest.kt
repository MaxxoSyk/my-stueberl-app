package de.landstueberl.mystueberlapp.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Release data must survive every update. Version 19 is the first schema with
 * real data (beta 1); every later version needs a migration from it.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDb = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StueberlDatabase::class.java
    )

    @Test
    fun baselineOpensWithCurrentCode() {
        // A database exactly as beta 1 creates it…
        helper.createDatabase(testDb, BASELINE_VERSION).close()

        // …must open with the current code, through all migrations.
        // Room fails here if a migration is missing or the schema doesn't match.
        Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            StueberlDatabase::class.java,
            testDb
        ).build().apply {
            openHelper.writableDatabase
            close()
        }
    }

    private companion object {
        const val BASELINE_VERSION = 19
    }
}