package com.diamonds.flows

import co.paralleluniverse.fibers.Suspendable
import com.diamonds.contracts.DiamondContract
import com.diamonds.states.DiamondState
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class MineDiamondFlow(val diamond: DiamondState) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // DO NOT GET THE NOTARY THIS WAY IN PRODUCTION. SHOULD KNOW THE IDENTITY OF THE NOTARY BEFORE HAND
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        // Is this also an acceptable way to get legal identities?
        val myKey = serviceHub.myInfo.legalIdentities.first()
        val utx = TransactionBuilder(notary = notary)
                .addCommand(DiamondContract.Commands.Mine(), listOf(myKey.owningKey))
                .addOutputState(diamond)

        val stx = serviceHub.signInitialTransaction(utx)

//        val flows = diamond.participants.filter { it != myKey.anonymise() }.map {
//            val party = serviceHub.identityService.requireWellKnownPartyFromAnonymous(it)
//            initiateFlow(party)
//        }

        return subFlow(FinalityFlow(stx, listOf<FlowSession>()))
    }
}

@InitiatedBy(MineDiamondFlow::class)
class MineDiamondFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
    }
}
