package com.aya.module.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainState,
    onIntent: (MainIntent) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("A-Y-A Module") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isModuleActive) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = if (state.isModuleActive) "Modul Aktif (API 102)" else "Modul Tidak Aktif",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onIntent(MainIntent.ToggleState(!state.isModuleActive)) }) {
                Text("Toggle State")
            }
        }
    }
}
