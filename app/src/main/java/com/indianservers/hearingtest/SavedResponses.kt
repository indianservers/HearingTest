package com.indianservers.hearingtest

/** Ignore invalid or older preference entries before they reach the results UI. */
internal fun validResponses(entries: Map<String, *>): Map<String, String> =
    entries.mapNotNull { (key, raw) ->
        val parts = key.split(':')
        val ear = parts.getOrNull(0)?.toIntOrNull()
        val frequency = parts.getOrNull(1)?.toIntOrNull()
        val value = raw as? String
        val volume = value?.toFloatOrNull()
        if (parts.size == 2 && ear in 0..2 && frequency in responseFrequencies &&
            (value == "no" || (volume != null && volume.isFinite() && volume in 0f..0.5f))) {
            "$ear:$frequency" to value!!
        } else null
    }.toMap()

private val responseFrequencies = setOf(250, 500, 1000, 2000, 3000, 4000, 6000, 8000,
    10000, 12000, 14000, 15000, 16000, 17000, 17400, 18000, 19000, 20000, 21000, 22000)
