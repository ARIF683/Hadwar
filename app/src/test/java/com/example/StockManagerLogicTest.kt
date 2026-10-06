package com.example

import com.example.data.model.Item
import com.example.ui.StockViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StockManagerLogicTest {

    @Test
    fun testCurrencyFormatting() {
        val formatted = StockViewModel.formatRupees(150.50)
        assertEquals("₹150.50", formatted)

        val formattedZero = StockViewModel.formatRupees(0.0)
        assertEquals("₹0.00", formattedZero)
    }

    @Test
    fun testItemValuation() {
        val item = Item(
            id = "test_1",
            o = 1,
            name = "Brass Valve 1/2",
            cost = 85.0,
            price = 120.0,
            qty = 10.0,
            low = 2.0
        )
        val stockValue = item.qty * item.cost
        assertEquals(850.0, stockValue, 0.001)

        val margin = ((item.price - item.cost) / item.cost) * 100
        assertEquals(41.176, margin, 0.01)

        val isLow = item.qty < 0 || (item.low > 0 && item.qty <= item.low)
        assertTrue(!isLow)
    }
}
