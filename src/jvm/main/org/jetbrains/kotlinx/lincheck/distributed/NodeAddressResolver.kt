/*
 * Lincheck
 *
 * Copyright (C) 2019 - 2021 JetBrains s.r.o.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>
 */

package org.jetbrains.kotlinx.lincheck.distributed

/**
 * Stores information about classes included in the scenario execution.
 * Maps a node id to the corresponding class and the class to a range of corresponding ids.
 */
class NodeAddressResolver<Message>(
    testClass: Class<out Node<Message>>,
    val nodesWithScenario: Int,
    private val additionalClasses: Map<Class<out Node<Message>>, Pair<Int, Boolean>>
) {
    private val nodeTypeToRange: Map<Class<out Node<Message>>, List<Int>>
    val totalNumberOfNodes = nodesWithScenario + additionalClasses.values.map { it.first }.sum()
    private val nodes = mutableListOf<Class<out Node<Message>>>()

    init {
        repeat(nodesWithScenario) { nodes.add(testClass) }
        for ((cls, p) in additionalClasses) {
            repeat(p.first) { nodes.add(cls) }
        }
        nodeTypeToRange = nodes.mapIndexed { i, cls -> cls to i }.groupBy({ it.first }, { it.second })
    }

    /**
     * Returns a list of ids for a specified class [cls].
     */
    operator fun get(cls: Class<out Node<Message>>) = nodeTypeToRange[cls]

    /**
     * Returns a class for a specified id [iNode].
     */
    operator fun get(iNode: Int) = nodes[iNode]

    /**
     * Returns if the node with a specified id [iNode] support failures. Some node class do not support
     * failures according to user configuration.
     */
    fun canFail(iNode: Int) = additionalClasses[nodes[iNode]]?.second ?: true
}