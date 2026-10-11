package io.github.cepeterjob.taverndroid.characters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.cepeterjob.taverndroid.engine.CharacterInput
import io.github.cepeterjob.taverndroid.engine.InputValidationException
import io.github.cepeterjob.taverndroid.engine.StoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class CharactersViewModel(private val repository: StoryRepository) : ViewModel() {
    val characters = repository.observeCharacters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val message = MutableStateFlow<String?>(null)
    val error = message
    suspend fun create(input: CharacterInput): Boolean = try {
        repository.createCharacter(input)
        message.value = null
        true
    } catch (invalid: InputValidationException) {
        message.value = when (invalid.field) {
            "name" -> "Enter a name from 1 to 80 characters."
            "description" -> "Keep the description within 16,000 characters."
            else -> "Keep the greeting within 8,000 characters."
        }
        false
    } catch (failure: Exception) {
        if (failure is CancellationException) throw failure
        message.value = "Could not save character. Try again."
        false
    }
}
