package com.fithnitek.config

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

// Envoi des codes OTP par Gmail.
// Les identifiants ne sont JAMAIS écrits dans le code : ils viennent des variables d'environnement
//   GMAIL_USER          : adresse Gmail expéditrice
//   GMAIL_APP_PASSWORD  : mot de passe d'application Gmail (16 caractères)
object EmailService {

    private fun gmailUser(): String =
        System.getenv("GMAIL_USER")?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Variable d'environnement GMAIL_USER manquante")

    private fun appPassword(): String =
        System.getenv("GMAIL_APP_PASSWORD")?.replace(" ", "")?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Variable d'environnement GMAIL_APP_PASSWORD manquante")

    // Envoyer le code OTP de 6 chiffres
    fun sendResetCode(toEmail: String, code: String) {
        val user = gmailUser()
        val password = appPassword()

        val properties = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.starttls.required", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
            put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
            put("mail.smtp.connectiontimeout", "10000")
            put("mail.smtp.timeout", "10000")
            put("mail.smtp.writetimeout", "10000")
        }

        val session = Session.getInstance(
            properties,
            object : Authenticator() {
                override fun getPasswordAuthentication() = PasswordAuthentication(user, password)
            }
        )
        // Pas de session.debug = true : il affiche l'authentification SMTP dans les logs.

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(user))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
            setSubject("Fi Thnitek - Code de vérification", "UTF-8")
            setText(
                """
                Bonjour,

                Vous avez demandé la réinitialisation de votre mot de passe Fi Thnitek.

                Votre code de vérification est :

                $code

                Ce code est valable pendant 15 minutes.
                Ne partagez ce code avec personne.

                Si vous n'avez pas demandé cette opération, ignorez cet email.

                L'équipe Fi Thnitek
                """.trimIndent(),
                "UTF-8"
            )
        }

        try {
            Transport.send(message)
        } catch (e: Exception) {
            System.err.println("Erreur SMTP : ${e.javaClass.simpleName} - ${e.message}")
            throw e
        }
    }
}