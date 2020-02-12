package com.diamonds.contracts

import com.diamonds.states.DiamondState
import com.diamonds.states.DiamondType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.finance.DOLLARS
import net.corda.finance.`issued by`
import net.corda.finance.contracts.asset.Cash
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.util.*

class DiamondSellTests {
    private val ledgerServices = MockServices(listOf("com.diamonds.contracts", "net.corda.finance.contracts.asset"))
    val defaultDiamond = DiamondState(owner = ALICE.party, value = 200.DOLLARS)

    private fun createCashState(owner: TestIdentity, issuer: TestIdentity, amount: Amount<Currency>): Cash.State {

        val defaultRef = ByteArray(1) { 1 }
        return Cash.State(amount = amount `issued by` issuer.ref(defaultRef.first()), owner = owner.party)
    }

    @Test
    fun shouldOnlyWorkWithTheCorrectCommandOptions() {
        class dummyCommand : TypeOnlyCommandData()
        ledgerServices.ledger {
            transaction {
                input(DiamondContract.ID, defaultDiamond)
                input(Cash.PROGRAM_ID, createCashState(BOB, MEGACORP, defaultDiamond.value))

                output(Cash.PROGRAM_ID, createCashState(ALICE, MEGACORP, defaultDiamond.value))
                output(DiamondContract.ID, defaultDiamond.copy(owner = BOB.party))

                command(listOf(ALICE.publicKey, BOB.publicKey), DiamondContract.Commands.Sell())
                command(listOf(ALICE.publicKey, BOB.publicKey), Cash.Commands.Move())
                this.verifies()
            }
            transaction {
                input(DiamondContract.ID, defaultDiamond)
                input(Cash.PROGRAM_ID, createCashState(BOB, MEGACORP, defaultDiamond.value))

                output(DiamondContract.ID, defaultDiamond.copy(owner = BOB.party))
                output(Cash.PROGRAM_ID, createCashState(ALICE, MEGACORP, defaultDiamond.value))

                // Object declaration as a random command object that isn't part of DiamondContract.Commands
                command(listOf(ALICE.publicKey, BOB.publicKey), dummyCommand())
                command(listOf(ALICE.publicKey, BOB.publicKey), Cash.Commands.Move())
                this.fails()
            }
        }
    }

    @Test
    fun shouldOnlyChangeOwnerField() {
        ledgerServices.ledger {
            transaction {
                input(DiamondContract.ID, defaultDiamond)
                input(Cash.PROGRAM_ID, createCashState(BOB, MEGACORP, defaultDiamond.value))

                output(DiamondContract.ID, defaultDiamond.copy(state = DiamondType.CUT))
                output(Cash.PROGRAM_ID, createCashState(ALICE, MEGACORP, defaultDiamond.value))

                command(listOf(ALICE.publicKey, BOB.publicKey), DiamondContract.Commands.Sell())
                command(listOf(ALICE.publicKey, BOB.publicKey), Cash.Commands.Move())
                this.fails()
            }
        }
    }

    @Test
    fun shouldHaveTheCorrectAmountOfMoney() {
        ledgerServices.ledger {
            transaction {
                input(DiamondContract.ID, defaultDiamond)
                input(Cash.PROGRAM_ID, createCashState(BOB, MEGACORP, defaultDiamond.value - 50.DOLLARS))

                output(DiamondContract.ID, defaultDiamond.copy(owner = BOB.party))
                output(Cash.PROGRAM_ID, createCashState(ALICE, MEGACORP, defaultDiamond.value - 50.DOLLARS))

                command(listOf(ALICE.publicKey, BOB.publicKey), DiamondContract.Commands.Sell())
                command(listOf(ALICE.publicKey, BOB.publicKey), Cash.Commands.Move())
                this.fails()
            }
        }
    }

    @Test
    fun shouldHaveEqualNumbersOfInputsAndOutputs() {
        ledgerServices.ledger {
            transaction {
                input(DiamondContract.ID, defaultDiamond)
                input(Cash.PROGRAM_ID, createCashState(BOB, MEGACORP, defaultDiamond.value))

                output(DiamondContract.ID, defaultDiamond.copy(owner = BOB.party))
                output(DiamondContract.ID, defaultDiamond.copy(owner = BOB.party))
                output(Cash.PROGRAM_ID, createCashState(ALICE, MEGACORP, defaultDiamond.value))

                command(listOf(ALICE.publicKey, BOB.publicKey), DiamondContract.Commands.Sell())
                command(listOf(ALICE.publicKey, BOB.publicKey), Cash.Commands.Move())

                this `fails with` "Different number of diamonds inputs and outputs. Inputs: 1, outputs 2"
            }
            transaction {
                val bobDiamond = DiamondState(owner = BOB.party, value = 100.DOLLARS)
                input(Cash.PROGRAM_ID, createCashState(BOB, MEGACORP, defaultDiamond.value))
                input(DiamondContract.ID, defaultDiamond)
                input(DiamondContract.ID, bobDiamond)

                output(DiamondContract.ID, defaultDiamond.copy(owner = BOB.party))
                output(DiamondContract.ID, bobDiamond.copy(owner = ALICE.party))
                output(Cash.PROGRAM_ID, createCashState(ALICE, MEGACORP, defaultDiamond.value))

                command(listOf(ALICE.publicKey, BOB.publicKey), DiamondContract.Commands.Sell())
                command(listOf(ALICE.publicKey, BOB.publicKey), Cash.Commands.Move())

                this `fails with` "Incorrect number of cash states detected. Should have 2, instead have 1"
            }
        }
    }

    @Test
    fun shouldTransferCashFromTransfereeToTranferer() {
        ledgerServices.ledger {
            transaction {
                input(DiamondContract.ID, defaultDiamond)
                input(Cash.PROGRAM_ID, createCashState(ALICE, MEGACORP, defaultDiamond.value))

                output(Cash.PROGRAM_ID, createCashState(CHARLIE, MEGACORP, defaultDiamond.value))
                output(DiamondContract.ID, defaultDiamond.copy(owner = BOB.party))

                command(listOf(ALICE.publicKey, BOB.publicKey), DiamondContract.Commands.Sell())
                command(listOf(ALICE.publicKey, BOB.publicKey), Cash.Commands.Move())
                this `fails with` "Cash state is not being transferred to correct party"
            }
        }
    }

    @Test
    fun shouldHaveCorrectSignersOnTransaction() {
        ledgerServices.ledger {
            transaction {
                input(DiamondContract.ID, defaultDiamond)
                input(Cash.PROGRAM_ID, createCashState(BOB, MEGACORP, defaultDiamond.value))

                output(Cash.PROGRAM_ID, createCashState(ALICE, MEGACORP, defaultDiamond.value))
                output(DiamondContract.ID, defaultDiamond.copy(owner = BOB.party))

                command(listOf(BOB.publicKey), DiamondContract.Commands.Sell())
                command(listOf(ALICE.publicKey, BOB.publicKey), Cash.Commands.Move())
                this `fails with` "All required signers were not available on transaction"
            }
        }
    }
}