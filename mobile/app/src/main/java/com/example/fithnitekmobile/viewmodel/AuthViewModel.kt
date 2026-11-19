package com.example.fithnitekmobile.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fithnitekmobile.models.ForgotPasswordRequest
import com.example.fithnitekmobile.models.LoginRequest
import com.example.fithnitekmobile.models.RegisterRequest
import com.example.fithnitekmobile.models.ResetPasswordRequest
import com.example.fithnitekmobile.models.UserDto
import com.example.fithnitekmobile.models.VerifyCodeRequest
import com.example.fithnitekmobile.models.VerifyCodeResponse
import com.example.fithnitekmobile.network.ApiService
import com.example.fithnitekmobile.session.SessionManager
import com.example.fithnitekmobile.util.Event
import com.example.fithnitekmobile.util.FormError
import com.example.fithnitekmobile.util.FormField
import com.example.fithnitekmobile.util.Resource
import kotlinx.coroutines.launch

class AuthViewModel(
    private val sessionManager: SessionManager,
    private val apiService: ApiService
) : ViewModel() {

    private val _authenticatedUser = MutableLiveData<UserDto?>(sessionManager.getUser())
    val authenticatedUser: LiveData<UserDto?> = _authenticatedUser

    private val _formError = MutableLiveData<Event<FormError>>()
    val formError: LiveData<Event<FormError>> = _formError

    private val _loginResult = MutableLiveData<Event<Resource<UserDto>>>()
    val loginResult: LiveData<Event<Resource<UserDto>>> = _loginResult

    private val _registerResult = MutableLiveData<Event<Resource<Unit>>>()
    val registerResult: LiveData<Event<Resource<Unit>>> = _registerResult

    private val _forgotResult = MutableLiveData<Event<Resource<Unit>>>()
    val forgotResult: LiveData<Event<Resource<Unit>>> = _forgotResult

    private val _verifyResult = MutableLiveData<Event<Resource<VerifyCodeResponse>>>()
    val verifyResult: LiveData<Event<Resource<VerifyCodeResponse>>> = _verifyResult

    private val _resetResult = MutableLiveData<Event<Resource<Unit>>>()
    val resetResult: LiveData<Event<Resource<Unit>>> = _resetResult

    private val _conducteurResult = MutableLiveData<Event<Resource<Unit>>>()
    val conducteurResult: LiveData<Event<Resource<Unit>>> = _conducteurResult

    fun refreshSession() {
        _authenticatedUser.value = sessionManager.getUser()
    }

    fun logout() {
        sessionManager.clearSession()
        _authenticatedUser.value = null
    }

    fun login(email: String, password: String) {
        if (email.isBlank()) {
            _formError.value = Event(FormError(FormField.EMAIL, "Veuillez entrer votre email"))
            return
        }
        if (password.isBlank()) {
            _formError.value = Event(FormError(FormField.PASSWORD, "Veuillez entrer votre mot de passe"))
            return
        }

        viewModelScope.launch {
            _loginResult.value = Event(Resource.Loading)
            try {
                val response = apiService.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    val user = UserDto(
                        idUser = body.idUser ?: 0,
                        email = email,
                        role = body.role ?: "PASSAGER",
                        token = body.token
                    )
                    if (body.token != null) {
                        sessionManager.saveSession(body.token, body.idUser ?: 0, body.role ?: "PASSAGER", email = email)
                    }
                    _authenticatedUser.value = user
                    _loginResult.value = Event(Resource.Success(user))
                } else {
                    _loginResult.value = Event(Resource.Error(response.body()?.message ?: "Identifiants incorrects"))
                }
            } catch (e: Exception) {
                _loginResult.value = Event(Resource.Error(e.localizedMessage ?: "Erreur réseau"))
            }
        }
    }

    fun register(nom: String, prenom: String, email: String, telephone: String, password: String, confirmation: String) {
        if (nom.isBlank()) {
            _formError.value = Event(FormError(FormField.NOM, "Veuillez entrer votre nom"))
            return
        }
        if (prenom.isBlank()) {
            _formError.value = Event(FormError(FormField.PRENOM, "Veuillez entrer votre prénom"))
            return
        }
        if (email.isBlank()) {
            _formError.value = Event(FormError(FormField.EMAIL, "Veuillez entrer votre email"))
            return
        }
        if (password.isBlank()) {
            _formError.value = Event(FormError(FormField.PASSWORD, "Veuillez entrer un mot de passe"))
            return
        }
        if (password != confirmation) {
            _formError.value = Event(FormError(FormField.CONFIRM, "Les mots de passe ne correspondent pas"))
            return
        }

        viewModelScope.launch {
            _registerResult.value = Event(Resource.Loading)
            try {
                val response = apiService.register(RegisterRequest(nom, prenom, email, password, telephone))
                if (response.isSuccessful && response.body()?.success == true) {
                    _registerResult.value = Event(Resource.Success(Unit))
                } else {
                    _registerResult.value = Event(Resource.Error(response.body()?.message ?: "Erreur lors de la création du compte", response.code()))
                }
            } catch (e: Exception) {
                _registerResult.value = Event(Resource.Error(e.localizedMessage ?: "Erreur réseau"))
            }
        }
    }

    fun forgotPassword(email: String) {
        if (email.isBlank()) {
            _formError.value = Event(FormError(FormField.EMAIL, "Veuillez entrer votre email"))
            return
        }

        viewModelScope.launch {
            _forgotResult.value = Event(Resource.Loading)
            try {
                val response = apiService.forgotPassword(ForgotPasswordRequest(email))
                if (response.isSuccessful && response.body()?.success == true) {
                    _forgotResult.value = Event(Resource.Success(Unit))
                } else {
                    _forgotResult.value = Event(Resource.Error(response.body()?.message ?: "Erreur lors de l'envoi du code"))
                }
            } catch (e: Exception) {
                _forgotResult.value = Event(Resource.Error(e.localizedMessage ?: "Erreur réseau"))
            }
        }
    }

    fun verifyCode(email: String, code: String) {
        if (code.isBlank()) {
            _formError.value = Event(FormError(FormField.CODE, "Veuillez entrer le code"))
            return
        }

        viewModelScope.launch {
            _verifyResult.value = Event(Resource.Loading)
            try {
                val response = apiService.verifyCode(VerifyCodeRequest(email, code))
                if (response.isSuccessful && response.body()?.success == true) {
                    _verifyResult.value = Event(Resource.Success(response.body()!!))
                } else {
                    _verifyResult.value = Event(Resource.Error(response.body()?.message ?: "Code invalide"))
                }
            } catch (e: Exception) {
                _verifyResult.value = Event(Resource.Error(e.localizedMessage ?: "Erreur réseau"))
            }
        }
    }

    fun resetPassword(ticket: String, password: String, confirmation: String) {
        if (password.isBlank()) {
            _formError.value = Event(FormError(FormField.PASSWORD, "Veuillez entrer un nouveau mot de passe"))
            return
        }
        if (password != confirmation) {
            _formError.value = Event(FormError(FormField.CONFIRM, "Les mots de passe ne correspondent pas"))
            return
        }

        viewModelScope.launch {
            _resetResult.value = Event(Resource.Loading)
            try {
                val response = apiService.resetPassword(ResetPasswordRequest(ticket, password))
                if (response.isSuccessful && response.body()?.success == true) {
                    _resetResult.value = Event(Resource.Success(Unit))
                } else {
                    _resetResult.value = Event(Resource.Error(response.body()?.message ?: "Erreur lors de la réinitialisation"))
                }
            } catch (e: Exception) {
                _resetResult.value = Event(Resource.Error(e.localizedMessage ?: "Erreur réseau"))
            }
        }
    }

    fun devenirConducteur() {
        viewModelScope.launch {
            _conducteurResult.value = Event(Resource.Loading)
            try {
                val response = apiService.devenirConducteur()
                if (response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()!!
                    val currentUser = _authenticatedUser.value
                    if (currentUser != null && body.token != null) {
                        sessionManager.saveSession(
                            token = body.token,
                            idUser = currentUser.idUser,
                            role = "CONDUCTEUR",
                            nom = currentUser.nom,
                            prenom = currentUser.prenom,
                            email = currentUser.email
                        )
                        _authenticatedUser.value = currentUser.copy(role = "CONDUCTEUR", token = body.token)
                    }
                    _conducteurResult.value = Event(Resource.Success(Unit))
                } else {
                    _conducteurResult.value = Event(Resource.Error(response.body()?.message ?: "Erreur"))
                }
            } catch (e: Exception) {
                _conducteurResult.value = Event(Resource.Error(e.localizedMessage ?: "Erreur réseau"))
            }
        }
    }
}
