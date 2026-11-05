
package com.fithnitek.config

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

object EmailService {

    // Adresse Gmail utilisée pour envoyer les codes OTP
    private const val GMAIL = "rayenessid15@gmail.com"

    // Mot de passe d'application Gmail (16 caractères)
    private const val APP_PASSWORD = "bosm uece rzum qrgg"

    // Envoyer le code de vérification de 6 chiffres
    fun sendResetCode(toEmail: String, code: String) {

        // Configuration du serveur SMTP Gmail
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

        // Authentification avec Gmail
        val session = Session.getInstance(
            properties,
            object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(
                        GMAIL,
                        APP_PASSWORD.replace(" ", "")
                    )
            }
        )

        // Construire l'email
        val message = MimeMessage(session).apply {

            setFrom(InternetAddress(GMAIL))

            setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(toEmail)
            )

            setSubject(
                "Fi Thnitek - Code de vérification",
                "UTF-8"
            )

            // Corps de l'email : code OTP sans lien
            setText(
                """
                Bonjour,

                Vous avez demandé la réinitialisation
                de votre mot de passe Fi Thnitek.

                Votre code de vérification est :

                $code

                Ce code est valable pendant 15 minutes.

                Ne partagez ce code avec personne.

                Si vous n'avez pas demandé cette opération,
                ignorez cet email.

                L'équipe Fi Thnitek
                """.trimIndent(),
                "UTF-8"
            )
        }

        // Envoyer l'email
        Transport.send(message)
    }
}
