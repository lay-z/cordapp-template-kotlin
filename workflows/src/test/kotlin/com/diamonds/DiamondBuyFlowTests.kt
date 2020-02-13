package com.diamonds

import com.diamonds.flows.MineDiamondFlow
import com.diamonds.flows.MineDiamondFlowResponder
import com.diamonds.flows.BuyDiamondFlow
import com.diamonds.states.DiamondState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.withoutIssuer
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.finance.POUNDS
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashIssueFlow
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

class IssueCashFlow(val amount: Amount<Currency>): FlowLogic<Cash.State>() {
    override fun call(): Cash.State {
        val cashFlow = CashIssueFlow(amount, OpaqueBytes.of(0), serviceHub.networkMapCache.notaryIdentities.first())
        val cashIssueTransaction = subFlow(cashFlow)
        return cashIssueTransaction.stx.tx.outputs.single().data as Cash.State
    }
}
class DiamondBuyFlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.diamonds.contracts"),
            TestCordapp.findCordapp("com.diamonds.flows"),
            TestCordapp.findCordapp("net.corda.finance.contracts.asset")
    ),
            notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")
    ))))
    private val a = network.createNode(MockNodeParameters())
    private val b = network.createNode(MockNodeParameters())

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
    fun `Should be able to Purchase Diamond`() {
        // Given
        val mineDiamondFlow = a.startFlow(MineDiamondFlow(100.POUNDS))


        network.runNetwork()

        val signedTx = mineDiamondFlow.getOrThrow()

        val diamond = signedTx.tx.outputsOfType<DiamondState>().single()

        val cashFlow = b.startFlow(IssueCashFlow(diamond.value))

        network.runNetwork()

        cashFlow.getOrThrow()
        // When
        // Now for the actual code. lol
        val buyFlow = b.startFlow(BuyDiamondFlow(diamond))

        network.runNetwork()

        buyFlow.getOrThrow()

        // Should have the diamond in its vault
        val page = b.services.vaultService.queryBy(DiamondState::class.java)
        assert(page.states.first().state.data.linearId == diamond.linearId)

        val cashPageB = b.services.vaultService.queryBy(Cash.State::class.java)
        assert(cashPageB.states.isEmpty())

        // node a should not have diamond in vault?
        val pageA = a.services.vaultService.queryBy(DiamondState::class.java)
        assert(pageA.states.isEmpty())


        // Node a should now have some cash
        val cashPage = a.services.vaultService.queryBy(Cash.State::class.java)
        assert(cashPage.states.first().state.data.amount.withoutIssuer() == diamond.value)
    }
}
