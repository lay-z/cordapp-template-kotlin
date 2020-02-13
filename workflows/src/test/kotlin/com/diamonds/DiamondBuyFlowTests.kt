package com.diamonds

import com.diamonds.flows.MineDiamondFlow
import com.diamonds.flows.MineDiamondFlowResponder
import com.diamonds.flows.BuyDiamondFlow
import com.diamonds.states.DiamondState
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.finance.POUNDS
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashIssueFlow
import net.corda.testing.internal.chooseIdentityAndCert
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
        val transferer = a.info.chooseIdentityAndCert()
        val transferee = b.info.chooseIdentityAndCert()
        val diamond = DiamondState(owner = transferer.party, value = 100.POUNDS)
        val mineDiamondFlow = a.startFlow(MineDiamondFlow(diamond))


//        val cashFlow = b.startFlow(IssueCashFlow(diamond.value))
        network.runNetwork()

        mineDiamondFlow.getOrThrow()
//        cashFlow.getOrThrow()

        // When
        // Now for the actual code. lol
        val buyFlow = b.startFlow(BuyDiamondFlow(diamond, transferee.party))

        network.runNetwork()

        buyFlow.getOrThrow()

        // Should have the diamond in its vault
        val page = b.services.vaultService.queryBy(DiamondState::class.java, QueryCriteria.LinearStateQueryCriteria(linearId = listOf(diamond.linearId)))
        assert(page.states.first().state.data.linearId == diamond.linearId)

        // node a should not have diamond in vault?
        val pageA = a.services.vaultService.queryBy(DiamondState::class.java, QueryCriteria.LinearStateQueryCriteria(linearId = listOf(diamond.linearId)))
        assert(pageA.states.isEmpty())
    }
}
