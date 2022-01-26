/*
 * Lincheck
 *
 * Copyright (C) 2019 - 2022 JetBrains s.r.o.
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

package org.jetbrains.kotlinx.lincheck.test.distributed

import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.distributed.*
import org.jetbrains.kotlinx.lincheck.distributed.event.*
import org.jetbrains.kotlinx.lincheck.verifier.EpsilonVerifier
import org.junit.Test

class CrashingNode(private val env: Environment<Int, Unit>) : Node<Int, Unit> {
    private val receivedMessages = mutableSetOf<Int>()
    override fun onMessage(message: Int, sender: Int) {
        if (receivedMessages.add(message)) {
            env.send(-message, sender)
        }
    }

    @Operation
    fun operation(value: Int) {
        env.broadcast(value)
    }

    override fun validate(events: List<Event>, databases: List<Unit>) {
        data class ExpectedNotification(val from: Int, val to: Int)

        val expectedNotifications = mutableListOf<ExpectedNotification>()
        val crashes = mutableListOf<Int>()
        for (e in events) {
            when (e) {
                is NodeCrashEvent -> {
                    expectedNotifications.removeIf { it.to == e.iNode }
                    crashes.add(e.iNode)
                    (0 until env.numberOfNodes).filter { it !in crashes }
                        .forEach { expectedNotifications.add(ExpectedNotification(e.iNode, it)) }
                }
                is CrashNotificationEvent -> {
                    val index = expectedNotifications.indexOfFirst { it.from == e.crashedNode && e.iNode == it.to }
                    check(index != -1)
                    expectedNotifications.removeAt(index)
                }
                is NodeRecoveryEvent -> {
                    crashes.remove(e.iNode)
                }
                is NetworkPartitionEvent -> {
                    for (i in e.firstPart) {
                        for (j in e.secondPart) {
                            if (j !in crashes) expectedNotifications.add(ExpectedNotification(i, j))
                            if (i !in crashes) expectedNotifications.add(ExpectedNotification(j, i))
                        }
                    }
                }
                else -> continue
            }
        }
        check(expectedNotifications.isEmpty())
    }
}

class CrashNotificationTest {
    @Test
    fun test() = createDistributedOptions<Int>()
        .addNodes(CrashingNode::class.java, 2, 4, CrashMode.NO_CRASH, NetworkPartitionMode.COMPONENTS)
        .verifier(EpsilonVerifier::class.java)
        //.storeLogsForFailedScenario("logs.txt")
        .check()
}