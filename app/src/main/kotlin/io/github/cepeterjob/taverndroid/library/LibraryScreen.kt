package io.github.cepeterjob.taverndroid.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LibraryScreen() {
    Column(Modifier.padding(20.dp)) {
        Text("Library", style = MaterialTheme.typography.headlineMedium)
        Text("Your library is empty.", Modifier.padding(top = 24.dp))
        Text("Imports and shared assets will arrive in a later build.")
    }
}
