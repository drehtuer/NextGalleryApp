package net.drehtuer.nextgallery.core.data.index

data class IndexStats(val upserted: Int, val deleted: Int) {
    operator fun plus(other: IndexStats) = IndexStats(upserted + other.upserted, deleted + other.deleted)

    companion object {
        val EMPTY = IndexStats(upserted = 0, deleted = 0)
    }
}
