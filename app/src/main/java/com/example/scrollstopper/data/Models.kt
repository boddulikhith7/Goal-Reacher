package com.example.scrollstopper.data

enum class BlockType(val label: String, val timeRange: String, val xpValue: Int) {
    BLOCK1("Block 1 (Theory)", "6:00 AM - 8:00 AM", 20),
    BLOCK2("Block 2 (Practice)", "6:00 PM - 8:30 PM", 20),
    BLOCK3("Block 3 (Rotation)", "9:00 PM - 10:00 PM", 10)
}

data class WeekPlan(
    val weekNumber: Int,
    val phase: String,
    val subjects: String,
    val topic: String,
    val block1Source: String,
    val block2Source: String
) {
    fun serialize(): String {
        return "$weekNumber^$phase^$subjects^$topic^$block1Source^$block2Source"
    }

    companion object {
        fun deserialize(str: String): WeekPlan? {
            val parts = str.split("^")
            if (parts.size < 6) return null
            return WeekPlan(
                weekNumber = parts[0].toIntOrNull() ?: 1,
                phase = parts[1],
                subjects = parts[2],
                topic = parts[3],
                block1Source = parts[4],
                block2Source = parts[5]
            )
        }
    }
}

data class StudyBlockState(
    val type: BlockType,
    val title: String,
    val source: String,
    val isCompleted: Boolean
)

data class TimetableBlock(
    val id: String,
    val label: String,
    val timeRange: String,
    val xpValue: Int = 20,
    val isCompleted: Boolean = false
) {
    fun serialize(): String {
        return "$id|$label|$timeRange|$xpValue|$isCompleted"
    }

    companion object {
        fun deserialize(str: String): TimetableBlock? {
            val parts = str.split("|")
            if (parts.size < 5) return null
            return TimetableBlock(
                id = parts[0],
                label = parts[1],
                timeRange = parts[2],
                xpValue = parts[3].toIntOrNull() ?: 20,
                isCompleted = parts[4].toBoolean()
            )
        }
    }
}

data class ErrorLogItem(
    val id: String,
    val topic: String,
    val subject: String,
    val reason: String,
    val isSolved: Boolean = false
) {
    fun serialize(): String {
        return "$id|$topic|$subject|$reason|$isSolved"
    }

    companion object {
        fun deserialize(str: String): ErrorLogItem? {
            val parts = str.split("|")
            if (parts.size < 5) return null
            return ErrorLogItem(
                id = parts[0],
                topic = parts[1],
                subject = parts[2],
                reason = parts[3],
                isSolved = parts[4].toBoolean()
            )
        }
    }
}

data class Flashcard(
    val question: String,
    val answer: String,
    val isMastered: Boolean = false
) {
    fun serialize(): String {
        return "$question^$answer^$isMastered"
    }

    companion object {
        fun deserialize(str: String): Flashcard? {
            val parts = str.split("^")
            if (parts.size < 3) return null
            return Flashcard(
                question = parts[0],
                answer = parts[1],
                isMastered = parts[2].toBoolean()
            )
        }
    }
}

data class BlockerQuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
) {
    fun serialize(): String {
        val optsJoined = options.joinToString("|")
        return "$question^$optsJoined^$correctIndex"
    }

    companion object {
        fun deserialize(str: String): BlockerQuizQuestion? {
            val parts = str.split("^")
            if (parts.size < 3) return null
            return BlockerQuizQuestion(
                question = parts[0],
                options = parts[1].split("|"),
                correctIndex = parts[2].toIntOrNull() ?: 0
            )
        }
    }
}
