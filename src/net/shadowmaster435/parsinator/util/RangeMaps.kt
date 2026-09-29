package net.shadowmaster435.parsinator.util


class IntRangeMap<V> : RangeMap<Int, V>() {
    override fun snap(minValue: Int, maxValue: Int, key: Int): Int {
        return when(snapType) {
            SnapType.ROUND -> if (key < minValue + ((maxValue - minValue) * 0.5)) minValue else maxValue
            SnapType.ROUND_DOWN -> if (key < maxValue) minValue else maxValue
            SnapType.ROUND_UP -> if (key > minValue) maxValue else minValue
        }
    }
    companion object {
        operator fun <T> invoke(snapType: SnapType = SnapType.ROUND_DOWN, init: IntRangeMap<T>.() -> Unit): IntRangeMap<T> {
            val map = IntRangeMap<T>()
            init(map)
            map.snapType = snapType
            return map
        }
    }
}
class LongRangeMap<V> : RangeMap<Long, V>() {
    override fun snap(minValue: Long, maxValue: Long, key: Long): Long {
        return when(snapType) {
            SnapType.ROUND -> if (key < minValue + ((maxValue - minValue) * 0.5)) minValue else maxValue
            SnapType.ROUND_DOWN -> if (key < maxValue) minValue else maxValue
            SnapType.ROUND_UP -> if (key > minValue) maxValue else minValue
        }
    }
    companion object {
        operator fun <T> invoke(snapType: SnapType = SnapType.ROUND_DOWN, init: LongRangeMap<T>.() -> Unit): LongRangeMap<T> {
            val map = LongRangeMap<T>()
            init(map)
            map.snapType = snapType
            return map
        }
    }
}
class FloatRangeMap<V> : RangeMap<Float, V>() {
    override fun snap(minValue: Float, maxValue: Float, key: Float): Float {
        return when(snapType) {
            SnapType.ROUND -> if (key < minValue + ((maxValue - minValue) * 0.5f)) minValue else maxValue
            SnapType.ROUND_DOWN -> if (key < maxValue) minValue else maxValue
            SnapType.ROUND_UP -> if (key > minValue) maxValue else minValue
        }
    }
    companion object {
        operator fun <T> invoke(snapType: SnapType = SnapType.ROUND_DOWN, init: FloatRangeMap<T>.() -> Unit): FloatRangeMap<T> {
            val map = FloatRangeMap<T>()
            init(map)
            map.snapType = snapType
            return map
        }
    }
}
class DoubleRangeMap<V> : RangeMap<Double, V>() {
    override fun snap(minValue: Double, maxValue: Double, key: Double): Double {
        return when(snapType) {
            SnapType.ROUND -> if (key < minValue + ((maxValue - minValue) * 0.5)) minValue else maxValue
            SnapType.ROUND_DOWN -> if (key < maxValue) minValue else maxValue
            SnapType.ROUND_UP -> if (key > minValue) maxValue else minValue
        }
    }
    companion object {
        operator fun <T> invoke(snapType: SnapType = SnapType.ROUND_DOWN, init: DoubleRangeMap<T>.() -> Unit): DoubleRangeMap<T> {
            val map = DoubleRangeMap<T>()
            init(map)
            map.snapType = snapType
            return map
        }
    }
}

/**
 * allows indexing via a single value based the range between entries with different rounding options for indexing
 * behavior using [ints][IntRangeMap]
 * ```kotlin
 * val map = IntRangeMap<String>()
 * // any indexes greater than the max range value will return the last value and vice versa
 * map[0] = "a"
 * map[4] // will always return "a" at this point in execution as with any other values
 * map[4] = "b"
 * map[5] // will always return "b"
 * map[-1] // will always return "a"
 * map[3] // on round down it will give "a" on round_up and round "b"
 * map[8] = "c"
 * map[7] // on round down it will give "b" on round_up and round "c"
 * map[4] // will always give "b" due to being the exact index
 * map[2] // on round down it will give "a" on round_up and round "b"
 */

abstract class RangeMap<K: Comparable<K>, V>: MutableMap<K, V> {
    private val internalEntries = mutableListOf<MutableMap.MutableEntry<K, V>>()
    private var minIndex: K? = null
    private var maxIndex: K? = null
    @JvmField var snapType: SnapType = SnapType.ROUND_DOWN

    override val size
        get() = internalEntries.size
    override val keys: LinkedHashSet<K> get() {
        val set = linkedSetOf<K>()
        internalEntries.forEach { (k, v) ->
            set.add(k)
        }
        return set
    }

