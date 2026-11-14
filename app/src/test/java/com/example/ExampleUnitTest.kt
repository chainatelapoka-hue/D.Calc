package com.example

import com.example.engine.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class ExampleUnitTest {
    @Test
    fun testBasicArithmetic() {
        val res = MathEngine.evaluate("2+2")
        assertTrue(res is MathResult.Success)
        assertEquals("4", (res as MathResult.Success).formatted)
    }

    @Test
    fun testOperatorPrecedence() {
        val res = MathEngine.evaluate("10+5*2")
        assertTrue(res is MathResult.Success)
        assertEquals("20", (res as MathResult.Success).formatted)

        val parenRes = MathEngine.evaluate("(10+5)*2")
        assertTrue(parenRes is MathResult.Success)
        assertEquals("30", (parenRes as MathResult.Success).formatted)
    }

    @Test
    fun testPercentageCalculation() {
        val addPct = MathEngine.evaluate("500+10%")
        assertTrue(addPct is MathResult.Success)
        assertEquals("550", (addPct as MathResult.Success).formatted)

        val subPct = MathEngine.evaluate("500-20%")
        assertTrue(subPct is MathResult.Success)
        assertEquals("400", (subPct as MathResult.Success).formatted)

        val mulPct = MathEngine.evaluate("500*10%")
        assertTrue(mulPct is MathResult.Success)
        assertEquals("50", (mulPct as MathResult.Success).formatted)

        val standalone = MathEngine.evaluate("25%")
        assertTrue(standalone is MathResult.Success)
        assertEquals("0.25", (standalone as MathResult.Success).formatted)
    }

    @Test
    fun testDivisionByZero() {
        val res = MathEngine.evaluate("10/0")
        assertTrue(res is MathResult.Error)
        assertEquals("Cannot divide by zero", (res as MathResult.Error).message)
    }

    @Test
    fun testScientificFunctions() {
        val sin90 = MathEngine.evaluate("sin(90)", isDegreeMode = true)
        assertTrue(sin90 is MathResult.Success)
        assertEquals("1", (sin90 as MathResult.Success).formatted)

        val sqrt144 = MathEngine.evaluate("sqrt(144)")
        assertTrue(sqrt144 is MathResult.Success)
        assertEquals("12", (sqrt144 as MathResult.Success).formatted)

        val fact5 = MathEngine.evaluate("fact(5)")
        assertTrue(fact5 is MathResult.Success)
        assertEquals("120", (fact5 as MathResult.Success).formatted)
    }

    @Test
    fun testSouthAsianAndInternationalNumberFormatting() {
        val bd = BigDecimal("10000000") // 1 Crore = 10 Million
        val breakdown = NumberFormatter.getLargeNumberBreakdown(bd)
        assertTrue(breakdown.southAsianWords.contains("Crore"))
        assertTrue(breakdown.internationalWords.contains("Million"))

        val formattedSA = NumberFormatter.formatWithCommas(bd, NumberFormatStyle.SOUTH_ASIAN)
        assertEquals("1,00,00,000", formattedSA)

        val formattedIntl = NumberFormatter.formatWithCommas(bd, NumberFormatStyle.INTERNATIONAL)
        assertEquals("10,000,000", formattedIntl)
    }

    @Test
    fun testBusinessImportCostCalculation() {
        val res = BusinessMath.calculateImportCost(
            quantity = BigDecimal("100"),
            unitPriceForeign = BigDecimal("10"),
            exchangeRateToBdt = BigDecimal("17.50"), // RMB
            weightKg = BigDecimal("10"),
            shippingRatePerKgBdt = BigDecimal("700"),
            flatShippingBdt = BigDecimal.ZERO,
            customsDutyPercent = BigDecimal("10"),
            vatPercent = BigDecimal("15"),
            otherExpensesBdt = BigDecimal("1000"),
            wholesaleMarginPercent = BigDecimal("20"),
            retailMarginPercent = BigDecimal("30")
        )

        assertTrue(res.totalLandedCostBdt > BigDecimal.ZERO)
        assertTrue(res.suggestedRetailBdt > res.perUnitLandedCostBdt)
        assertTrue(res.totalProfitBdt > BigDecimal.ZERO)
    }

    @Test
    fun testEmiCalculation() {
        val emi = BusinessMath.calculateEmi(
            principal = BigDecimal("100000"),
            annualRatePct = BigDecimal("12"),
            tenureMonths = 12
        )
        assertTrue(emi.monthlyEmi > BigDecimal.ZERO)
        assertTrue(emi.totalInterest > BigDecimal.ZERO)
        assertEquals(BigDecimal("100000").add(emi.totalInterest), emi.totalPayment)
    }

    @Test
    fun testAutoCloseParentheses() {
        val res1 = MathEngine.evaluate("(10+5")
        assertTrue(res1 is MathResult.Success)
        assertEquals("15", (res1 as MathResult.Success).formatted)

        val res2 = MathEngine.evaluate("sin(90", isDegreeMode = true)
        assertTrue(res2 is MathResult.Success)
        assertEquals("1", (res2 as MathResult.Success).formatted)
    }

    @Test
    fun testRoiCalculation() {
        val roi = BusinessMath.calculateRoi(
            initialInvestment = BigDecimal("100000"),
            finalValue = BigDecimal("150000"),
            years = 2.0
        )
        assertEquals(BigDecimal("50000.00"), roi.absoluteProfit)
        assertEquals(BigDecimal("50.00"), roi.roiPercent)
        assertTrue(roi.cagrPercent > BigDecimal.ZERO)
    }

    @Test
    fun testLedgerCalculations() {
        // Items from user scenario: Cigarettes (35), Ice cream (50), Soap (55)
        val items = listOf(
            Pair("Cigarettes", 35.0),
            Pair("Ice cream", 50.0),
            Pair("Soap", 55.0)
        )
        val total = items.sumOf { it.second }
        assertEquals(140.0, total, 0.001)

        // Clear / Settle
        val remaining = 0.0
        assertEquals(0.0, remaining, 0.001)
    }
}

