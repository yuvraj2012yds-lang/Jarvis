package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TaskEntity::class,
        ScheduleEntity::class,
        ActionLogEntity::class,
        ArtifactEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {

    abstract fun jarvisDao(): JarvisDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_core.db"
                )
                    .addCallback(JarvisDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class JarvisDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.jarvisDao())
                }
            }
        }

        private suspend fun populateInitialData(dao: JarvisDao) {
            // Seed initial tasks
            dao.insertTask(
                TaskEntity(
                    title = "Calibrate Quantum Arc Telemetry",
                    priority = "HIGH",
                    category = "Systems"
                )
            )
            dao.insertTask(
                TaskEntity(
                    title = "Review Security Firewall Encryption",
                    priority = "MEDIUM",
                    category = "Security"
                )
            )
            dao.insertTask(
                TaskEntity(
                    title = "Sync High-Speed Neural Core Buffer",
                    priority = "LOW",
                    category = "Maintenance"
                )
            )

            // Seed initial schedule
            dao.insertSchedule(
                ScheduleEntity(
                    time = "09:00 AM",
                    title = "Autonomous Diagnostics Briefing",
                    description = "Core subsystems integrity check and status review",
                    location = "Primary Lab"
                )
            )
            dao.insertSchedule(
                ScheduleEntity(
                    time = "11:30 AM",
                    title = "Flight Telemetry & Propulsion Review",
                    description = "Review Mark VII avionics test logs",
                    location = "Hangar Bay 3"
                )
            )
            dao.insertSchedule(
                ScheduleEntity(
                    time = "03:00 PM",
                    title = "Global Network Security Audit",
                    description = "Zero-trust protocol verification",
                    location = "Control Room"
                )
            )

            // Seed initial action log
            dao.insertLog(
                ActionLogEntity(
                    command = "INITIALIZE_CORE",
                    actionType = "SYSTEM_BOOT",
                    summary = "JARVIS Autonomous Core v4.2 online. Neural bus synchronized.",
                    status = "SUCCESS"
                )
            )

            // Seed initial deliverable artifact
            val initialCode = """// ==========================================
// JARVIS CORE AUTONOMOUS WORKSPACE v4.2
// Status: ALL SUBSYSTEMS NOMINAL
// ==========================================

import asyncio
from typing import Dict, Any

class JarvisNeuralEngine:
    def __init__(self, mode: str = "AUTONOMOUS"):
        self.mode = mode
        self.wake_word = "Jarvis"
        self.system_status = "ONLINE"
        self.latency_ms = 4.2

    async def execute_voice_command(self, query: str) -> Dict[str, Any]:
        # Immediate direct execution
        return {
            "status": "COMPLETED",
            "voice_response": "On it, sir. Workspace updated immediately.",
            "artifact_rendered": True
        }

if __name__ == "__main__":
    engine = JarvisNeuralEngine()
    print("JARVIS Core active. Awaiting voice directive.")
"""
            dao.insertArtifact(
                ArtifactEntity(
                    title = "JarvisNeuralEngine.py",
                    type = "CODE",
                    language = "python",
                    content = initialCode,
                    previewDetails = "Python 3.12 • Autonomous Core Architecture"
                )
            )
        }
    }
}
