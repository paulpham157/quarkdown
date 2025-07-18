package com.quarkdown.stdlib

import com.quarkdown.core.function.library.loader.Module
import com.quarkdown.core.function.library.loader.moduleOf
import com.quarkdown.core.function.reflect.annotation.LikelyChained
import com.quarkdown.core.function.reflect.annotation.Name
import com.quarkdown.core.function.value.DynamicValue
import com.quarkdown.core.function.value.GeneralCollectionValue
import com.quarkdown.core.function.value.IterableValue
import com.quarkdown.core.function.value.NumberValue
import com.quarkdown.core.function.value.OutputValue
import com.quarkdown.core.function.value.PairValue
import com.quarkdown.core.function.value.Value
import com.quarkdown.core.function.value.data.Lambda
import com.quarkdown.core.function.value.wrappedAsValue
import com.quarkdown.stdlib.internal.asDouble

/**
 * Index of the first element in a collection.
 */
internal const val INDEX_STARTS_AT = 1

/**
 * `Collection` stdlib module exporter.
 * This module handles iterable collections.
 */
val Collection: Module =
    moduleOf(
        ::collectionGet,
        ::collectionFirst,
        ::collectionSecond,
        ::collectionThird,
        ::collectionLast,
        ::collectionSize,
        ::collectionSumAll,
        ::collectionAverage,
        ::collectionDistinct,
        ::collectionSorted,
        ::collectionReverse,
        ::collectionGroup,
        ::pair,
    )

/**
 * @param index index of an element in a collection, starting at 0
 * @return the index of the element in Quarkdown (starting at 1)
 */
private fun quarkdownIndexToKotlin(index: Int) = index - INDEX_STARTS_AT

/**
 * @param index index of the element to get (starting at 0)
 * @param collection collection to get the element from
 * @param fallback value to return if the index is out of bounds. If unset, `none` is returned.
 * @return element at the given index, or [fallback] if the index is out of bounds
 */
private fun nativeCollectionGet(
    index: Int,
    collection: Iterable<OutputValue<*>>,
    fallback: OutputValue<*> = NOT_FOUND,
): OutputValue<*> = collection.toList().getOrNull(index) ?: fallback

/**
 * @param collection collection to get the element from
 * @param index index of the element to get **(starting at 1)**
 * @param fallback value to return if the index is out of bounds. If unset, `false` is returned.
 * @return element at the given index, or [NOT_FOUND] if the index is out of bounds
 */
@Name("getat")
@LikelyChained
fun collectionGet(
    @Name("from") collection: Iterable<OutputValue<*>>,
    index: Int,
    @Name("orelse") fallback: DynamicValue = DynamicValue(NOT_FOUND),
) = nativeCollectionGet(quarkdownIndexToKotlin(index), collection, fallback)

/**
 * @param collection collection to get the first element from
 * @return first element of the collection, or [NOT_FOUND] if the collection is empty
 */
@Name("first")
@LikelyChained
fun collectionFirst(
    @Name("from") collection: Iterable<OutputValue<*>>,
) = nativeCollectionGet(0, collection)

/**
 * @param collection collection to get the second element from
 * @return second element of the collection, or [NOT_FOUND] if the collection has less than 2 elements
 */
@Name("second")
@LikelyChained
fun collectionSecond(
    @Name("from") collection: Iterable<OutputValue<*>>,
) = nativeCollectionGet(1, collection)

/**
 * @param collection collection to get the third element from
 * @return third element of the collection, or [NOT_FOUND] if the collection has less than 3 elements
 */
@Name("third")
@LikelyChained
fun collectionThird(
    @Name("from") collection: Iterable<OutputValue<*>>,
) = nativeCollectionGet(2, collection)

/**
 * @param collection collection to get the last element from
 * @return last element of the collection, or [NOT_FOUND] if the collection is empty
 */
@Name("last")
@LikelyChained
fun collectionLast(
    @Name("from") collection: Iterable<OutputValue<*>>,
): OutputValue<*> = collection.toList().lastOrNull() ?: NOT_FOUND

/**
 * @param collection collection to get the size of
 * @return the non-negative size of the collection
 */
@Name("size")
@LikelyChained
fun collectionSize(
    @Name("of") collection: Iterable<OutputValue<*>>,
): NumberValue = collection.count().wrappedAsValue()

/**
 * @param collection numeric collection to sum
 * @return the sum of all elements in the collection. If an element is not numeric it is ignored.
 */
@Name("sumall")
@LikelyChained
fun collectionSumAll(
    @Name("from") collection: Iterable<OutputValue<*>>,
): NumberValue =
    collection
        .sumOf { it.asDouble() }
        .wrappedAsValue()

/**
 * @param collection numeric collection to get the average from
 * @return the average of all elements in the collection. If an element is not numeric it is ignored.
 */
@Name("average")
@LikelyChained
fun collectionAverage(
    @Name("from") collection: Iterable<OutputValue<*>>,
): NumberValue =
    collection
        .map { it.asDouble() }
        .average()
        .wrappedAsValue()

/**
 * @param collection collection to get the distinct elements from
 * @return a new collection with the same elements as the original, without duplicates
 */
@Name("distinct")
@LikelyChained
fun collectionDistinct(
    @Name("from") collection: Iterable<OutputValue<*>>,
): IterableValue<OutputValue<*>> = collection.distinct().wrappedAsValue()

/**
 * @param collection collection to sort
 * @param sorting optional sorting function. If not provided, the collection is sorted by its natural order.
 * @return a new collection with the same elements as the original, sorted
 * @throws IllegalArgumentException if the elements, or the properties supplied by [sorting], cannot be compared
 */
@Suppress("UNCHECKED_CAST")
@Name("sorted")
@LikelyChained
fun collectionSorted(
    @Name("from") collection: Iterable<OutputValue<*>>,
    @Name("by") sorting: Lambda? = null,
): IterableValue<OutputValue<*>> {
    fun toComparable(value: Value<*>): Comparable<Comparable<*>> =
        value.unwrappedValue.let {
            requireNotNull(it) as? Comparable<Comparable<*>>
                ?: throw IllegalArgumentException("Cannot sort collection of unsortable type ${it::class}")
        }

    return when {
        sorting != null ->
            collection.sortedBy {
                val selector = sorting.invokeDynamic(it)
                toComparable(selector)
            }

        else -> collection.sortedBy { toComparable(it) }
    }.wrappedAsValue()
}

/**
 * @param collection collection to reverse
 * @return a new collection with the same elements as the original, in reverse order
 */
@Name("reversed")
@LikelyChained
fun collectionReverse(
    @Name("from") collection: Iterable<OutputValue<*>>,
): IterableValue<OutputValue<*>> = collection.reversed().wrappedAsValue()

/**
 * Groups a collection by their value.
 * @param collection collection to group
 * @return a collection of collections, each containing the elements that are equal to each other
 */
@Name("groupvalues")
@LikelyChained
fun collectionGroup(
    @Name("from") collection: Iterable<OutputValue<*>>,
): IterableValue<IterableValue<OutputValue<*>>> =
    collection
        .asSequence()
        .groupBy { it }
        .mapValues { it.value.toList() }
        .mapValues { it.value.wrappedAsValue() }
        .values
        .let(::GeneralCollectionValue)

/**
 * Creates a new pair.
 * @param first first element of the pair
 * @param second second element of the pair
 * @return a pair of the two elements
 */
fun pair(
    first: DynamicValue,
    second: DynamicValue,
): PairValue<*, *> = PairValue(first to second)
