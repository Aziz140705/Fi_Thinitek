package com.fithnitek.config

import java.math.BigDecimal
import java.util.UUID

// Frais d'activation du compte conducteur.
// Fixés PAR LE SERVEUR : le mobile ne les envoie jamais (sinon quelqu'un pourrait payer 0 DT).
// Pour changer le montant sans toucher au code : variable d'environnement FRAIS_CONDUCTEUR_DT.
object Frais {
    val CONDUCTEUR: BigDecimal =
        (System.getenv("FRAIS_CONDUCTEUR_DT")?.toBigDecimalOrNull() ?: BigDecimal("20.00"))
            .setScale(2, java.math.RoundingMode.HALF_UP)
}

data class PaiementResult(val success: Boolean, val reference: String, val message: String? = null)

// Le reste de l'application ne connaît que cette interface.
// Plus tard : écrire KonnectPaiementService ou FlouciPaiementService qui l'implémentent,
// et remplacer SimulatedPaiementService dans ConducteurRoutes.kt. Rien d'autre à changer.
interface PaiementService {
    val methode: String
    fun payer(idUser: Int, montant: BigDecimal): PaiementResult
}

// Étape 1 : paiement simulé (aucun argent n'est prélevé)
object SimulatedPaiementService : PaiementService {
    override val methode = "SIMULE"

    override fun payer(idUser: Int, montant: BigDecimal): PaiementResult =
        PaiementResult(success = true, reference = "SIM-" + UUID.randomUUID().toString().take(12).uppercase())
}