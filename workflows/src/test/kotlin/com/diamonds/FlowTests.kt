package com.diamonds

import com.diamonds.flows.MineDiamondFlow
import com.diamonds.flows.MineDiamondFlowResponder
import com.diamonds.states.DiamondState
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class FlowTests {
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
        val diamond = DiamondState(owner = miner)

        val flow = MineDiamondFlow(diamond)

        val futureFlow = a.startFlow(flow)

        network.runNetwork()

        futureFlow.getOrThrow()

        val states = a.services.vaultService.queryBy(DiamondState::class.java)
        assertEquals(states.states.first().state.data, diamond)
    }

    fun `Should have transaction notarized`() {
        val miner = a.info.chooseIdentityAndCert().party
        val diamond = DiamondState(owner = miner)

        val flow = MineDiamondFlow(diamond)

        val futureFlow = a.startFlow(flow)

        network.runNetwork()

        val tx = futureFlow.getOrThrow()

        tx.verifyRequiredSignatures()
    }

    // TODO: Understand why this doesn't fail?
    fun `Should not be able to run flow as counter party?`() {
        val miner = a.info.chooseIdentityAndCert().party
        val diamond = DiamondState(owner = miner)

        val flow = MineDiamondFlow(diamond)

        val futureFlow = b.startFlow(flow)

        network.runNetwork()

        val tx = futureFlow.getOrThrow()


        val astates = a.services.vaultService.queryBy(DiamondState::class.java)
        assertEquals(astates.states.first().state.data, diamond)

        // WIll it also be in Bs vault too? even if they arent necessarily a participant?
        val bstates = a.services.vaultService.queryBy(DiamondState::class.java)
        assertEquals(bstates.states.first().state.data, diamond)

        tx.verifyRequiredSignatures()
    }
}