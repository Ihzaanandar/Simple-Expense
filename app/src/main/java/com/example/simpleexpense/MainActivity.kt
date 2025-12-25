package com.example.simpleexpense

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.simpleexpense.databinding.ActivityMainBinding
import com.example.simpleexpense.databinding.DialogSummaryBinding
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MonthYearAdapter
    // FIX: Use the correct, consistent database class name
    private val db by lazy { ExpenseDatabase.getInstance(this).expenseDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        binding.fabAddMonth.setOnClickListener {
            showAddMonthDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        loadMonthYears()
    }

    private fun setupRecyclerView() {
        adapter = MonthYearAdapter(emptyList()) { monthYear ->
            showMonthOptionsDialog(monthYear)
        }
        binding.rvMonthYears.layoutManager = LinearLayoutManager(this)
        binding.rvMonthYears.adapter = adapter
    }

    private fun loadMonthYears() {
        lifecycleScope.launch {
            val monthYears = db.getAllMonthYears()
            adapter.updateData(monthYears)
            binding.emptyView.visibility = if (monthYears.isEmpty()) View.VISIBLE else View.GONE
            binding.rvMonthYears.visibility = if (monthYears.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun showAddMonthDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_month, null)
        val spinnerMonth = dialogView.findViewById<Spinner>(R.id.spinnerMonth)
        val spinnerYear = dialogView.findViewById<Spinner>(R.id.spinnerYear)

        val months = arrayOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, months)
        spinnerMonth.adapter = monthAdapter

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 5..currentYear + 5).map { it.toString() }
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, years)
        spinnerYear.adapter = yearAdapter
        spinnerYear.setSelection(years.indexOf(currentYear.toString()))
        spinnerMonth.setSelection(Calendar.getInstance().get(Calendar.MONTH))

        AlertDialog.Builder(this)
            .setTitle("Tambah Bulan Baru")
            .setView(dialogView)
            .setPositiveButton("Tambah") { _, _ ->
                val month = spinnerMonth.selectedItemPosition + 1
                val year = spinnerYear.selectedItem.toString().toInt()
                val key = "$year-${month.toString().padStart(2, '0')}"
                val newMonthYear = MonthYear(key, month, year)

                lifecycleScope.launch {
                    db.addMonthYear(newMonthYear)
                    loadMonthYears()
                    openExpenseDetail(newMonthYear)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showMonthOptionsDialog(monthYear: MonthYear) {
        val options = arrayOf("Lihat & Tambah Pengeluaran", "Lihat Rangkuman", "Hapus Bulan Ini")
        AlertDialog.Builder(this)
            .setTitle(monthYear.getDisplayName())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openExpenseDetail(monthYear)
                    1 -> showSummary(monthYear)
                    2 -> confirmDeleteMonth(monthYear)
                }
            }
            .show()
    }

    private fun openExpenseDetail(monthYear: MonthYear) {
        val intent = Intent(this, ExpenseDetailActivity::class.java)
        intent.putExtra("MONTH_YEAR_KEY", monthYear.key)
        startActivity(intent)
    }

    private fun showSummary(monthYear: MonthYear) {
        lifecycleScope.launch {
            val totalExpense = db.getTotalExpenseForMonth(monthYear.key) ?: 0.0
            val expenseCount = db.getExpenseCountForMonth(monthYear.key)
            val paymentMethodSummaries = db.getExpenseByPaymentMethod(monthYear.key)

            val prefs = getSharedPreferences("BudgetPrefs", Context.MODE_PRIVATE)
            val initialAmount = prefs.getFloat("initial_amount_${monthYear.key}", 0f).toDouble()
            val remainingAmount = initialAmount - totalExpense

            val dialogBinding = DialogSummaryBinding.inflate(layoutInflater)

            dialogBinding.tvInitialAmount.text = "Rp ${String.format("%,.0f", initialAmount)}"
            dialogBinding.tvTotalExpense.text = "- Rp ${String.format("%,.0f", totalExpense)}"
            dialogBinding.tvRemainingAmount.text = "Rp ${String.format("%,.0f", remainingAmount)}"
            dialogBinding.tvTransactionCount.text = expenseCount.toString()

            val remainingColor = when {
                remainingAmount < 0 -> ContextCompat.getColor(this@MainActivity, R.color.negative_amount)
                else -> ContextCompat.getColor(this@MainActivity, R.color.positive_amount)
            }
            dialogBinding.tvRemainingAmount.setTextColor(remainingColor)

            if (paymentMethodSummaries.isNotEmpty()) {
                dialogBinding.containerPaymentMethods.visibility = View.VISIBLE
                val inflater = LayoutInflater.from(this@MainActivity)
                paymentMethodSummaries.forEach { summary ->
                    val row = inflater.inflate(R.layout.summary_item_row, dialogBinding.containerPaymentMethods, false) as LinearLayout
                    val tvMethod = row.findViewById<TextView>(R.id.tvMethod)
                    val tvAmount = row.findViewById<TextView>(R.id.tvAmount)
                    tvMethod.text = "• ${summary.paymentMethod}"
                    tvAmount.text = "Rp ${String.format("%,.0f", summary.total)}"
                    dialogBinding.containerPaymentMethods.addView(row)
                }
            } else {
                dialogBinding.containerPaymentMethods.visibility = View.GONE
            }

            AlertDialog.Builder(this@MainActivity)
                .setTitle("Rangkuman ${monthYear.getDisplayName()}")
                .setView(dialogBinding.root)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun confirmDeleteMonth(monthYear: MonthYear) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Bulan")
            .setMessage("Yakin ingin menghapus semua data ${monthYear.getDisplayName()}?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch {
                    db.deleteExpensesForMonth(monthYear.key)
                    db.deleteMonthYear(monthYear.key)
                    val prefs = getSharedPreferences("BudgetPrefs", Context.MODE_PRIVATE)
                    prefs.edit().remove("initial_amount_${monthYear.key}").apply()
                    loadMonthYears()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
