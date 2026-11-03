package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Bookmaker
import com.example.data.model.BookmakerCatalogSeeder
import com.example.data.model.BookmakerRelation
import com.example.data.model.SupportedCurrency
import com.example.ui.BetManagerUiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SimpleDropdownSelector
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CrimsonRed

@Composable
fun BookmakersCatalogScreen(
    uiState: BetManagerUiState,
    onBack: () -> Unit,
    onSaveBookmaker: (Bookmaker) -> Unit
) {
    BackHandler { onBack() }
    var searchQuery by remember { mutableStateOf("") }
    var filterCountry by remember { mutableStateOf("TODOS") }
    var filterItf by remember { mutableStateOf("TODOS") }
    var editingBookmaker by remember { mutableStateOf<Bookmaker?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }

    val filtered = uiState.bookmakers.filter { bm ->
        val matchesQuery = searchQuery.isBlank() ||
            bm.brandName.contains(searchQuery, ignoreCase = true) ||
            bm.domain.contains(searchQuery, ignoreCase = true) ||
            bm.platformProvider.contains(searchQuery, ignoreCase = true) ||
            bm.oddsProvider.contains(searchQuery, ignoreCase = true)
        val matchesCountry = filterCountry == "TODOS" || bm.country == filterCountry
        val matchesItf = filterItf == "TODOS" || bm.itfAvailability == filterItf
        matchesQuery && matchesCountry && matchesItf
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Text(
                    text = "Catálogo de Casas (${filtered.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = {
                    editingBookmaker = null
                    showFormDialog = true
                },
                modifier = Modifier.testTag("add_bookmaker_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Casa")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Agregar")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar por marca, dominio, plataforma o proveedor de cuotas") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (listOf("TODOS") + BookmakerCatalogSeeder.supportedCountries).forEach { c ->
                FilterChip(
                    selected = filterCountry == c,
                    onClick = { filterCountry = c },
                    label = { Text(c) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tenis ITF:", style = MaterialTheme.typography.labelLarge)
            listOf("TODOS", "SI", "PARCIAL", "NO", "POR_VERIFICAR").forEach { itf ->
                FilterChip(
                    selected = filterItf == itf,
                    onClick = { filterItf = itf },
                    label = { Text(itf.replace("_", " ")) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.id }) { bm ->
                val relatedLinks = uiState.relations.filter {
                    it.bookmakerNames.contains(bm.brandName, ignoreCase = true)
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingBookmaker = bm
                            showFormDialog = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${bm.brandName} (${bm.country})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Dominio: ${bm.domain} • Moneda: ${bm.primaryCurrency}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(bm.dataReliability)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tenis ITF: ${bm.itfAvailability} • Verificación: ${bm.verificationEase} • Retiro: ${bm.withdrawalEase}",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "Plataforma: ${bm.platformProvider} • Cuotas: ${bm.oddsProvider} • Operador: ${bm.operatorCompany}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (relatedLinks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Casas relacionadas: " + relatedLinks.joinToString("; ") { "${it.relationType}: ${it.bookmakerNames}" },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        BookmakerFormDialog(
            initial = editingBookmaker,
            onDismiss = { showFormDialog = false },
            onConfirm = { updated ->
                onSaveBookmaker(updated)
                showFormDialog = false
            }
        )
    }
}

@Composable
private fun BookmakerFormDialog(
    initial: Bookmaker?,
    onDismiss: () -> Unit,
    onConfirm: (Bookmaker) -> Unit
) {
    var brandName by remember { mutableStateOf(initial?.brandName ?: "") }
    var country by remember { mutableStateOf(initial?.country ?: "Perú") }
    var domain by remember { mutableStateOf(initial?.domain ?: "") }
    var operatorCompany by remember { mutableStateOf(initial?.operatorCompany ?: "Por verificar") }
    var platformProvider by remember { mutableStateOf(initial?.platformProvider ?: "Por verificar") }
    var oddsProvider by remember { mutableStateOf(initial?.oddsProvider ?: "Por verificar") }
    var primaryCurrency by remember { mutableStateOf(initial?.primaryCurrency ?: "PEN") }
    var regulatoryStatus by remember { mutableStateOf(initial?.regulatoryStatus ?: "PENDIENTE_VERIFICAR") }
    var itfAvailability by remember { mutableStateOf(initial?.itfAvailability ?: "POR_VERIFICAR") }
    var verificationEase by remember { mutableStateOf(initial?.verificationEase ?: "SIN_EVALUAR") }
    var withdrawalEase by remember { mutableStateOf(initial?.withdrawalEase ?: "SIN_EVALUAR") }
    var estimatedWithdrawalTime by remember { mutableStateOf(initial?.estimatedWithdrawalTime ?: "Por verificar") }
    var paymentMethods by remember { mutableStateOf(initial?.paymentMethods ?: "Por verificar") }
    var verificationSource by remember { mutableStateOf(initial?.verificationSource ?: "Revisión del administrador") }
    var dataReliability by remember { mutableStateOf(initial?.dataReliability ?: "USUARIO") }
    var operationalNotes by remember { mutableStateOf(initial?.operationalNotes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva Casa en Catálogo" else "Editar Ficha de Casa") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = brandName,
                    onValueChange = { brandName = it },
                    label = { Text("Nombre comercial *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("País (ej. Perú, Chile, España u otro)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Sitio web o dominio del país") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SimpleDropdownSelector(
                    label = "Moneda Principal",
                    selectedValue = primaryCurrency,
                    options = SupportedCurrency.entries.map { it.code },
                    onValueSelected = { primaryCurrency = it }
                )
                SimpleDropdownSelector(
                    label = "Disponibilidad Circuito Tenis ITF",
                    selectedValue = itfAvailability,
                    options = listOf("SI", "PARCIAL", "NO", "POR_VERIFICAR"),
                    onValueSelected = { itfAvailability = it }
                )
                SimpleDropdownSelector(
                    label = "Facilidad de Verificación (KYC)",
                    selectedValue = verificationEase,
                    options = listOf("FACIL", "MEDIA", "DIFICIL", "SIN_EVALUAR"),
                    onValueSelected = { verificationEase = it }
                )
                SimpleDropdownSelector(
                    label = "Facilidad de Retiro",
                    selectedValue = withdrawalEase,
                    options = listOf("FACIL", "MEDIA", "DIFICIL", "SIN_EVALUAR"),
                    onValueSelected = { withdrawalEase = it }
                )
                OutlinedTextField(
                    value = operatorCompany,
                    onValueChange = { operatorCompany = it },
                    label = { Text("Empresa operadora") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = platformProvider,
                    onValueChange = { platformProvider = it },
                    label = { Text("Proveedor de plataforma / sportsbook") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = oddsProvider,
                    onValueChange = { oddsProvider = it },
                    label = { Text("Proveedor de cuotas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = estimatedWithdrawalTime,
                    onValueChange = { estimatedWithdrawalTime = it },
                    label = { Text("Tiempo habitual de retiro (intervalo estimado)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = paymentMethods,
                    onValueChange = { paymentMethods = it },
                    label = { Text("Medios de depósito y retiro") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SimpleDropdownSelector(
                    label = "Nivel de Fiabilidad del Dato",
                    selectedValue = dataReliability,
                    options = listOf("VERIFICADO", "REFERENCIA_PROVISIONAL", "USUARIO"),
                    onValueSelected = { dataReliability = it }
                )
                OutlinedTextField(
                    value = verificationSource,
                    onValueChange = { verificationSource = it },
                    label = { Text("Fuente y fecha de verificación") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = operationalNotes,
                    onValueChange = { operationalNotes = it },
                    label = { Text("Observaciones operativas y mercados") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (brandName.isBlank()) return@Button
                    onConfirm(
                        (initial ?: Bookmaker()).copy(
                            brandName = brandName.trim(),
                            country = country.trim().ifBlank { "Perú" },
                            domain = domain.trim(),
                            operatorCompany = operatorCompany.trim(),
                            platformProvider = platformProvider.trim(),
                            oddsProvider = oddsProvider.trim(),
                            primaryCurrency = primaryCurrency,
                            regulatoryStatus = regulatoryStatus,
                            itfAvailability = itfAvailability,
                            verificationEase = verificationEase,
                            withdrawalEase = withdrawalEase,
                            estimatedWithdrawalTime = estimatedWithdrawalTime.trim(),
                            paymentMethods = paymentMethods.trim(),
                            verificationSource = verificationSource.trim(),
                            dataReliability = dataReliability,
                            operationalNotes = operationalNotes.trim()
                        )
                    )
                }
            ) {
                Text("Guardar Ficha")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun BookmakerRelationsScreen(
    uiState: BetManagerUiState,
    onBack: () -> Unit,
    onSaveRelation: (BookmakerRelation) -> Unit,
    onDeleteRelation: (String) -> Unit
) {
    BackHandler { onBack() }
    var showAddDialog by remember { mutableStateOf(false) }

    // Vista de agrupación por proveedores comprobados en el catálogo
    val groupedByPlatform = uiState.bookmakers
        .filter { it.platformProvider.isNotBlank() && !it.platformProvider.equals("Por verificar", ignoreCase = true) }
        .groupBy { it.platformProvider }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Text(
                    text = "Casas Relacionadas y Proveedores",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Vínculo")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Vincular")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No se infieren relaciones sin evidencia comprobable. Distingue mismo operador legal, plataforma tecnológica, sportsbook, proveedor de cuotas o relación operativa observada.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (groupedByPlatform.isNotEmpty()) {
            Text(
                text = "Agrupación por Proveedor Tecnológico Confirmado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            groupedByPlatform.forEach { (provider, list) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Proveedor: $provider", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(list.joinToString(", ") { "${it.brandName} (${it.country})" }, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (uiState.relations.isEmpty()) {
            EmptyStateCard(
                title = "Sin relaciones registradas",
                description = "Documenta manualmente vínculos entre casas gemelas indicando tipo de vínculo, países afectados, fuente/evidencia y nivel de confianza.",
                icon = Icons.Default.Hub
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.relations, key = { it.id }) { rel ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(rel.relationType.replace("_", " "), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusBadge("Confianza: ${rel.confidenceLevel}")
                                    IconButton(onClick = { onDeleteRelation(rel.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar vínculo", tint = CrimsonRed)
                                    }
                                }
                            }
                            Text("Casas asociadas: ${rel.bookmakerNames}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text("Países afectados: ${rel.affectedCountries} • Comprobación: ${rel.checkDate}", style = MaterialTheme.typography.bodyMedium)
                            if (rel.evidenceSource.isNotBlank()) {
                                Text("Fuente / Evidencia: ${rel.evidenceSource}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (rel.notes.isNotBlank()) {
                                Text("Observaciones: ${rel.notes}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var relationType by remember { mutableStateOf("MISMA_PLATAFORMA") }
        var bookmakerNames by remember { mutableStateOf("") }
        var affectedCountries by remember { mutableStateOf("Perú") }
        var evidenceSource by remember { mutableStateOf("") }
        var confidenceLevel by remember { mutableStateOf("ALTO") }
        var checkDate by remember { mutableStateOf("2026-11-03") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Registrar Relación entre Casas") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SimpleDropdownSelector(
                        label = "Tipo de Vínculo",
                        selectedValue = relationType,
                        options = listOf(
                            "MISMO_OPERADOR",
                            "MISMA_PLATAFORMA",
                            "MISMO_SPORTSBOOK",
                            "MISMO_PROVEEDOR_CUOTAS",
                            "RELACION_OPERATIVA"
                        ),
                        onValueSelected = { relationType = it }
                    )
                    OutlinedTextField(
                        value = bookmakerNames,
                        onValueChange = { bookmakerNames = it },
                        label = { Text("Casas asociadas (ej. Betsson, Betsafe) *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = affectedCountries,
                        onValueChange = { affectedCountries = it },
                        label = { Text("Países afectados") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = evidenceSource,
                        onValueChange = { evidenceSource = it },
                        label = { Text("Fuente o evidencia verificable") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    SimpleDropdownSelector(
                        label = "Nivel de Confianza",
                        selectedValue = confidenceLevel,
                        options = listOf("ALTO", "MEDIO", "BAJO", "POR_VERIFICAR"),
                        onValueSelected = { confidenceLevel = it }
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Observaciones") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bookmakerNames.isBlank()) return@Button
                        onSaveRelation(
                            BookmakerRelation(
                                relationType = relationType,
                                bookmakerIds = bookmakerNames.trim(),
                                bookmakerNames = bookmakerNames.trim(),
                                affectedCountries = affectedCountries.trim(),
                                evidenceSource = evidenceSource.trim(),
                                confidenceLevel = confidenceLevel,
                                checkDate = checkDate,
                                notes = notes.trim()
                            )
                        )
                        showAddDialog = false
                    }
                ) {
                    Text("Guardar Relación")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
