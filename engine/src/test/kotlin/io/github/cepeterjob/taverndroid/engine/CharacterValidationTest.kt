package io.github.cepeterjob.taverndroid.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CharacterValidationTest {
    @Test fun trimsNameAndKeepsOptionalContent() {
        assertEquals(
            CharacterInput("Ada", "  description  ", "Hi"),
            normalizeCharacter(CharacterInput("  Ada  ", "  description  ", "Hi"))
        )
    }

    @Test fun rejectsBlankNames() {
        assertThrows(InputValidationException::class.java) {
            normalizeCharacter(CharacterInput(" \n\t"))
        }
    }

    @Test fun acceptsExactFieldBoundaries() {
        val input = CharacterInput("a".repeat(80), "b".repeat(16000), "c".repeat(8000))
        assertEquals(input, normalizeCharacter(input))
    }

    @Test fun rejectsEachOverLimitField() {
        for (input in listOf(
            CharacterInput("a".repeat(81)),
            CharacterInput("Ada", "b".repeat(16001)),
            CharacterInput("Ada", greeting = "c".repeat(8001))
        )) {
            assertThrows(InputValidationException::class.java) { normalizeCharacter(input) }
        }
    }

    @Test fun nameUsesUtf16Units() {
        assertEquals(80, normalizeCharacter(CharacterInput("\uD83D\uDE00".repeat(40))).name.length)
        assertThrows(InputValidationException::class.java) {
            normalizeCharacter(CharacterInput("\uD83D\uDE00".repeat(41)))
        }
    }
}
