package io.github.cepeterjob.taverndroid.engine

data class CharacterInput(val name: String, val description: String = "", val greeting: String = "")

data class Character(val id: String, val content: CharacterInput)

class InputValidationException(val field: String) : IllegalArgumentException("Invalid $field")

fun normalizeCharacter(input: CharacterInput): CharacterInput {
    val name = input.name.trim()
    if (name.isBlank() || name.length > 80) throw InputValidationException("name")
    if (input.description.length > 16000) throw InputValidationException("description")
    if (input.greeting.length > 8000) throw InputValidationException("greeting")
    return input.copy(name = name)
}
