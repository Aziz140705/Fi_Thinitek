package com.example.fithnitekmobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.example.fithnitekmobile.databinding.ActivityPassagerBinding
import com.example.fithnitekmobile.util.GeoPoint
import com.example.fithnitekmobile.util.GeoUtils
import com.example.fithnitekmobile.util.Resource
import com.example.fithnitekmobile.viewmodel.AuthViewModel
import com.example.fithnitekmobile.viewmodel.PassagerViewModel
import com.example.fithnitekmobile.models.ReservationMode
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.PropertyFactory.fillExtrusionBase
import org.maplibre.android.style.layers.PropertyFactory.fillExtrusionColor
import org.maplibre.android.style.layers.PropertyFactory.fillExtrusionHeight
import org.maplibre.android.style.layers.PropertyFactory.fillExtrusionOpacity

// VIEW de l'accueil passager : carte + formulaire de recherche + menu.
// Elle n'appelle plus Retrofit ni SharedPreferences : tout passe par les ViewModels.
//  - AuthViewModel     : session, rôle, déconnexion, "devenir conducteur"
//  - PassagerViewModel : départ / destination / trajets disponibles / compatibilité
class PassagerActivity : BaseActivity() {

    private lateinit var binding: ActivityPassagerBinding
    private val authViewModel: AuthViewModel by viewModels { viewModelFactory }
    private val viewModel: PassagerViewModel by viewModels { viewModelFactory }

    private var mapLibreMap: MapLibreMap? = null
    private var mapStarted = false   // true seulement si binding.map.onCreate() a été appelé
    private var isMapExpanded = false
    private var cachedDepartIcon: Icon? = null
    private var cachedDestIcon: Icon? = null
    private var cachedUserIcon: Icon? = null

    private var currentRole = "PASSAGER"

