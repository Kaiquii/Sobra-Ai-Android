package br.com.sobraai.ui

import java.util.Calendar

actual fun currentPeriod(): Period = Calendar.getInstance().let { Period(it.get(Calendar.MONTH) + 1, it.get(Calendar.YEAR)) }
