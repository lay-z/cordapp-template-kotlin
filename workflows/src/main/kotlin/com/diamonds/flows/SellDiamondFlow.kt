package com.diamonds.flows

import co.paralleluniverse.fibers.Suspendable
import com.diamonds.contracts.DiamondContract
import com.diamonds.states.DiamondState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class SellDiamondFlow(val diamond: DiamondState) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // DO NOT GET THE NOTARY THIS WAY IN PRODUCTION. SHOULD KNOW THE IDENTITY OF THE NOTARY BEFORE HAND
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        // Is this also an acceptable way to get legal identities?
        val thisNode = serviceHub.myInfo.legalIdentities.first()
//        val unsignedTx = TransactionBuilder(notary = notary)
////                .addCommand(DiamondContract.Commands.Mine(), diamond.participants.map { it.owningKey })
//                .addCommand(DiamondContract.Commands.Mine(), diamond.participants.map { thisNode.owningKey })
//                .addOutputState(diamond)


        return serviceHub.signInitialTransaction(
                TransactionBuilder(notary = notary, commands = mutableListOf(Command(DiamondContract.Commands.Sell(), diamond.participants.map { it.owningKey })))
        )
    }
}

@InitiatedBy(SellDiamondFlow::class)
class SellDiamondFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
    }
}
