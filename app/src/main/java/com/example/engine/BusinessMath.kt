package com.example.engine

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

object BusinessMath {
    private val MC = java.math.MathContext(34, RoundingMode.HALF_UP)

    data class ImportCostResult(
        val productCostForeign: BigDecimal,
        val productCostBdt: BigDecimal,
        val shippingCostBdt: BigDecimal,
        val customsDutyBdt: BigDecimal,
        val vatBdt: BigDecimal,
        val otherExpensesBdt: BigDecimal,
        val totalLandedCostBdt: BigDecimal,
        val perUnitLandedCostBdt: BigDecimal,
        val suggestedWholesaleBdt: BigDecimal,
        val suggestedRetailBdt: BigDecimal,
        val totalRevenueRetailBdt: BigDecimal,
        val totalProfitBdt: BigDecimal,
        val profitMarginPercent: BigDecimal
    )

    fun calculateImportCost(
        quantity: BigDecimal,
        unitPriceForeign: BigDecimal,
        exchangeRateToBdt: BigDecimal,
        weightKg: BigDecimal,
        shippingRatePerKgBdt: BigDecimal,
        flatShippingBdt: BigDecimal,
        customsDutyPercent: BigDecimal,
        vatPercent: BigDecimal,
        otherExpensesBdt: BigDecimal,
        wholesaleMarginPercent: BigDecimal,
        retailMarginPercent: BigDecimal
    ): ImportCostResult {
        val qty = if (quantity <= BigDecimal.ZERO) BigDecimal.ONE else quantity
        val productCostForeign = qty.multiply(unitPriceForeign, MC)
        val productCostBdt = productCostForeign.multiply(exchangeRateToBdt, MC)

        val weightShipping = weightKg.multiply(shippingRatePerKgBdt, MC)
        val totalShipping = weightShipping.add(flatShippingBdt, MC)

        // Duty & VAT usually calculated on (Product Cost + Shipping) or Product Cost
        val assessableVal = productCostBdt.add(totalShipping, MC)
        val duty = assessableVal.multiply(customsDutyPercent, MC).divide(BigDecimal(100), MC)
        val vatBase = assessableVal.add(duty, MC)
        val vat = vatBase.multiply(vatPercent, MC).divide(BigDecimal(100), MC)

        val totalLanded = productCostBdt
            .add(totalShipping, MC)
            .add(duty, MC)
            .add(vat, MC)
            .add(otherExpensesBdt, MC)

        val perUnitLanded = totalLanded.divide(qty, 2, RoundingMode.HALF_UP)

        val wholesaleMultiplier = BigDecimal.ONE.add(wholesaleMarginPercent.divide(BigDecimal(100), MC), MC)
        val suggestedWholesale = perUnitLanded.multiply(wholesaleMultiplier, MC).setScale(2, RoundingMode.HALF_UP)

        val retailMultiplier = BigDecimal.ONE.add(retailMarginPercent.divide(BigDecimal(100), MC), MC)
        val suggestedRetail = suggestedWholesale.multiply(retailMultiplier, MC).setScale(2, RoundingMode.HALF_UP)

        val totalRevenueRetail = suggestedRetail.multiply(qty, MC)
        val totalProfit = totalRevenueRetail.subtract(totalLanded, MC)
        val profitMargin = if (totalRevenueRetail > BigDecimal.ZERO) {
            totalProfit.multiply(BigDecimal(100), MC).divide(totalRevenueRetail, 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return ImportCostResult(
            productCostForeign = productCostForeign.setScale(2, RoundingMode.HALF_UP),
            productCostBdt = productCostBdt.setScale(2, RoundingMode.HALF_UP),
            shippingCostBdt = totalShipping.setScale(2, RoundingMode.HALF_UP),
            customsDutyBdt = duty.setScale(2, RoundingMode.HALF_UP),
            vatBdt = vat.setScale(2, RoundingMode.HALF_UP),
            otherExpensesBdt = otherExpensesBdt.setScale(2, RoundingMode.HALF_UP),
            totalLandedCostBdt = totalLanded.setScale(2, RoundingMode.HALF_UP),
            perUnitLandedCostBdt = perUnitLanded,
            suggestedWholesaleBdt = suggestedWholesale,
            suggestedRetailBdt = suggestedRetail,
            totalRevenueRetailBdt = totalRevenueRetail.setScale(2, RoundingMode.HALF_UP),
            totalProfitBdt = totalProfit.setScale(2, RoundingMode.HALF_UP),
            profitMarginPercent = profitMargin
        )
    }

    data class ExchangeProfitResult(
        val spread: BigDecimal,
        val totalGrossProfit: BigDecimal,
        val feeDeduction: BigDecimal,
        val netProfit: BigDecimal,
        val effectiveBuyingCost: BigDecimal,
        val effectiveSellingRevenue: BigDecimal
    )

    fun calculateCurrencyExchangeProfit(
        amount: BigDecimal,
        buyingRate: BigDecimal,
        sellingRate: BigDecimal,
        transactionFeePercent: BigDecimal,
        flatFee: BigDecimal
    ): ExchangeProfitResult {
        val spread = sellingRate.subtract(buyingRate, MC)
        val grossProfit = amount.multiply(spread, MC)
        val totalSalesVal = amount.multiply(sellingRate, MC)
        val pctFee = totalSalesVal.multiply(transactionFeePercent, MC).divide(BigDecimal(100), MC)
        val totalFee = pctFee.add(flatFee, MC)
        val netProfit = grossProfit.subtract(totalFee, MC)

        return ExchangeProfitResult(
            spread = spread.setScale(4, RoundingMode.HALF_UP),
            totalGrossProfit = grossProfit.setScale(2, RoundingMode.HALF_UP),
            feeDeduction = totalFee.setScale(2, RoundingMode.HALF_UP),
            netProfit = netProfit.setScale(2, RoundingMode.HALF_UP),
            effectiveBuyingCost = amount.multiply(buyingRate, MC).setScale(2, RoundingMode.HALF_UP),
            effectiveSellingRevenue = totalSalesVal.setScale(2, RoundingMode.HALF_UP)
        )
    }

    data class EmiResult(
        val monthlyEmi: BigDecimal,
        val totalPayment: BigDecimal,
        val totalInterest: BigDecimal
    )

    fun calculateEmi(principal: BigDecimal, annualRatePct: BigDecimal, tenureMonths: Int): EmiResult {
        if (principal <= BigDecimal.ZERO || tenureMonths <= 0) {
            return EmiResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        }
        val p = principal.toDouble()
        val r = (annualRatePct.toDouble() / 12.0) / 100.0
        val n = tenureMonths.toDouble()

        if (r == 0.0) {
            val emi = principal.divide(BigDecimal(tenureMonths), 2, RoundingMode.HALF_UP)
            return EmiResult(emi, principal, BigDecimal.ZERO)
        }

        val emiDouble = (p * r * (1 + r).pow(n)) / ((1 + r).pow(n) - 1)
        val emi = BigDecimal(emiDouble).setScale(2, RoundingMode.HALF_UP)
        val totalPayment = emi.multiply(BigDecimal(tenureMonths), MC).setScale(2, RoundingMode.HALF_UP)
        val totalInterest = totalPayment.subtract(principal, MC).setScale(2, RoundingMode.HALF_UP)

        return EmiResult(emi, totalPayment, totalInterest)
    }

    data class InterestResult(
        val totalInterest: BigDecimal,
        val maturityAmount: BigDecimal
    )

    fun calculateSimpleInterest(principal: BigDecimal, annualRatePct: BigDecimal, timeYears: BigDecimal): InterestResult {
        val interest = principal.multiply(annualRatePct, MC).multiply(timeYears, MC).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        val maturity = principal.add(interest, MC).setScale(2, RoundingMode.HALF_UP)
        return InterestResult(interest, maturity)
    }

    fun calculateCompoundInterest(
        principal: BigDecimal,
        annualRatePct: BigDecimal,
        timeYears: Double,
        compoundTimesPerYear: Int = 12
    ): InterestResult {
        val p = principal.toDouble()
        val r = annualRatePct.toDouble() / 100.0
        val n = compoundTimesPerYear.toDouble()
        val t = timeYears

        val amountDouble = p * (1 + r / n).pow(n * t)
        val maturity = BigDecimal(amountDouble).setScale(2, RoundingMode.HALF_UP)
        val interest = maturity.subtract(principal, MC).setScale(2, RoundingMode.HALF_UP)
        return InterestResult(interest, maturity)
    }

    data class RoiResult(
        val absoluteProfit: BigDecimal,
        val roiPercent: BigDecimal,
        val cagrPercent: BigDecimal
    )

    fun calculateRoi(initialInvestment: BigDecimal, finalValue: BigDecimal, years: Double): RoiResult {
        val profit = finalValue.subtract(initialInvestment, MC)
        val roiPct = if (initialInvestment > BigDecimal.ZERO) {
            profit.multiply(BigDecimal(100), MC).divide(initialInvestment, 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val cagrPct = if (years > 0 && initialInvestment > BigDecimal.ZERO && finalValue > BigDecimal.ZERO) {
            val ratio = finalValue.toDouble() / initialInvestment.toDouble()
            val cagrDouble = (ratio.pow(1.0 / years) - 1.0) * 100.0
            BigDecimal(cagrDouble).setScale(2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return RoiResult(profit.setScale(2, RoundingMode.HALF_UP), roiPct, cagrPct)
    }
}
