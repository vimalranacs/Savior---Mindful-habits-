package com.example.domain

data class ReflectionPrompt(
    val headline: String,
    val body: String
)

/**
 * Calm, non-judgmental reflection prompts displayed during a mindful pause.
 */
object ReflectionPrompts {
    val all = listOf(
        ReflectionPrompt(
            headline = "A little pause before you proceed.",
            body = "Remember why you chose to limit this app.\n\nConvenience is useful, but constant convenience can make unnecessary spending feel effortless.\n\nDo you actually need something right now, or is this just a habit?"
        ),
        ReflectionPrompt(
            headline = "You don't have to turn every craving into an order.",
            body = "A passing impulse often fades in a few quiet moments.\n\nCheck whether this is something you genuinely need right now."
        ),
        ReflectionPrompt(
            headline = "Could this wait until your next grocery run?",
            body = "A few minutes of convenience can sometimes become a habit of unnecessary spending.\n\nAdding it to tomorrow's list keeps you in charge."
        ),
        ReflectionPrompt(
            headline = "Pause. Check whether you actually need it.",
            body = "You set this gentle boundary for yourself.\n\nTake one slow breath before deciding what serves you best right now."
        ),
        ReflectionPrompt(
            headline = "Convenience should remain a choice.",
            body = "You are allowed to choose convenience. Just make sure it is your choice—not an automatic reflex."
        )
    )

    val unlockReasons = listOf(
        "Planned grocery order",
        "Genuine household need",
        "Checking prices only",
        "Meal for family / guests",
        "Craving — choosing consciously"
    )

    fun pickForAttempt(seed: Long): ReflectionPrompt {
        val index = ((seed / 1000L).toInt().let { kotlin.math.abs(it) }) % all.size
        return all[index]
    }
}
