package com.example.simpleexpense

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.simpleexpense.databinding.ActivityExpenseDetailBinding
import kotlinx.coroutines.launch
import java.util.*

class ExpenseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseDetailBinding
    private lateinit var adapter: ExpenseAdapter
    // FIX: Use the correct, consistent database class name
    private val db: ExpenseDao by lazy { ExpenseDatabase.getInstance(applicationContext).expenseDao() }
    private lateinit var monthYearKey: String
    private lateinit var prefs: SharedPreferences

    private var initialAmount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        monthYearKey = intent.getStringExtra("MONTH_YEAR_KEY") ?: return // Close if key is missing

        prefs = getSharedPreferences("BudgetPrefs", Context.MODE_PRIVATE)

        // Extract month and year from key for display name
        val parts = monthYearKey.split("-")
        val year = parts.first().toInt()
        val month = parts.last().toInt()
        val monthName = Date(0).let { val cal = Calendar.getInstance(); cal.set(year, month - 1, 1); cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) }
        binding.tvTitle.text = "$monthName $year"

        setupRecyclerView()

        loadInitialAmount()
        loadExpenses()

        binding.fabAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnEditBudget.setOnClickListener {
            showEditInitialAmountDialog()
        }

        val isBudgetSet = prefs.contains(getPrefsKey())
        if (!isBudgetSet) {
            showEditInitialAmountDialog(isFirstTime = true)
        }
    }

    private fun setupRecyclerView() {
        adapter = ExpenseAdapter(
            emptyList(),
            onEditClick = { expense -> showEditExpenseDialog(expense) },
            onDeleteClick = { expense -> confirmDeleteExpense(expense) }
        )
        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = adapter
    }

    private fun loadInitialAmount() {
        initialAmount = prefs.getFloat(getPrefsKey(), 0f).toDouble()
        updateSummaryUI()
    }

    private fun saveInitialAmount(amount: Double) {
        initialAmount = amount
        prefs.edit().putFloat(getPrefsKey(), amount.toFloat()).apply()
        updateSummaryUI()
    }

    private fun getPrefsKey(): String {
        return "initial_amount_$monthYearKey"
    }

    private fun updateSummaryUI() {
        lifecycleScope.launch {
            val totalExpense = db.getTotalExpenseForMonth(monthYearKey) ?: 0.0
            val remainingAmount = initialAmount - totalExpense

            binding.tvInitialAmount.text = "Rp ${String.format("%,.0f", initialAmount)}"
            binding.tvTotalExpense.text = "- Rp ${String.format("%,.0f", totalExpense)}"
            binding.tvRemainingAmount.text = "Rp ${String.format("%,.0f", remainingAmount)}"

            val remainingColor = when {
                remainingAmount < 0 -> ContextCompat.getColor(this@ExpenseDetailActivity, R.color.negative_amount)
                else -> ContextCompat.getColor(this@ExpenseDetailActivity, R.color.positive_amount)
            }
            binding.tvRemainingAmount.setTextColor(remainingColor)
        }
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val expenses = db.getExpensesForMonth(monthYearKey)
            adapter.updateData(expenses)
            binding.emptyView.visibility = if (expenses.isEmpty()) View.VISIBLE else View.GONE
            binding.rvExpenses.visibility = if (expenses.isEmpty()) View.GONE else View.VISIBLE
            updateSummaryUI()
        }
    }

    private fun showAddExpenseDialog() {
        showAmountDialog { amount ->
            showPaymentMethodDialog { paymentMethod ->
                showDescriptionDialog { description ->
                    lifecycleScope.launch {
                        val expense = Expense(
                            id = UUID.randomUUID().toString(),
                            amount = amount,
                            paymentMethod = paymentMethod,
                            description = description,
                            monthYearKey = monthYearKey
                        )
                        db.addExpense(expense)
                        loadExpenses()
                    }
                }
            }
        }
    }

    private fun showEditExpenseDialog(expense: Expense) {
        showAmountDialog(defaultValue = expense.amount) { amount ->
            showPaymentMethodDialog(defaultValue = expense.paymentMethod) { paymentMethod ->
                showDescriptionDialog(defaultValue = expense.description) { description ->
                    lifecycleScope.launch {
                        val updatedExpense = expense.copy(
                            amount = amount,
                            paymentMethod = paymentMethod,
                            description = description,
                            timestamp = System.currentTimeMillis()
                        )
                        db.updateExpense(updatedExpense)
                        loadExpenses()
                    }
                }
            }
        }
    }

    private fun confirmDeleteExpense(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Pengeluaran")
            .setMessage("Yakin ingin menghapus pengeluaran ini?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch {
                    db.deleteExpense(expense.id)
                    loadExpenses()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // --- Dialog Functions (unchanged, but shown for completeness) ---

    private fun showAmountDialog(title: String = "Nominal Pengeluaran", defaultValue: Double? = null, onComplete: (Double) -> Unit) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Contoh: 50000"
            defaultValue?.let { setText(it.toLong().toString()) }
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Lanjut") { _, _ ->
                val amountStr = input.text.toString()
                if (amountStr.isNotEmpty()) {
                    onComplete(amountStr.toDouble())
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showPaymentMethodDialog(defaultValue: String? = null, onComplete: (String) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_method, null)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinnerPaymentMethod)

        val methods = arrayOf("Cash", "Transfer Bank", "E-Wallet", "Kartu Debit", "Kartu Kredit")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, methods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        defaultValue?.let { spinner.setSelection(methods.indexOf(it).coerceAtLeast(0)) }

        AlertDialog.Builder(this)
            .setTitle("Metode Pembayaran")
            .setView(dialogView)
            .setPositiveButton("Lanjut") { _, _ ->
                onComplete(spinner.selectedItem.toString())
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDescriptionDialog(defaultValue: String? = null, onComplete: (String) -> Unit) {
        val input = EditText(this).apply {
            hint = "Contoh: Makan siang"
            defaultValue?.let { setText(it) }
        }

        AlertDialog.Builder(this)
            .setTitle("Keterangan")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val description = input.text.toString().ifEmpty { "Tanpa keterangan" }
                onComplete(description)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showEditInitialAmountDialog(isFirstTime: Boolean = false) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Contoh: 3000000"
            if (initialAmount > 0) {
                setText(initialAmount.toLong().toString())
            }
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("Atur Uang Awal Bulan")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val amountStr = input.text.toString()
                val newAmount = if (amountStr.isNotEmpty()) amountStr.toDouble() else 0.0
                saveInitialAmount(newAmount)
            }

        if (!isFirstTime) {
            builder.setNegativeButton("Batal", null)
        } else {
            builder.setCancelable(false)
        }

        builder.show()
    }
}
