package com.diamonds.flows

import co.paralleluniverse.fibers.Suspendable
import com.diamonds.contracts.DiamondContract
import com.diamonds.states.DiamondState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.unwrap
import net.corda.finance.workflows.asset.CashUtils

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class BuyDiamondFlow(val diamond: DiamondState, val newOwner: Party) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {
        // DO NOT GET THE NOTARY THIS WAY IN PRODUCTION. SHOULD KNOW THE IDENTITY OF THE NOTARY BEFORE HAND
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        // Is this also an acceptable way to get legal identities?
        val thisNode = serviceHub.myInfo.legalIdentities.first()

        // First we need to get the diamond state of the diamond we want to purchase
        val diamondOwner = serviceHub.networkMapCache.getNodeByLegalIdentity(diamond.owner)
                ?: // TODO work out what would be a better exception type
                throw Exception("Could not find party in network map!")

        // I'm guessing it might not be the first??
        val ownerFlow = initiateFlow(diamondOwner.legalIdentities.first())

        // First we must send the diammondId of the stone that we want to receive
        ownerFlow.send(diamond.linearId)

        subFlow(ReceiveTransactionFlow(ownerFlow, checkSufficientSignatures = false, statesToRecord = StatesToRecord.ALL_VISIBLE))

        val diamondStateRef = subFlow(ReceiveStateAndRefFlow<DiamondState>(ownerFlow)).first()
        val newStateAndCommand = diamond.withNewOwner(newOwner)

        val unsignedTx = TransactionBuilder(notary = notary)
                .addCommand(newStateAndCommand.command, diamond.participants.map { it.owningKey } + listOf(newOwner.owningKey))
                .addOutputState(newStateAndCommand.ownableState)
                .addInputState(diamondStateRef)

        // Now we need to add in the command for transferring the cash across
//        val (withCashTx, _) =  CashUtils.generateSpend(serviceHub, unsignedTx, diamond.value, ourIdentityAndCert, diamond.owner)

        // Verify and make sure that the contract is correct
        // TODO: not sure if this is necessary!
        // withCashTx.verify(serviceHub)

        val partiallySignedTx = serviceHub.signInitialTransaction(unsignedTx)

        val signedTransactions = subFlow(CollectSignaturesFlow(partiallySignedTx, listOf(ownerFlow)))

        return subFlow(FinalityFlow(signedTransactions, ownerFlow))
    }
}

@InitiatedBy(BuyDiamondFlow::class)
class SellDiamondFlowResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
        // TODO: should do some evaluation to make sure that the data type is what we expect
        val diamondId = counterpartySession.receive<UniqueIdentifier>().unwrap { it }
        val page = serviceHub.vaultService.queryBy(DiamondState::class.java, QueryCriteria.LinearStateQueryCriteria(linearId = listOf(diamondId)))
        val diamondStateRef = page.states.first()

        val diamondTransactionState = serviceHub.validatedTransactions.getTransaction(diamondStateRef.ref.txhash)!!

        // Send the history of transactions so that the other party is able to verify
        subFlow(SendTransactionFlow(counterpartySession, diamondTransactionState))

        // Send the state and ref so that counterparty is able to include as inputs to transactions
        subFlow(SendStateAndRefFlow(counterpartySession, listOf(diamondStateRef)))

        // Now we need to wait to sign transaction
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) {
                // Check that the transaction is really an DiamondState transaction
                assert(stx.tx.outputs.single().data is DiamondState)
            }
        }

        subFlow(signedTransactionFlow)
        subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}
