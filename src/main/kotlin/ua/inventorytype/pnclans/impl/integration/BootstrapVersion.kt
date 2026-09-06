package ua.inventorytype.pnclans.impl.integration

/** Сравнение версий до установки pnLibrary. Намеренно не зависит от классов библиотеки. */
internal object BootstrapVersion {
    fun isAtLeast(installed: String, required: String): Boolean {
        val left = parse(installed) ?: return false
        val right = parse(required) ?: return false
        for (i in 0..2) {
            val order = left.first[i].compareTo(right.first[i])
            if (order != 0) return order > 0
        }
        val a = left.second
        val b = right.second
        if (a.isEmpty() || b.isEmpty()) return a.isEmpty()
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrNull(i) ?: return false
            val y = b.getOrNull(i) ?: return true
            val xn = x.toBigIntegerOrNull()
            val yn = y.toBigIntegerOrNull()
            val order = when {
                xn != null && yn != null -> xn.compareTo(yn)
                xn != null -> -1
                yn != null -> 1
                else -> x.compareTo(y)
            }
            if (order != 0) return order > 0
        }
        return true
    }

    private fun parse(value: String): Pair<List<java.math.BigInteger>, List<String>>? {
        val match = Regex("^v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$")
            .matchEntire(value.trim()) ?: return null
        val pre = match.groupValues[4].takeIf { it.isNotEmpty() }?.split('.') ?: emptyList()
        if (pre.any { it.length > 1 && it[0] == '0' && it.all(Char::isDigit) }) return null
        return (1..3).map { match.groupValues[it].toBigInteger() } to pre
    }
}
