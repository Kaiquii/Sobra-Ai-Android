package com.example.appfinanceiro.feature.despesas.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanceiro.core.data.SessionManager
import com.example.appfinanceiro.core.designsystem.components.AppDatePickerDialog
import com.example.appfinanceiro.core.designsystem.theme.PrimaryBlue
import com.example.appfinanceiro.core.designsystem.theme.TextMuted
import com.example.appfinanceiro.core.network.Category
import com.example.appfinanceiro.core.network.CategoryRequest
import com.example.appfinanceiro.core.network.ExpenseRequest
import com.example.appfinanceiro.core.network.parseApiErrorMessage
import com.example.appfinanceiro.core.network.auth.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovaDespesaScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userToken by remember { SessionManager(context) }.token.collectAsState(initial = null)

    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val inputBgColor = MaterialTheme.colorScheme.surface

    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var expandedCategory by remember { mutableStateOf(false) }

    val sources = listOf("Salário", "Adiantamento", "Renda Extra")
    var selectedSource by remember { mutableStateOf(sources[0]) }
    var expandedSource by remember { mutableStateOf(false) }

    val calendar = remember { Calendar.getInstance() }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'00:00:00XXX", Locale.getDefault())
    val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var dateText by remember { mutableStateOf(displayFormat.format(calendar.time)) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    var selectedType by remember { mutableStateOf("Única") }
    var installments by remember { mutableIntStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }

    val dialogBackgroundColor = MaterialTheme.colorScheme.background
    val dialogTextColor = MaterialTheme.colorScheme.onBackground

    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var isCreatingCategory by remember { mutableStateOf(false) }

    LaunchedEffect(userToken) {
        if (userToken != null) {
            try {
                categories = RetrofitClient.financeApi.getCategories("Bearer $userToken").categories
                if (categories.isNotEmpty()) selectedCategory = categories[0]
            } catch (e: Exception) { }
        }
    }

    if (showCreateCategoryDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isCreatingCategory) {
                    showCreateCategoryDialog = false
                    newCategoryName = ""
                }
            },
            containerColor = dialogBackgroundColor,
            titleContentColor = dialogTextColor,
            textContentColor = dialogTextColor,
            title = {
                Text(
                    text = "Criar categoria",
                    color = dialogTextColor,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Digite o nome da nova categoria.",
                        color = dialogTextColor
                    )

                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Nome da categoria") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isCreatingCategory,
                    onClick = {
                        val categoryName = newCategoryName.trim()
                        if (categoryName.isEmpty()) {
                            Toast.makeText(
                                context,
                                "Digite um nome para a categoria",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@TextButton
                        }

                        isCreatingCategory = true
                        coroutineScope.launch {
                            try {
                                val response = RetrofitClient.financeApi.createCategory(
                                    "Bearer $userToken",
                                    CategoryRequest(name = categoryName)
                                )

                                categories = categories + response.data
                                selectedCategory = response.data
                                showCreateCategoryDialog = false
                                newCategoryName = ""

                                Toast.makeText(
                                    context,
                                    "Categoria criada com sucesso!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Erro ao criar categoria",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } finally {
                                isCreatingCategory = false
                            }
                        }
                    }
                ) {
                    Text("Salvar", color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isCreatingCategory,
                    onClick = {
                        showCreateCategoryDialog = false
                        newCategoryName = ""
                    }
                ) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            state = datePickerState,
            onConfirm = { selectedDateMillis ->
                selectedDateMillis?.let { millis ->
                    val selectedCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = millis
                    }
                    calendar.set(
                        selectedCal.get(Calendar.YEAR),
                        selectedCal.get(Calendar.MONTH),
                        selectedCal.get(Calendar.DAY_OF_MONTH)
                    )
                    dateText = displayFormat.format(calendar.time)
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Nova Despesa",
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = textColor
                        )
                    }
                },
                actions = { Spacer(modifier = Modifier.width(48.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            ExpenseValueInput(amountText = amountText, onAmountChange = { amountText = it })
            Spacer(modifier = Modifier.height(32.dp))

            FormLabel("Descrição")
            CustomInput(description, { description = it }, Icons.Default.Description, "Ex: Supermercado", inputBgColor)

            CategoryDropdown(
                selectedValue = selectedCategory?.name ?: "Selecione",
                options = categories.map { it.name },
                expanded = expandedCategory,
                onExpandedChange = { shouldExpand ->
                    if (categories.isEmpty()) {
                        expandedCategory = false
                        Toast.makeText(
                            context,
                            "Não tem categoria criada, crie uma por favor no botão de +",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        expandedCategory = shouldExpand
                    }
                },
                onSelect = { index ->
                    selectedCategory = categories[index]
                    expandedCategory = false
                },
                onAddCategoryClick = {
                    showCreateCategoryDialog = true
                }
            )


            CustomDropdown(
                label = "Origem do Pagamento",
                selectedValue = selectedSource,
                options = sources,
                expanded = expandedSource,
                onExpandedChange = { expandedSource = it },
                onSelect = { index -> selectedSource = sources[index]; expandedSource = false }
            )

            FormLabel("Data de Pagamento")
            CustomInput(
                value = dateText,
                onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() || char == '/' }) dateText = it },
                icon = null,
                trailingIcon = Icons.Default.CalendarToday,
                onTrailingIconClick = { showDatePicker = true },
                placeholder = "DD/MM/AAAA",
                bgColor = inputBgColor,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            PaymentTypeSelector(
                selectedType = selectedType,
                onTypeChange = {
                    selectedType = it
                    if (it != "Parcelada") installments = 1
                }
            )

            if (selectedType == "Parcelada") {
                InstallmentCounter(
                    installments = installments,
                    onInstallmentChange = { installments = it }
                )
            }

            FormLabel("Observações")
            CustomInput(
                value = notes,
                onValueChange = {
                    if (it.length <= 500) {
                        notes = it
                    }
                },
                icon = null,
                placeholder = "Detalhes extras da despesa",
                bgColor = inputBgColor,
                singleLine = false,
                minLines = 3,
                maxLines = 5
            )
            Text(
                text = "${notes.length}/500",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )


            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (amountText.isEmpty() || description.isEmpty() || selectedCategory == null) {
                        Toast.makeText(context, "Preencha todos os campos obrigatórios!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (notes.length > 500) {
                        Toast.makeText(context, "Observações devem ter no máximo 500 caracteres.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val finalAmount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val parsedDate = try { displayFormat.parse(dateText) ?: calendar.time } catch (e: Exception) { calendar.time }
                            val request = ExpenseRequest(
                                amount = finalAmount,
                                description = description,
                                category_id = selectedCategory!!.id,
                                payment_source = selectedSource,
                                date = dateFormat.format(parsedDate),
                                type = selectedType,
                                installments = if (selectedType == "Parcelada") installments else 1,
                                notes = notes.trim()
                            )


                            RetrofitClient.financeApi.createExpense("Bearer $userToken", request)
                            Toast.makeText(context, "Despesa salva com sucesso!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        } catch (e: HttpException) {
                            val apiMessage = parseApiErrorMessage(e.response()?.errorBody()?.string())
                            Toast.makeText(context, apiMessage ?: "Erro ao salvar", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) { Toast.makeText(context, "Erro ao salvar", Toast.LENGTH_SHORT).show()
                        } finally { isLoading = false }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Salvar Despesa", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
