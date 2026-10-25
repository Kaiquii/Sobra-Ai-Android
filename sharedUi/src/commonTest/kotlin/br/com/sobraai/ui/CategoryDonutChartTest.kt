package br.com.sobraai.ui

import com.example.appfinanceiro.core.network.CategoryReportResponse
import com.example.appfinanceiro.feature.relatorios.components.categoryDonutSlices
import com.example.appfinanceiro.feature.relatorios.components.categoryDonutHitTest
import kotlin.test.*

class CategoryDonutChartTest {
    @Test fun touchIdentifiesEachClockwiseQuarterAndItsOriginalLegendIndex() {
        val slices = categoryDonutSlices((1..4).map { category(it, 25.0) })
        assertEquals(0, categoryDonutHitTest(slices, 100f, 30f, 200f))
        assertEquals(1, categoryDonutHitTest(slices, 170f, 100f, 200f))
        assertEquals(2, categoryDonutHitTest(slices, 100f, 170f, 200f))
        assertEquals(3, categoryDonutHitTest(slices, 30f, 100f, 200f))
        val withZero = categoryDonutSlices(listOf(category(1, 0.0), category(2, 20.0)))
        assertEquals(1, categoryDonutHitTest(withZero, 100f, 30f, 200f))
    }

    @Test fun holeOuterPaddingEmptyDataAndInvalidCoordinatesCannotSelectASlice() {
        val slices = categoryDonutSlices(listOf(category(1, 100.0)))
        assertNull(categoryDonutHitTest(slices, 100f, 100f, 200f))
        assertNull(categoryDonutHitTest(slices, 100f, 50f, 200f))
        assertNull(categoryDonutHitTest(slices, 100f, 5f, 200f))
        assertNull(categoryDonutHitTest(slices, 0f, 0f, 200f))
        assertNull(categoryDonutHitTest(emptyList(), 100f, 30f, 200f))
        assertNull(categoryDonutHitTest(slices, Float.NaN, 30f, 200f))
        assertNull(categoryDonutHitTest(slices, 100f, 30f, 0f))
    }

    @Test fun touchNearTheTopHandlesTheAngleWrapWithoutMixingFirstAndLastSlices() {
        val slices = categoryDonutSlices((1..4).map { category(it, 25.0) })
        assertEquals(0, categoryDonutHitTest(slices, 100.1f, 30f, 200f))
        assertEquals(3, categoryDonutHitTest(slices, 99.9f, 30f, 200f))
    }

    private fun category(id: Int, amount: Double, percent: Double = 0.0) =
        CategoryReportResponse(id, "Categoria $id", amount, percent)

    @Test fun slicesUseAmountsAndKeepTheLegendOrderWithoutChangingApiPercentages() {
        val categories = listOf(category(1, 70.0, 69.99), category(2, 20.0, 20.01), category(3, 10.0, 10.0))
        val slices = categoryDonutSlices(categories)
        assertEquals(listOf(0, 1, 2), slices.map { it.colorIndex })
        assertEquals(listOf(-90f, 162f, 234f), slices.map { it.startAngle })
        assertEquals(listOf(252f, 72f, 36f), slices.map { it.sweepAngle })
        assertEquals(69.99, categories.first().percentage)
        assertEquals(100.0, categories.sumOf { it.total_amount })
    }

    @Test fun emptyAndZeroMonthsHaveNoInventedSlices() {
        assertTrue(categoryDonutSlices(emptyList()).isEmpty())
        assertTrue(categoryDonutSlices(listOf(category(1, 0.0), category(2, 0.0))).isEmpty())
        val single = categoryDonutSlices(listOf(category(1, 0.0), category(2, 15.0))).single()
        assertEquals(1, single.colorIndex)
        assertEquals(-90f, single.startAngle)
        assertEquals(360f, single.sweepAngle)
    }

    @Test fun smallValuesAndManyCategoriesCloseTheCircleWithoutDroppingCategories() {
        val categories = (1..20).map { category(it, it / 100.0) }
        val slices = categoryDonutSlices(categories)
        assertEquals(20, slices.size)
        assertEquals(categories.indices.toList(), slices.map { it.colorIndex })
        assertEquals(360.0, slices.sumOf { it.sweepAngle.toDouble() }, 0.0001)
        assertTrue(slices.all { it.startAngle.isFinite() && it.sweepAngle.isFinite() && it.sweepAngle > 0f })
        assertEquals(270.0, (slices.last().startAngle + slices.last().sweepAngle).toDouble(), 0.0001)
    }

    @Test fun finiteAmountsDoNotOverflowWhileCalculatingTheChart() {
        val slices = categoryDonutSlices(listOf(category(1, 1e308), category(2, 1e308)))
        assertEquals(listOf(180f, 180f), slices.map { it.sweepAngle })
    }

    @Test fun negativeAdjustmentsAndInvalidAmountsAreNotMisrepresentedAsPositiveSlices() {
        for (amount in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertTrue(categoryDonutSlices(listOf(category(1, 100.0), category(2, amount))).isEmpty())
        }
    }
}
