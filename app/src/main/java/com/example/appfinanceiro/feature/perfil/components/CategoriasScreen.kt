package com.example.appfinanceiro.feature.perfil.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material.icons.filled.Category
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appfinanceiro.core.data.FinanceActionsViewModel
import com.example.appfinanceiro.core.data.SessionManager
import com.example.appfinanceiro.core.data.userMessageOr
import com.example.appfinanceiro.core.designsystem.theme.expenseDetailBlock
import com.example.appfinanceiro.feature.despesas.components.CustomInput
import com.example.appfinanceiro.core.designsystem.components.AppDataErrorBanner
import com.example.appfinanceiro.core.designsystem.components.AppLoadingIndicator
import com.example.appfinanceiro.core.designsystem.components.dataRequestErrorMessage
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.network.Category
import com.example.appfinanceiro.core.network.CategoryRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(
    onNavigateBack: () -> Unit,
    actionsViewModel: FinanceActionsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userToken by remember { SessionManager(context) }.token.collectAsState(initial = null)

    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasLoadedOnce by remember { mutableStateOf(false) }
    var loadErrorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    var newCategoryName by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var editingCategoryName by remember { mutableStateOf("") }

    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var deleteErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userToken, refreshTrigger) {
        if (userToken != null) {
            isLoading = true
            try {
                val response = actionsViewModel.getCategories(userToken!!)
                categories = response.categories
                loadErrorMessage = null
            } catch (e: Exception) {
                loadErrorMessage = e.userMessageOr("Erro ao carregar categorias")
            } finally {
                isLoading = false
                hasLoadedOnce = true
            }
        }
    }

    if (editingCategory != null) {
        val dialogBackgroundColor = MaterialTheme.colorScheme.background
        val dialogTextColor = MaterialTheme.colorScheme.onBackground
        val dialogSecondaryTextColor = dialogTextColor.copy(alpha = 0.8f)

        AlertDialog(
            onDismissRequest = { editingCategory = null },
            containerColor = dialogBackgroundColor,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Editar categoria",
                    color = dialogTextColor,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = editingCategoryName,
                    onValueChange = { editingCategoryName = it },
                    label = {
                        Text(
                            "Nome da categoria",
                            color = dialogSecondaryTextColor
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = dialogTextColor,
                        unfocusedTextColor = dialogTextColor,
                        focusedLabelColor = dialogSecondaryTextColor,
                        unfocusedLabelColor = dialogSecondaryTextColor,
                        focusedBorderColor = dialogSecondaryTextColor,
                        unfocusedBorderColor = dialogSecondaryTextColor
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editingCategoryName.isBlank()) return@TextButton
                        coroutineScope.launch {
                            try {
                                actionsViewModel.updateCategory(
                                    userToken ?: return@launch,
                                    editingCategory!!.id,
                                    CategoryRequest(editingCategoryName.trim())
                                )
                                Toast.makeText(context, "Categoria atualizada!", Toast.LENGTH_SHORT).show()
                                editingCategory = null
                                editingCategoryName = ""
                                refreshTrigger++
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erro ao atualizar categoria", Toast.LENGTH_SHORT).show()
                            }
                            }
                    }
                ) {
                    Text("Salvar", color = PrimaryBlue, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("Cancelar", color = PrimaryBlue, fontWeight = FontWeight.Medium)
                }
            }
        )
    }


    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Excluir categoria",
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Tem certeza que deseja remover essa categoria?",
                    color = textColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedCategory = categoryToDelete ?: return@TextButton

                        coroutineScope.launch {
                            try {
                                actionsViewModel.deleteCategory(
                                    userToken ?: return@launch,
                                    selectedCategory.id
                                )
                                Toast.makeText(context, "Categoria removida!", Toast.LENGTH_SHORT).show()
                                categoryToDelete = null
                                refreshTrigger++
                            } catch (e: Exception) {
                                deleteErrorMessage = e.userMessageOr("Erro ao remover categoria")
                                categoryToDelete = null
                            }
                            }
                    }
                ) {
                    Text("Sim", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancelar", color = PrimaryBlue, fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    if (deleteErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { deleteErrorMessage = null },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "Não foi possível excluir",
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = deleteErrorMessage ?: "",
                    color = textColor
                )
            },
            confirmButton = {
                TextButton(onClick = { deleteErrorMessage = null }) {
                    Text("OK", color = PrimaryBlue, fontWeight = FontWeight.Medium)
                }
            }
        )
    }


    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            SettingsTopBar("Categorias", onNavigateBack, isLoading && hasLoadedOnce)
        }
    ) { paddingValues ->
        if (isLoading && !hasLoadedOnce) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                AppLoadingIndicator(size = 40.dp, strokeWidth = 4.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                loadErrorMessage?.let { error ->
                    item(key = "categories_error") {
                        AppDataErrorBanner(
                            message = dataRequestErrorMessage(
                                errorMessage = error,
                                showingPreviousData = categories.isNotEmpty(),
                                dataLabel = "as categorias"
                            ),
                            isRetrying = isLoading,
                            onRetry = { refreshTrigger++ }
                        )
                    }
                }

                item {
                    SettingsCard(compact = true) {
                        Text("Nova categoria", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CustomInput(
                                value = newCategoryName,
                                onValueChange = { newCategoryName = it },
                                modifier = Modifier.weight(1f).semantics {
                                    contentDescription = "Nome da categoria"
                                },
                                icon = null,
                                placeholder = "Nome da categoria",
                                bgColor = MaterialTheme.colorScheme.expenseDetailBlock
                            )
                            FilledIconButton(
                                modifier = Modifier.size(48.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = androidx.compose.ui.graphics.Color.White
                                ),
                                enabled = newCategoryName.isNotBlank(),
                                onClick = {
                                    if (newCategoryName.isBlank()) return@FilledIconButton
                                    coroutineScope.launch {
                                        try {
                                            actionsViewModel.createCategory(
                                                userToken ?: return@launch,
                                                CategoryRequest(newCategoryName.trim())
                                            )
                                            Toast.makeText(context, "Categoria criada!", Toast.LENGTH_SHORT).show()
                                            newCategoryName = ""
                                            refreshTrigger++
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Erro ao criar categoria", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Adicionar categoria")
                            }
                        }
                    }
                }
                item {
                    Text(
                        "Suas categorias · ${categories.size}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                if (categories.isEmpty() && loadErrorMessage == null) {
                    item {
                        SettingsCard(compact = true) {
                            SettingsHeading(
                                "Nenhuma categoria por aqui",
                                "Adicione sua primeira categoria para começar a organizar as despesas.",
                                Icons.Default.Category
                            )
                        }
                    }
                }
                items(categories, key = { it.id }) { category ->
                    SettingsCard(compact = true) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SettingsIcon(Icons.Default.Category, compact = true)
                            Text(
                                text = category.name,
                                color = textColor,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    editingCategory = category
                                    editingCategoryName = category.name
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar ${category.name}", tint = PrimaryBlue)
                            }

                            IconButton(
                                onClick = {
                                    categoryToDelete = category
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir ${category.name}", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