    override val values: LinkedHashSet<V> get() {
        val set = linkedSetOf<V>()
        internalEntries.forEach { (k, v) ->
            set.add(v)
        }
        return set
    }
    override val entries get() = LinkedHashSet(internalEntries)
    override fun isEmpty() = internalEntries.isEmpty()

    abstract fun snap(minValue: K, maxValue: K, key: K): K

    override fun containsKey(key: K): Boolean {
        if (minIndex == null || maxIndex == null) return false
        val mi = minIndex ?: throw ConcurrentModificationException()
        val mx = maxIndex ?: throw ConcurrentModificationException()
        if (key.compareTo(mi) == 0 || key.compareTo(mx) == 0) return true
        internalEntries.forEachIndexed { index, entry ->
            if (index + 1 < size) {
                val next = internalEntries[index + 1]
                if (key >= entry.key && key <= next.key) return true
            }
        }
        return false
    }

    override fun containsValue(value: V) = internalEntries.find { (_, v) -> value == v } != null

    open fun put(range: ClosedRange<K>, value: V) {
        put(range.start, value)
        put(range.endInclusive, value)
    }

    override fun put(key: K, value: V): V? {
        class Entry(override val key: K, private var internalValue: V) : MutableMap.MutableEntry<K, V> {
            override val value get() = internalValue
            override fun setValue(newValue: V): V {
                val old = internalValue
                internalValue = newValue
                return old
            }
        }
        if (minIndex == null) {
            minIndex = key
            internalEntries.add(Entry(key, value))
            return null
        }
        val mi = minIndex ?: throw ConcurrentModificationException()
        if (maxIndex == null) {
            if (key < mi) {
                maxIndex = mi
                minIndex = key
                internalEntries.add(Entry(key, value))
            } else if (key > mi) {
                maxIndex = key
                internalEntries.add(Entry(key, value))
            }
            else return internalEntries.first().value
        }
        val mx = maxIndex ?: throw ConcurrentModificationException()
        if (key in mi..mx) {
            var prevIndex = -1
            internalEntries.forEachIndexed { index, entry ->
                if (size == 1) {
                    prevIndex = 0
                } else if (index + 1 < size) {
                    val next = internalEntries[index + 1]

                    val snapped = snap(entry.key, next.key, key)
                    prevIndex = index + (if (snapped.compareTo(next.key) == 0) 1 else 0)
                    return@forEachIndexed
                }
            }
            val previous = internalEntries.getOrNull(prevIndex)?.value
            internalEntries[prevIndex] = Entry(key, value)
            return previous
        }
        return null
    }

    override fun remove(key: K): V? {
        if (minIndex == null || maxIndex == null) return null
        val mi = minIndex ?: throw ConcurrentModificationException()
        val mx = maxIndex ?: throw ConcurrentModificationException()
        if (key.compareTo(mi) == 0) {
            val v = internalEntries.first().value
            internalEntries.removeFirst()
            if (internalEntries.size > 1)
                return v
        }
        if (key.compareTo(mx) == 0) return internalEntries.last().value

        internalEntries.forEachIndexed { index, entry ->
            if (index + 1 < size) {
                val next = internalEntries[index + 1]
                val mx = key.compareTo(next.key)
                if (key >= entry.key && mx < 0) {
                    val previous = internalEntries[index]
                    internalEntries.removeAt(index)
                    return previous.value
                }
                if (mx == 0) {
                    val previous = internalEntries[index + 1]
                    internalEntries.removeAt(index + 1)
                    return previous.value
                }
            }
        }
        return null
    }

    override fun toString(): String {
        if (entries.isEmpty()) return "{}"
        var str = "{"
        entries.forEachIndexed { index, it ->
            str = str + "${it.key}[${it.value}]" + if (index == internalEntries.size - 1) "}" else " - "
        }
        return str
    }
    override fun putAll(from: Map<out K, V>) {
        from.forEach { (k, v) ->
            put(k, v)
        }
    }

    override fun clear() {
        internalEntries.clear()
        minIndex = null
        maxIndex = null
    }

    override fun get(key: K): V? {
        val mi = minIndex ?: return null
        val mx = maxIndex ?: return internalEntries.first().value
        if (key <= mi) return internalEntries.first().value
        if (key >= mx) return internalEntries.last().value
        internalEntries.forEachIndexed { index, entry ->
            if (index < internalEntries.size - 1) {
                val next = internalEntries[index + 1]
                return if (next.key.compareTo(snap(entry.key, next.key, key)) == 0) next.value
                else entry.value
            }
        }
        return null
    }
    enum class SnapType {
        ROUND,
        ROUND_UP,
        ROUND_DOWN
    }
}