    // Gestionnaire de demandes de permissions de géolocalisation
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fine || coarse) {
            mapLibreMap?.style?.let { enableLocationComponent(it) }
        } else {
            toast("Permission de localisation refusée")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)

        binding = ActivityPassagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pas de session valide : retour à l'accueil (le ViewModel a déjà vérifié l'expiration du token)
        val user = authViewModel.authenticatedUser.value
        if (user == null) {
            goToWelcome()
            return
        }
        renderRole(user.role)

        binding.map.onCreate(savedInstanceState)
        mapStarted = true
        initMap()
        setupButtons()
        setupOnBackPressed()
        observeViewModels()

        // Charge les vrais trajets publiés par les conducteurs
        viewModel.loadTrajetsDisponibles()
    }

    // ------------------------------------------------------------------
    // Observation des ViewModels
    // ------------------------------------------------------------------
    private fun observeViewModels() {
        // Session : si elle disparaît (déconnexion, token expiré) on quitte l'écran ;
        // si le rôle change (devenir conducteur) l'affichage est mis à jour.
        authViewModel.authenticatedUser.observe(this) { user ->
            if (user == null) goToWelcome() else renderRole(user.role)
        }

        authViewModel.conducteurResult.observe(this) { event ->
            when (val state = event.getContentIfNotHandled()) {
                is Resource.Success -> {
                    toast("Vous êtes maintenant conducteur 🚗", long = true)
                    startActivity(Intent(this, MesTrajetsActivity::class.java))
                }
                is Resource.Error -> showError(state.message)
                else -> Unit
            }
        }

        viewModel.mapInfo.observe(this) { info ->
            binding.tvMapInfo.text = info
        }

        // Dès que les données de la carte changent, on la redessine
        viewModel.trajets.observe(this) { redrawRouteAndMarkers() }
        viewModel.passagerPosition.observe(this) { redrawRouteAndMarkers() }
    }

    private fun renderRole(role: String) {
        currentRole = role

        binding.tvRolePill.text = when (role) {
            "CONDUCTEUR" -> "Conducteur"
            "ADMIN" -> "Admin"
            else -> "Passager"
        }
        binding.tvBienvenue.text = "Bienvenue, $role !"

        try {
            val headerView = binding.navView.getHeaderView(0)
            headerView?.findViewById<TextView>(R.id.tvHeaderBienvenue)?.text = "Bienvenue, $role !"
        } catch (e: Exception) {
            Log.e("FiThnitek", "Header view error", e)
        }

        binding.btnPasserConducteur.text =
            if (role == "CONDUCTEUR") "Espace Conducteur 🚗" else "Passer à un compte conducteur 🚗"
    }

    // ------------------------------------------------------------------
    // Carte (code d'origine conservé ; l'état vient maintenant du ViewModel)
    // ------------------------------------------------------------------
    private fun initMap() {
        binding.map.getMapAsync { map ->
            mapLibreMap = map

            map.uiSettings.isZoomGesturesEnabled = true
            map.uiSettings.isScrollGesturesEnabled = true
            map.uiSettings.isRotateGesturesEnabled = true
            map.uiSettings.isTiltGesturesEnabled = true
            map.uiSettings.isDoubleTapGesturesEnabled = true
            map.uiSettings.isCompassEnabled = true

            val apiKey = BuildConfig.CARTO_API_KEY
            val styleUrl = if (apiKey.isNotBlank()) {
                "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json?key=$apiKey"
            } else {
                "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json"
            }

            map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                setupMapContent(map, style)
            }
        }
    }

    private fun setupMapContent(map: MapLibreMap, style: Style) {
        val position = viewModel.passagerPosition.value ?: GeoPoint(36.8150, 10.1950)
        map.cameraPosition = CameraPosition.Builder()
            .target(LatLng(position.lat, position.lng))
            .zoom(13.5)
            .tilt(45.0)
            .bearing(0.0)
            .build()

        enableLocationComponent(style)
        redrawRouteAndMarkers()

        try {
            if (style.getSource("openmaptiles") != null) {
                val buildingLayer = FillExtrusionLayer("3d-buildings", "openmaptiles")
                buildingLayer.withSourceLayer("building")
                buildingLayer.setProperties(
                    fillExtrusionColor(Color.parseColor("#B7D8F5")),
                    fillExtrusionHeight(get("render_height")),
                    fillExtrusionBase(get("render_min_height")),
                    fillExtrusionOpacity(0.9f)
                )
                style.addLayer(buildingLayer)
            }
        } catch (e: Exception) {
            Log.e("FiThnitekMap", "Bâtiments 3D non disponibles", e)
        }

        map.addOnMapClickListener { tapped ->
            handleMapTap(tapped)
            true
        }

        // Appui long = on fixe directement la destination
        map.addOnMapLongClickListener { tapped ->
            viewModel.selectDestination()
            handleMapTap(tapped)
            true
        }
    }

    private fun enableLocationComponent(style: Style) {
        val map = mapLibreMap ?: return

        val hasFine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            try {
                val locationComponent = map.locationComponent
                val options = LocationComponentOptions.builder(this)
                    .foregroundDrawable(R.drawable.ic_user_location_pin)
                    .build()
                val activation = LocationComponentActivationOptions.builder(this, style)
                    .locationComponentOptions(options)
                    .build()

                locationComponent.activateLocationComponent(activation)
                locationComponent.isLocationComponentEnabled = true
                locationComponent.cameraMode = CameraMode.NONE
                locationComponent.renderMode = RenderMode.COMPASS

                // Vraie position GPS du passager (si disponible) -> ViewModel
                locationComponent.lastKnownLocation?.let {
                    viewModel.setPassagerPosition(GeoPoint(it.latitude, it.longitude))
                }
            } catch (e: Exception) {
                Log.e("FiThnitekMap", "Erreur activation composant localisation", e)
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Le ViewModel décide quoi faire du clic ; la View affiche le résultat
    private fun handleMapTap(tapped: LatLng) {
        if (mapLibreMap == null) return

        val point = GeoPoint(tapped.latitude, tapped.longitude)
        viewModel.onMapTapped(point)
        if (viewModel.selectingDepart) {
            val label = "%.4f, %.4f".format(point.lat, point.lng)
            binding.etDepart.setText(label)
            toast("📍 Départ fixé : $label")
        } else {
            val label = "%.4f, %.4f".format(point.lat, point.lng)
            binding.etDestination.setText(label)
            toast("🏁 Destination fixée : $label")
        }
        redrawRouteAndMarkers()
    }

    private fun initIcons() {
        if (cachedDepartIcon == null) cachedDepartIcon = getIconFromVector(R.drawable.ic_depart_pin, 38, 38)
        if (cachedDestIcon == null) cachedDestIcon = getIconFromVector(R.drawable.ic_destination_pin, 38, 38)
        if (cachedUserIcon == null) cachedUserIcon = getIconFromVector(R.drawable.ic_user_location_pin, 40, 40)
    }

    private fun getIconFromVector(drawableId: Int, widthDp: Int = 36, heightDp: Int = 36): Icon? {
        return try {
            val drawable = ContextCompat.getDrawable(this, drawableId) ?: return null
            val density = resources.displayMetrics.density
            val pxWidth = (widthDp * density).toInt()
            val pxHeight = (heightDp * density).toInt()

            val bitmap = Bitmap.createBitmap(pxWidth, pxHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, pxWidth, pxHeight)
            drawable.draw(canvas)

            IconFactory.getInstance(this).fromBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("FiThnitekMap", "Erreur création icône vectorielle", e)
            null
        }
    }

    private fun redrawRouteAndMarkers() {
        val map = mapLibreMap ?: return
        map.clear()
        initIcons()

        // 1) Trajets réellement publiés par les conducteurs (lignes vertes)
        viewModel.trajets.value.orEmpty().filter { it.hasCoordinates() }.forEach { t ->
            map.addPolyline(
                PolylineOptions()
                    .add(LatLng(t.departLat!!, t.departLng!!), LatLng(t.destLat!!, t.destLng!!))
                    .color(Color.parseColor("#15803D"))
                    .width(3f)
            )
        }

        val depart = viewModel.depart.value
        val destination = viewModel.destination.value

        // 2) Départ choisi par le passager
        if (depart != null) {
            val options = MarkerOptions()
                .position(LatLng(depart.lat, depart.lng))
                .title("📍 Point de Départ")
                .snippet(viewModel.departLabel ?: "")
            cachedDepartIcon?.let { options.icon(it) }
            map.addMarker(options)
        }

        // 3) Destination choisie
        if (destination != null) {
            val distText = if (depart != null)
                "Distance estimée: %.1f km".format(GeoUtils.distanceKm(depart.lat, depart.lng, destination.lat, destination.lng)) else ""
            val options = MarkerOptions()
                .position(LatLng(destination.lat, destination.lng))
                .title("🏁 Destination")
                .snippet(distText)
            cachedDestIcon?.let { options.icon(it) }
            map.addMarker(options)
        }

        // 4) Position du passager
        viewModel.passagerPosition.value?.let { p ->
            val options = MarkerOptions()
                .position(LatLng(p.lat, p.lng))
                .title("👤 Votre Position (Passager)")
                .snippet("Vous êtes actuellement dans cette zone")
            cachedUserIcon?.let { options.icon(it) }
            map.addMarker(options)
        }

        // 5) Ligne rouge du trajet souhaité par le passager
        if (depart != null && destination != null) {
            map.addPolyline(
                PolylineOptions()
                    .add(LatLng(depart.lat, depart.lng), LatLng(destination.lat, destination.lng))
                    .color(Color.parseColor("#E10600"))
                    .width(6f)
            )
        }
    }

    // ------------------------------------------------------------------
    // Boutons et menu
    // ------------------------------------------------------------------
    private fun setupButtons() {
        binding.btnMenu.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }
        binding.btnProfil.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.nav_accueil -> true

                R.id.nav_rechercher -> {
                    startActivity(RechercheActivity.newIntent(this, viewModel.buildSearchParams("", "")))
                    true
                }

                R.id.nav_reservations -> {
                    openReservations(ReservationMode.PASSAGER)
                    true
                }

                R.id.nav_trajets, R.id.nav_passer_conducteur, R.id.nav_dossier -> {
                    showPasserConducteur()
                    true
                }

                R.id.nav_profil -> {
                    toast("Profil - à développer")
                    true
                }

                R.id.nav_parametres -> {
                    toast("Paramètres - à développer")
                    true
                }

                R.id.nav_deconnexion -> {
                    authViewModel.logout()   // l'observer de la session ramène à l'accueil
                    true
                }

                else -> false
            }
        }

        binding.btnPasserConducteur.setOnClickListener { showPasserConducteur() }

        binding.btnLocaliser.setOnClickListener {
            val map = mapLibreMap ?: return@setOnClickListener

            val lastLocation = try {
                if (map.locationComponent.isLocationComponentActivated) {
                    map.locationComponent.lastKnownLocation
                } else null
            } catch (e: Exception) {
                null
            }

            if (lastLocation != null) {
                val userPos = LatLng(lastLocation.latitude, lastLocation.longitude)
                viewModel.setPassagerPosition(GeoPoint(userPos.latitude, userPos.longitude))
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(userPos, 15.0))
                toast("Ma position GPS actuelle")
            } else {
                val p = viewModel.passagerPosition.value ?: GeoPoint(36.8150, 10.1950)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(p.lat, p.lng), 15.0))
                mapLibreMap?.style?.let { enableLocationComponent(it) }
            }
        }

        binding.btnZoomIn.setOnClickListener {
            mapLibreMap?.let { map ->
                map.animateCamera(CameraUpdateFactory.zoomTo(map.cameraPosition.zoom + 1.0))
            }
        }
        binding.btnZoomOut.setOnClickListener {
            mapLibreMap?.let { map ->
                map.animateCamera(CameraUpdateFactory.zoomTo(map.cameraPosition.zoom - 1.0))
            }
        }

        binding.btnAgrandir.setOnClickListener {
            isMapExpanded = !isMapExpanded
            if (isMapExpanded) {
                binding.cardPassengerRide.visibility = View.GONE
                toast("Carte plein écran - touchez pour sélectionner")
            } else {
                binding.cardPassengerRide.visibility = View.VISIBLE
                toast("Carte réduite")
            }
        }

        // Le champ actif décide si le prochain clic sur la carte fixe le départ ou la destination
        binding.etDepart.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) viewModel.selectingDepart = true
        }
        binding.etDestination.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) viewModel.selectingDepart = false
        }

        // US Sprint 2 : lancer la recherche de trajets
        binding.btnRechercher.setOnClickListener {
            val depart = binding.etDepart.text.toString().trim()
            val destination = binding.etDestination.text.toString().trim()

            if (depart.isEmpty()) {
                binding.tilDepart.error = "Veuillez saisir le point de départ"
                return@setOnClickListener
            }
            binding.tilDepart.error = null

            if (destination.isEmpty()) {
                binding.tilDestination.error = "Veuillez saisir la destination"
                return@setOnClickListener
            }
            binding.tilDestination.error = null

            startActivity(RechercheActivity.newIntent(this, viewModel.buildSearchParams(depart, destination)))
        }

        binding.btnMesReservations.setOnClickListener { openReservations(ReservationMode.PASSAGER) }

        binding.btnDeconnexion.setOnClickListener { authViewModel.logout() }
    }

    private fun openReservations(mode: ReservationMode) {
        startActivity(Intent(this, ReservationsActivity::class.java).apply {
            putExtra(ReservationsActivity.EXTRA_MODE, mode.name)
        })
    }

    // Conducteur : ouvre son espace. Passager : propose de devenir conducteur.
    private fun showPasserConducteur() {
        if (currentRole == "CONDUCTEUR") {
            startActivity(Intent(this, MesTrajetsActivity::class.java))
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Passer à un compte conducteur 🚗")
            .setMessage(
                "Pour publier des trajets, il faut un compte conducteur.\n\n" +
                        "Sprint 2 : le passage est immédiat et gratuit.\n\n" +
                        "Sprint 3 (prévu) :\n" +
                        "1. Paiement des frais d'activation\n" +
                        "2. Dépôt du dossier (CIN, permis, carte grise, assurance, photo voiture)\n" +
                        "3. Validation par l'administrateur (PENDING -> VALIDATED / REJECTED)\n\n" +
                        "Devenir conducteur maintenant ?"
            )
            .setPositiveButton("Devenir conducteur") { _, _ -> authViewModel.devenirConducteur() }
            .setNegativeButton("Plus tard", null)
            .show()
    }

    // ------------------------------------------------------------------
    // Cycle de vie de la carte
    // ------------------------------------------------------------------
    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::binding.isInitialized && binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (mapStarted) binding.map.onStart()
    }

    override fun onResume() {
        super.onResume()
        if (mapStarted) {
            binding.map.onResume()
            // Revenir de "Mes trajets" : les trajets / places ont pu changer
            viewModel.loadTrajetsDisponibles()
        }
    }

    override fun onPause() {
        if (mapStarted) binding.map.onPause()
        super.onPause()
    }

    override fun onStop() {
        if (mapStarted) binding.map.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        if (mapStarted) binding.map.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mapStarted) binding.map.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (mapStarted) binding.map.onLowMemory()
    }
}
