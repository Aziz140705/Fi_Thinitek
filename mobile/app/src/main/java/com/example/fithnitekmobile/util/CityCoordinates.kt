package com.example.fithnitekmobile.util

object CityCoordinates {
    private val cityMap = mapOf(
        "tunis" to GeoPoint(36.8065, 10.1815),
        "ariana" to GeoPoint(36.8665, 10.1930),
        "ben arous" to GeoPoint(36.7531, 10.2222),
        "manouba" to GeoPoint(36.8081, 10.0972),
        "nabeul" to GeoPoint(36.4561, 10.7376),
        "hammamet" to GeoPoint(36.4000, 10.6167),
        "zaghouan" to GeoPoint(36.4029, 10.1429),
        "béja" to GeoPoint(36.7256, 9.1817),
        "beja" to GeoPoint(36.7256, 9.1817),
        "jendouba" to GeoPoint(36.5011, 8.7803),
        "le kef" to GeoPoint(36.1742, 8.7049),
        "kef" to GeoPoint(36.1742, 8.7049),
        "siliana" to GeoPoint(36.0849, 9.3708),
        "sousse" to GeoPoint(35.8256, 10.6369),
        "monastir" to GeoPoint(35.7833, 10.8333),
        "mahdia" to GeoPoint(35.5047, 11.0622),
        "sfax" to GeoPoint(34.7406, 10.7603),
        "kairouan" to GeoPoint(35.6781, 10.0963),
        "kasserine" to GeoPoint(35.1676, 8.8365),
        "sidi bouzid" to GeoPoint(35.0382, 9.4849),
        "gabès" to GeoPoint(33.8815, 10.0982),
        "gabes" to GeoPoint(33.8815, 10.0982),
        "medenine" to GeoPoint(33.3549, 10.5055),
        "djerba" to GeoPoint(33.8075, 10.8451),
        "tataouine" to GeoPoint(32.9297, 10.4518),
        "gafsa" to GeoPoint(34.4250, 8.7842),
        "tozeur" to GeoPoint(33.9197, 8.1336),
        "kebili" to GeoPoint(33.7044, 8.9690)
    )

    fun get(cityName: String): GeoPoint? {
        val key = cityName.trim().lowercase()
        return cityMap[key] ?: cityMap.entries.firstOrNull { key.contains(it.key) }?.value
    }
}
