package com.example.appfinanceiro.feature.perfil.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.data.SessionManager
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.network.Category
import com.example.appfinanceiro.core.network.CategoryRequest
import com.example.appfinanceiro.core.network.auth.RetrofitClient
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userToken by remember { SessionManager(context) }.token.collectAsState(initial = null)

    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
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
                val response = RetrofitClient.financeApi.getCategories("Bearer $userToken")
                categories = response.categories
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao carregar categorias", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
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
                                RetrofitClient.financeApi.updateCategory(
                                    "Bearer $userToken",
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
                                RetrofitClient.financeApi.deleteCategory(
                                    "Bearer $userToken",
                                    selectedCategory.id
                                )
                                Toast.makeText(context, "Categoria removida!", Toast.LENGTH_SHORT).show()
                                categoryToDelete = null
                                refreshTrigger++
                            } catch (e: HttpException) {
                                val errorBody = e.response()?.errorBody()?.string()
                                val message = try {
                                    JSONObject(errorBody ?: "").optString(
                                        "error",
                                        "Erro ao remover categoria"
                                    )
                                } catch (_: Exception) {
                                    "Erro ao remover categoria"
                                }

                                deleteErrorMessage = message
                                categoryToDelete = null
                            } catch (e: Exception) {
                                deleteErrorMessage = "Erro ao remover categoria"
                                categoryToDelete = null
                            }
                        }
                    }
                ) {
                    Text("Sim", color = Color(0xFFFF7A7A), fontWeight = FontWeight.Medium)
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
            TopAppBar(
                title = { Text("Categorias", color = textColor, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Nova categoria") },
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (newCategoryName.isBlank()) return@Button
                                coroutineScope.launch {
                                    try {
                                        RetrofitClient.financeApi.createCategory(
                                            "Bearer $userToken",
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
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Adicionar")
                        }
                    }
                }

                items(categories) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceColor, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = PrimaryBlue)
                        }

                        IconButton(
                            onClick = {
                                categoryToDelete = category
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFFF6B6B))
                        }
                    }
                }
            }
        }
    }
}
