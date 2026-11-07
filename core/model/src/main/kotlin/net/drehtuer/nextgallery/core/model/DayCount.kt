package net.drehtuer.nextgallery.core.model

import java.time.LocalDate

/** The number of images taken on a local date (D7). */
data class DayCount(val date: LocalDate, val count: Int)
