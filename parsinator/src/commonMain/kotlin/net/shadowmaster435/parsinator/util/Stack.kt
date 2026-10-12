package net.shadowmaster435.parsinator.util




// Because kotlin native doesnt have it
open class Stack<T> : MutableList<T> {
    override val size: Int get() = holderList.size
    protected var holderList: ArrayList<T>

    open fun push(element: T) = add(element)
    open fun pop(): T {
        if (holderList.isEmpty()) throw EmptyStackException()
        else return removeAt(holderList.size - 1)
    }
    open fun peek() = holderList.last()
    constructor() {
        holderList = arrayListOf()
    }
    constructor(from: Collection<T>) : this() {
        holderList = ArrayList(from)
    }

    override fun toString() = "$holderList"
    //region boilerplate
    override fun isEmpty() = holderList.isEmpty()
    override fun contains(element: T) = holderList.contains(element)
    override fun containsAll(elements: Collection<T>) = holderList.containsAll(elements)
    override fun get(index: Int) = holderList[index]
    override fun indexOf(element: T) = holderList.indexOf(element)
    override fun lastIndexOf(element: T) = holderList.lastIndexOf(element)
    override fun add(element: T) = holderList.add(element)
    override fun remove(element: T) = holderList.remove(element)
    override fun addAll(elements: Collection<T>) = holderList.addAll(elements)
    override fun addAll(index: Int, elements: Collection<T>) = holderList.addAll(index, elements)
    override fun removeAll(elements: Collection<T>) = holderList.removeAll(holderList)
    override fun retainAll(elements: Collection<T>) = holderList.retainAll(elements)
    override fun clear() = holderList.clear()
    override fun set(index: Int, element: T) = holderList.set(index, element)
    override fun add(index: Int, element: T) = holderList.add(index, element)
    override fun removeAt(index: Int) = holderList.removeAt(index)
    override fun listIterator() = holderList.listIterator()
    override fun listIterator(index: Int) = holderList.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int) = holderList.subList(fromIndex, toIndex)
    override fun iterator() = holderList.iterator()
    //endregion
}

private class EmptyStackException: RuntimeException("Stack is empty")
