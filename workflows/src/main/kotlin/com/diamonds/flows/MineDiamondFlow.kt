package com.diamonds.flows

import co.paralleluniverse.fibers.Suspendable
import com.diamonds.contracts.DiamondContract
import com.diamonds.states.DiamondState
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.*

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class MineDiamondFlow(val value: Amount<Currency>) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        val diamond = DiamondState(value = value, owner = ourIdentity)
        // DO NOT GET THE NOTARY THIS WAY IN PRODUCTION. SHOULD KNOW THE IDENTITY OF THE NOTARY BEFORE HAND
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val unsignedTx = TransactionBuilder(notary = notary)
                .addCommand(DiamondContract.Commands.Mine(), diamond.participants.map { ourIdentity.owningKey })
                .addOutputState(diamond)


        unsignedTx.verify(serviceHub)

        val partiallySignedTx = serviceHub.signInitialTransaction(unsignedTx)

        val flows = diamond.participants.filter { it != ourIdentity }.map {
            val party = serviceHub.identityService.requireWellKnownPartyFromAnonymous(it)
            initiateFlow(party)
        }

        // TODO: uneccessary?
        val signedTransaction = subFlow(CollectSignaturesFlow(partiallySignedTx, flows))


        return subFlow(FinalityFlow(signedTransaction, flows))
    }
}

@InitiatedBy(MineDiamondFlow::class)
class MineDiamondFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(){
        // Responder flow logic goes here.
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
                // Check that the transaction is really an issue transaction
                assert(stx.tx.outputs.single().data is DiamondState)
            }
        }

        subFlow(signedTransactionFlow)
        subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}
