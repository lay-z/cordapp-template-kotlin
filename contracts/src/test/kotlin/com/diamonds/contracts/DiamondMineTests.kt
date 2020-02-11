package com.diamonds.contracts

import com.diamonds.states.DiamondState
import com.diamonds.states.DiamondType
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.AbstractParty
import net.corda.finance.DOLLARS
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class DiamondMineTests {
    private val ledgerServices = MockServices(listOf("com.diamonds.contracts"))
    val defaultDiamond = DiamondState(owner = ALICE.party, value = 100.DOLLARS)

    @Test
    fun shouldOnlyWorkWithTheCorrectCommandOptions() {
        class DummyCommand : TypeOnlyCommandData()
        ledgerServices.ledger {
            transaction {
                output(DiamondContract.ID, defaultDiamond)
                command(listOf(ALICE.publicKey), DiamondContract.Commands.Mine())
                this.verifies()
            }
            transaction {
                output(DiamondContract.ID, defaultDiamond)
                command(listOf(ALICE.publicKey), DummyCommand())
                this.fails()
            }
        }
    }

    @Test
    fun miningShouldOnlyHave0InputsAndOnly1Output() {
        ledgerServices.ledger {
            transaction {
                output(DiamondContract.ID, defaultDiamond)
                command(listOf(ALICE.publicKey), DiamondContract.Commands.Mine())
                this.verifies()
            }
            transaction {
                output(DiamondContract.ID, defaultDiamond)
                input(DiamondContract.ID, defaultDiamond)
                command(listOf(ALICE.publicKey), DiamondContract.Commands.Mine())
                this.fails()
            }
        }
    }

    @Test
    fun ownersHaveToSignStones() { // Not sure if its possible to check this in the contract code. :sob:
        ledgerServices.ledger {
            transaction {
                output(DiamondContract.ID, defaultDiamond)
                output(DiamondContract.ID, DiamondState(owner = BOB.party, value = 100.DOLLARS))
                command(listOf(ALICE.publicKey, BOB.publicKey), DiamondContract.Commands.Mine())
                this.verifies()
            }
            transaction {
                output(DiamondContract.ID, defaultDiamond)
                command(listOf(BOB.publicKey), DiamondContract.Commands.Mine())
                this.fails()
            }
            transaction {
                output(DiamondContract.ID, defaultDiamond)
                output(DiamondContract.ID, DiamondState(owner = BOB.party, value = 100.DOLLARS))
                command(listOf(BOB.publicKey), DiamondContract.Commands.Mine())
                this.fails()
            }
        }
    }

    @Test
    fun mustHaveAtleastOneDiamondMined() { // Not sure if its possible to check this in the contract code. :sob:
        ledgerServices.ledger {
            transaction {
                output(DiamondContract.ID, defaultDiamond)
                command(listOf(ALICE.publicKey), DiamondContract.Commands.Mine())
                this.verifies()
            }
            transaction {
                output(DiamondContract.ID, defaultDiamond)
                output(DiamondContract.ID, defaultDiamond)
                command(listOf(ALICE.publicKey), DiamondContract.Commands.Mine())
                this.verifies()
            }
            transaction {
                data class dummyContract(override val participants: List<AbstractParty> = listOf()): ContractState
                output(DiamondContract.ID, dummyContract())
                command(listOf(ALICE.publicKey), DiamondContract.Commands.Mine())
                this.fails()
            }
        }
    }

    @Test
    fun newlyMinedStonesCanOnlyBeOfTypeROUGH() {
        ledgerServices.ledger {
            transaction {
                val diamond = defaultDiamond.copy(state = DiamondType.POLISHED)
                output(DiamondContract.ID, diamond)
                command(listOf(ALICE.publicKey), DiamondContract.Commands.Mine())
                this.fails()
            }
            transaction {
                val diamond = defaultDiamond.copy(state = DiamondType.CUT)
                output(DiamondContract.ID, diamond)
                command(listOf(ALICE.publicKey), DiamondContract.Commands.Mine())
                this.fails()
            }
            transaction {
                val diamond = defaultDiamond.copy(state = DiamondType.ROUGH)
                output(DiamondContract.ID, diamond)
                command(listOf(ALICE.publicKey), DiamondContract.Commands.Mine())
                this.verifies()
            }
        }
    }
}