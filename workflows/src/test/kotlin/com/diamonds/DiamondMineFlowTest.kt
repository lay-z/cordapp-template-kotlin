package com.diamonds

import com.diamonds.flows.MineDiamondFlow
import com.diamonds.flows.MineDiamondFlowResponder
import com.diamonds.states.DiamondState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.utilities.getOrThrow
import net.corda.finance.DOLLARS
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DiamondMineFlowTest {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
        TestCordapp.findCordapp("com.diamonds.contracts"),
        TestCordapp.findCordapp("com.diamonds.flows")
    )))
    private val a = network.createNode()
    private val b = network.createNode()

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(MineDiamondFlowResponder::class.java)
        }
    }

    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `Should be able to mine a diamond`() {
        val miner = a.info.chooseIdentityAndCert().party

        val flow = MineDiamondFlow(value=100.DOLLARS)

        val futureFlow = a.startFlow(flow)

        network.runNetwork()

        futureFlow.getOrThrow()

        val states = a.services.vaultService.queryBy(DiamondState::class.java)
        assert(states.states.isNotEmpty())
    }

    @Test
    fun `Should have transaction notarized`() {
        val flow = MineDiamondFlow(value=100.DOLLARS)

        val futureFlow = a.startFlow(flow)

        network.runNetwork()

        val tx = futureFlow.getOrThrow()

        tx.verifyRequiredSignatures()
    }

    @Test
    fun `If Node is not the owner of the stone we should get thrown an error`() {
        val flow = MineDiamondFlow(value=100.DOLLARS)

        val futureFlow = b.startFlow(flow)

        network.runNetwork()

        assertFailsWith<TransactionVerificationException> { futureFlow.getOrThrow() }
    }
}