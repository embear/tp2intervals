package org.freekode.tp2intervals.infrastructure.platform.trainingpeaks.workout.structure

import config.mock.ObjectMapperFactory
import org.freekode.tp2intervals.domain.workout.structure.SingleStep
import org.freekode.tp2intervals.infrastructure.platform.intervalsicu.workout.ToIntervalsStructureConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpenDurationStepTest {
    private val objectMapper = ObjectMapperFactory.objectMapper()

    private val tpStructureJson = """
        {
          "primaryLengthMetric": "duration",
          "primaryIntensityMetric": "percentOfThresholdHr",
          "structure": [
            {
              "type": "step",
              "length": {"value": 1, "unit": "repetition"},
              "steps": [{
                "name": "Warmup",
                "length": {"value": 600, "unit": "second"},
                "openDuration": true,
                "targets": [{"minValue": 60, "maxValue": 70}]
              }]
            },
            {
              "type": "step",
              "length": {"value": 1, "unit": "repetition"},
              "steps": [{
                "name": "Tempo",
                "length": {"value": 1200, "unit": "second"},
                "openDuration": false,
                "targets": [{"minValue": 80, "maxValue": 90}]
              }]
            },
            {
              "type": "step",
              "length": {"value": 1, "unit": "repetition"},
              "steps": [{
                "name": "Cooldown",
                "length": {"value": 300, "unit": "second"},
                "targets": [{"minValue": 50, "maxValue": 60}]
              }]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `should keep openDuration of tp steps`() {
        val dto = objectMapper.readValue(tpStructureJson, TPWorkoutStructureDTO::class.java)

        val steps = FromTPStructureConverter.toWorkoutStructure(dto).steps.map { it as SingleStep }

        assertTrue(steps[0].openEnded)
        assertFalse(steps[1].openEnded)
        assertFalse(steps[2].openEnded) // field is missing
    }

    @Test
    fun `should write press lap only for open ended steps in intervals`() {
        val dto = objectMapper.readValue(tpStructureJson, TPWorkoutStructureDTO::class.java)
        val structure = FromTPStructureConverter.toWorkoutStructure(dto)

        val lines = ToIntervalsStructureConverter(structure).toIntervalsStructureStr().lines()

        assertTrue(lines[0].contains("Warmup") && lines[0].trim().endsWith("Press lap"), lines[0])
        assertFalse(lines[1].contains("Press lap"), lines[1])
        assertFalse(lines[2].contains("Press lap"), lines[2])
    }

    @Test
    fun `should write openDuration back to tp only when true`() {
        val dto = objectMapper.readValue(tpStructureJson, TPWorkoutStructureDTO::class.java)
        val structure = FromTPStructureConverter.toWorkoutStructure(dto)

        val json = ToTPStructureConverter.toStructureString(objectMapper, structure)
        val roundTrip = objectMapper.readValue(json, TPWorkoutStructureDTO::class.java)

        assertEquals(true, roundTrip.structure[0].steps[0].openDuration)
        assertEquals(null, roundTrip.structure[1].steps[0].openDuration)
    }
}
