package com.fithnitek.routes

import com.fithnitek.config.Frais
import com.fithnitek.config.JwtConfig
import com.fithnitek.config.PhotoStorage
import com.fithnitek.config.SimulatedPaiementService
import com.fithnitek.models.DossierResponse
import com.fithnitek.models.LoginResponse
import com.fithnitek.models.MessageResponse
import com.fithnitek.repository.ConducteurRepository
import com.fithnitek.repository.DossierInput
import com.fithnitek.repository.OpResult
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray

private val CIN_REGEX = Regex("^[0-9]{8}$")
private val PERMIS_REGEX = Regex("^[A-Z0-9/-]{4,20}$")
private val MATRICULE_REGEX = Regex("^[\\p{L}0-9 -]{4,20}$")

private val PHOTOS = listOf("cinPhoto" to "du CIN", "permisPhoto" to "du permis", "vehiculePhoto" to "du véhicule")

private class DossierForm {
    val fields = mutableMapOf<String, String>()
    val files = mutableMapOf<String, ByteArray>()
    var tooBig: Boolean = false
}

private fun ApplicationCall.currentUserId(): Int =
    principal<JWTPrincipal>()?.payload?.subject?.toIntOrNull() ?: 0

private suspend fun ApplicationCall.respondFail(fail: OpResult.Fail) {
    respond(HttpStatusCode.fromValue(fail.httpStatus), MessageResponse(false, fail.message))
}

private suspend inline fun ApplicationCall.guarded(crossinline block: suspend () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        respond(HttpStatusCode.InternalServerError, MessageResponse(false, e.localizedMessage ?: "Erreur serveur"))
    }
}

// Lit le formulaire multipart (champs texte + photos)
private suspend fun ApplicationCall.readDossierForm(): DossierForm {
    val form = DossierForm()
    receiveMultipart().forEachPart { part ->
        val name = part.name
        when (part) {
            is PartData.FormItem -> if (name != null) form.fields[name] = part.value
            is PartData.FileItem -> if (name != null) {
                val bytes = part.provider().readRemaining(PhotoStorage.MAX_BYTES + 1).readByteArray()
                if (bytes.size > PhotoStorage.MAX_BYTES) form.tooBig = true else form.files[name] = bytes
            }
            else -> Unit
        }
        part.dispose()
    }
    return form
}

private fun validate(form: DossierForm): Pair<DossierInput?, String?> {
    if (form.tooBig) return null to "Photo trop lourde (3 Mo maximum)"

    fun field(name: String) = form.fields[name]?.trim().orEmpty()

    val cin = field("cin")
    val permis = field("permis").uppercase()
    val marque = field("marque")
    val model = field("model")
    val matricule = field("matricule").uppercase().replace(Regex("\\s+"), " ")
    val couleur = field("couleur").ifEmpty { null }
    val nbPlaces = field("nbPlaces").toIntOrNull()
    val sieges = field("nbSiegesEnfant").ifEmpty { "0" }.toIntOrNull()

    if (!CIN_REGEX.matches(cin)) return null to "CIN invalide : 8 chiffres"
    if (!PERMIS_REGEX.matches(permis)) return null to "Numéro de permis invalide (4 à 20 caractères)"
    if (marque.length !in 2..50) return null to "Marque invalide"
    if (model.length !in 1..50) return null to "Modèle invalide"
    if (!MATRICULE_REGEX.matches(matricule)) return null to "Immatriculation invalide (4 à 20 caractères)"
    if (couleur != null && couleur.length > 20) return null to "Couleur trop longue (20 caractères maximum)"
    if (nbPlaces == null || nbPlaces !in 1..8) return null to "Nombre de places invalide (1 à 8)"
    if (sieges == null || sieges !in 0..nbPlaces) return null to "Nombre de sièges enfant invalide"

    for ((name, label) in PHOTOS) {
        val bytes = form.files[name] ?: return null to "Photo $label manquante"
        if (PhotoStorage.detectExtension(bytes) == null) return null to "Photo $label invalide (JPG ou PNG)"
    }

    return DossierInput(
        cin = cin, permis = permis, marque = marque, model = model, matricule = matricule,
        couleur = couleur, nbPlaces = nbPlaces, nbSiegesEnfant = sieges,
        cinPhoto = form.files.getValue("cinPhoto"),
        permisPhoto = form.files.getValue("permisPhoto"),
        vehiculePhoto = form.files.getValue("vehiculePhoto")
    ) to null
}

fun Route.conducteurRoutes() {

    authenticate("auth-jwt") {
        route("/api/conducteur") {

            // Mon dossier (statut + montant des frais)
            get("/dossier") {
                call.guarded {
                    val userId = call.currentUserId()
                    val dossier = withContext(Dispatchers.IO) {
                        ConducteurRepository.find(userId, Frais.CONDUCTEUR)
                    }
                    call.respond(HttpStatusCode.OK, DossierResponse(true, "OK", dossier))
                }
            }

            // Déposer le dossier : vérification automatique, puis statut PENDING (à payer)
            post("/dossier") {
                call.guarded {
                    val form = call.readDossierForm()
                    val (input, error) = validate(form)
                    if (input == null) {
                        call.respond(HttpStatusCode.BadRequest, MessageResponse(false, error ?: "Dossier invalide"))
                        return@guarded
                    }

                    val idUser = call.currentUserId()
                    when (val result = withContext(Dispatchers.IO) { ConducteurRepository.save(idUser, input) }) {
                        is OpResult.Fail -> call.respondFail(result)
                        is OpResult.Ok -> {
                            val dossier = withContext(Dispatchers.IO) {
                                ConducteurRepository.find(idUser, Frais.CONDUCTEUR)
                            }
                            call.respond(
                                HttpStatusCode.Created,
                                DossierResponse(true, "Dossier vérifié. Il ne reste que le paiement.", dossier)
                            )
                        }
                    }
                }
            }

            // Payer : le montant vient du serveur, pas du mobile
            post("/paiement") {
                call.guarded {
                    val idUser = call.currentUserId()
                    val result = withContext(Dispatchers.IO) {
                        ConducteurRepository.payer(idUser, Frais.CONDUCTEUR, SimulatedPaiementService)
                    }
                    when (result) {
                        is OpResult.Fail -> call.respondFail(result)
                        is OpResult.Ok -> call.respond(
                            HttpStatusCode.OK,
                            LoginResponse(
                                success = true,
                                message = "Paiement confirmé. Vous êtes conducteur !",
                                token = JwtConfig.generateToken(idUser, "CONDUCTEUR"),
                                idUser = idUser,
                                role = "CONDUCTEUR"
                            )
                        )
                    }
                }
            }
        }
    }
}
