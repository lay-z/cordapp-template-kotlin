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
        class dummyCommand: TypeOnlyCommandData()
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
                command(listOf(ALICE.publicKey), dummyCommand())
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
}