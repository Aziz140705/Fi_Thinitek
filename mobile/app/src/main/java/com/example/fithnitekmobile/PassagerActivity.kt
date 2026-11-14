package com.example.fithnitekmobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import com.example.fithnitekmobile.databinding.ActivityPassagerBinding
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
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
import kotlin.math.cos
import kotlin.math.sqrt

class PassagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPassagerBinding
    private var mapLibreMap: MapLibreMap? = null

    private var selectionMode = "depart" // depart ou destination
    private var departMarker: Marker? = null
    private var destMarker: Marker? = null
    private var isMapExpanded = false

    private var cachedDepartIcon: Icon? = null
    private var cachedDestIcon: Icon? = null
    private var cachedUserIcon: Icon? = null

    private var pointX = LatLng(36.8000, 10.1800)
    private var pointY = LatLng(36.8500, 10.2200)
    private val pointZ = LatLng(36.8150, 10.1950)

    // Gestionnaire de demandes de permissions de géolocalisation
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            mapLibreMap?.style?.let { style ->
                enableLocationComponent(style)
            }
        } else {
            Toast.makeText(this, "Permission de localisation refusée", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapLibre.getInstance(this)

        binding = ActivityPassagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val session = getSharedPreferences("FiThnitekSession", MODE_PRIVATE)
        val token = session.getString("token", null)
        val role = session.getString("role", "PASSAGER") ?: "PASSAGER"

        if (token.isNullOrBlank() || isTokenExpired(token)) {
            session.edit { clear() }
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        binding.tvRolePill.text = when (role) {
            "CONDUCTEUR" -> "Conducteur"
            "ADMIN" -> "Admin"
            else -> "Passager"
        }
        binding.tvBienvenue.text = "Bienvenue, $role !"

        try {
            val headerView = binding.navView.getHeaderView(0)
            val tvHeaderBienvenue = headerView?.findViewById<TextView>(R.id.tvHeaderBienvenue)
            tvHeaderBienvenue?.text = "Bienvenue, $role !"
        } catch (e: Exception) {
            Log.e("FiThnitek", "Header view error", e)
        }

        if (role == "CONDUCTEUR") {
            binding.btnPasserConducteur.text = "Espace Conducteur 🚗"
        }

        binding.map.onCreate(savedInstanceState)
        initMap()
        setupButtons()
    }

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
        map.cameraPosition = CameraPosition.Builder()
            .target(pointZ)
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

        map.addOnMapClickListener { tappedPoint ->
            handleMapTap(tappedPoint)
            true
        }
        map.addOnMapLongClickListener { tappedPoint ->
            selectionMode = "destination"
            handleMapTap(tappedPoint)
            true
        }

        Log.i("FiThnitekMap", "Carte CARTO Voyager chargée avec succès")
    }

    private fun enableLocationComponent(style: Style) {
        val map = mapLibreMap ?: return

        val hasFinePermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarsePermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFinePermission || hasCoarsePermission) {
            try {
                val locationComponent = map.locationComponent
                val locationComponentOptions = LocationComponentOptions.builder(this)
                    .foregroundDrawable(R.drawable.ic_user_location_pin)
                    .build()

                val activationOptions = LocationComponentActivationOptions.builder(this, style)
                    .locationComponentOptions(locationComponentOptions)
                    .build()

                locationComponent.activateLocationComponent(activationOptions)
                locationComponent.isLocationComponentEnabled = true
                locationComponent.cameraMode = CameraMode.NONE
                locationComponent.renderMode = RenderMode.COMPASS
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

    private fun handleMapTap(tapped: LatLng) {
        if (mapLibreMap == null) return

        val latFormatted = String.format("%.4f", tapped.latitude)
        val lngFormatted = String.format("%.4f", tapped.longitude)

        if (selectionMode == "depart") {
            pointX = tapped
            binding.etDepart.setText("Départ : $latFormatted, $lngFormatted")
            Toast.makeText(this, "📍 Départ fixé ($latFormatted, $lngFormatted)", Toast.LENGTH_SHORT).show()
            selectionMode = "destination"
        } else {
            pointY = tapped
            binding.etDestination.setText("Destination : $latFormatted, $lngFormatted")
            Toast.makeText(this, "🏁 Destination fixée ($latFormatted, $lngFormatted)", Toast.LENGTH_SHORT).show()
            selectionMode = "depart"
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

        val distKm = calculateDistanceKm(pointX, pointY)
        val distText = String.format("%.1f km", distKm)

        val departOptions = MarkerOptions()
            .position(pointX)
            .title("📍 Point de Départ")
            .snippet("Lat: ${String.format("%.4f", pointX.latitude)}, Lng: ${String.format("%.4f", pointX.longitude)}")

        cachedDepartIcon?.let { departOptions.icon(it) }
        departMarker = map.addMarker(departOptions)

        val destOptions = MarkerOptions()
            .position(pointY)
            .title("🏁 Destination")
            .snippet("Distance estimée: $distText")

        cachedDestIcon?.let { destOptions.icon(it) }
        destMarker = map.addMarker(destOptions)

        val userOptions = MarkerOptions()
            .position(pointZ)
            .title("👤 Votre Position (Passager)")
            .snippet("Vous êtes actuellement dans cette zone")

        cachedUserIcon?.let { userOptions.icon(it) }
        map.addMarker(userOptions)

        map.addPolyline(
            PolylineOptions()
                .add(pointX, pointY)
                .color(Color.parseColor("#E10600"))
                .width(6f)
        )

        updateCompatibility()
    }

    private fun calculateDistanceKm(p1: LatLng, p2: LatLng): Double {
        val lat1 = Math.toRadians(p1.latitude)
        val lon1 = Math.toRadians(p1.longitude)
        val lat2 = Math.toRadians(p2.latitude)
        val lon2 = Math.toRadians(p2.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return 6371.0 * c
    }

    private fun updateCompatibility() {
        val compatible = isOnRoute(pointZ, pointX, pointY, 3000.0)
        val distKm = calculateDistanceKm(pointX, pointY)
        binding.tvMapInfo.text = if (compatible) {
            "✅ Trajet compatible (~${String.format("%.1f", distKm)} km)"
        } else {
            "❌ Trajet non compatible (> 5km détour)"
        }
        binding.tvMapInfo.setTextColor(if (compatible) Color.rgb(21, 128, 61) else Color.RED)
    }

    private fun setupButtons() {

        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.btnProfil.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.nav_accueil, R.id.nav_rechercher -> {
                    Toast.makeText(this, "Accueil Carte", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_reservations -> {
                    Toast.makeText(this, "Mes réservations - à développer", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_trajets -> {
                    Toast.makeText(this, "Mes trajets - à développer", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_passer_conducteur, R.id.nav_dossier -> {
                    showPasserConducteur()
                    true
                }
                R.id.nav_profil -> {
                    Toast.makeText(this, "Profil - à développer", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_parametres -> {
                    Toast.makeText(this, "Paramètres - à développer", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_deconnexion -> {
                    val session = getSharedPreferences("FiThnitekSession", MODE_PRIVATE)
                    session.edit { clear() }
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                    true
                }
                else -> false
            }
        }

        binding.btnPasserConducteur.setOnClickListener {
            showPasserConducteur()
        }

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
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(userPos, 15.0))
                Toast.makeText(this, "Ma position GPS actuelle", Toast.LENGTH_SHORT).show()
            } else {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(pointZ, 15.0))
                mapLibreMap?.style?.let { style ->
                    enableLocationComponent(style)
                }
            }
        }

        binding.btnZoomIn.setOnClickListener {
            mapLibreMap?.let { map ->
                val current = map.cameraPosition
                map.animateCamera(CameraUpdateFactory.zoomTo(current.zoom + 1.0))
            }
        }

        binding.btnZoomOut.setOnClickListener {
            mapLibreMap?.let { map ->
                val current = map.cameraPosition
                map.animateCamera(CameraUpdateFactory.zoomTo(current.zoom - 1.0))
            }
        }

        binding.btnAgrandir.setOnClickListener {
            isMapExpanded = !isMapExpanded
            if (isMapExpanded) {
                binding.cardPassengerRide.visibility = View.GONE
                Toast.makeText(this, "Carte plein écran - touchez pour sélectionner", Toast.LENGTH_SHORT).show()
            } else {
                binding.cardPassengerRide.visibility = View.VISIBLE
                Toast.makeText(this, "Carte réduite", Toast.LENGTH_SHORT).show()
            }
        }

        binding.etDepart.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) selectionMode = "depart"
        }
        binding.etDestination.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) selectionMode = "destination"
        }

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

            Toast.makeText(this, "Recherche de trajets en cours...", Toast.LENGTH_SHORT).show()
        }

        binding.btnMesReservations.setOnClickListener {
            Toast.makeText(this, "Mes réservations bientôt disponibles", Toast.LENGTH_SHORT).show()
        }

        binding.btnDeconnexion.setOnClickListener {
            val session = getSharedPreferences("FiThnitekSession", MODE_PRIVATE)
            session.edit { clear() }

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            finish()
        }
    }

    private fun showPasserConducteur() {
        val role = getSharedPreferences("FiThnitekSession", MODE_PRIVATE).getString("role", "PASSAGER")
        if (role == "CONDUCTEUR") {
            Toast.makeText(this, "Tu es déjà Conducteur - Espace Conducteur à développer", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Passer à un compte conducteur 🚗")
            .setMessage(
                "Étapes RG06 :\n\n" +
                        "1. PAYER - Frais d'activation (ex: 20 DT) - à développer avec paiement\n" +
                        "2. METTRE TON DOSSIER :\n" +
                        "   • CIN recto / verso (confidentiel)\n" +
                        "   • Permis de conduire\n" +
                        "   • Carte grise\n" +
                        "   • Assurance\n" +
                        "   • Photo voiture + nb places + nb sièges enfant (RG09)\n\n" +
                        "3. EN ATTENTE - Admin vérifie ton dossier\n" +
                        "   PENDING -> VALIDATED / REJECTED\n\n" +
                        "Tu veux commencer ?"
            )
            .setPositiveButton("Payer puis déposer dossier") { _, _ ->
                Toast.makeText(this, "Paiement + Dossier - à développer (Sprint 3)", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Plus tard", null)
            .show()
    }

    private fun isOnRoute(
        point: LatLng,
        start: LatLng,
        end: LatLng,
        maxDistanceMeters: Double
    ): Boolean {
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon2 = Math.toRadians(end.longitude)
        val latP = Math.toRadians(point.latitude)
        val lonP = Math.toRadians(point.longitude)

        val averageLatitude = (lat1 + lat2) / 2.0
        val x = (lon2 - lon1) * cos(averageLatitude)
        val y = lat2 - lat1

        val px = (lonP - lon1) * cos(averageLatitude)
        val py = latP - lat1

        val lengthSquared = x * x + y * y
        if (lengthSquared == 0.0) return false

        val t = ((px * x + py * y) / lengthSquared).coerceIn(0.0, 1.0)
        val closestX = x * t
        val closestY = y * t

        val dx = px - closestX
        val dy = py - closestY

        val distance = sqrt(dx * dx + dy * dy) * 6371000.0
        return distance <= maxDistanceMeters
    }

    private fun isTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true

            val decoded = Base64.decode(
                parts[1],
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )

            val payload = JSONObject(String(decoded, Charsets.UTF_8))
            val exp = payload.getLong("exp")
            val currentTime = System.currentTimeMillis() / 1000L

            currentTime >= exp
        } catch (e: Exception) {
            Log.e("FiThnitekSession", "Erreur de vérification du token", e)
            true
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::binding.isInitialized && binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onStart() {
        super.onStart()
        if (::binding.isInitialized) binding.map.onStart()
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) binding.map.onResume()
    }

    override fun onPause() {
        if (::binding.isInitialized) binding.map.onPause()
        super.onPause()
    }

    override fun onStop() {
        if (::binding.isInitialized) binding.map.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        if (::binding.isInitialized) binding.map.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::binding.isInitialized) binding.map.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        if (::binding.isInitialized) binding.map.onLowMemory()
    }
}