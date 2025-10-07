package com.example.exploreo.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val language: String = "English",
)

data class Bookmark(
    val id: String = "", // document id
    val uid: String = "",
    val placeName: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val categories: List<String> = emptyList(),
)

data class ItineraryItem(
    val title: String = "",
    val note: String = "",
    val dateMillis: Long? = null,
    val time: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
)

data class Itinerary(
    val id: String = "",
    val uid: String = "",
    val items: List<ItineraryItem> = emptyList(),
)

class FirestoreRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private fun uid(): String = auth.currentUser?.uid ?: ""

    suspend fun upsertUserProfile(name: String, language: String) {
        val u = auth.currentUser ?: return
        val profile = UserProfile(uid = u.uid, name = name, email = u.email ?: "", language = language)
        db.collection("users").document(u.uid).set(profile).await()
    }

    suspend fun addBookmark(place: Place) {
        val u = uid()
        if (u.isEmpty()) return
        val doc = db.collection("users").document(u).collection("bookmarks").document()
        val bookmark = Bookmark(
            id = doc.id,
            uid = u,
            placeName = place.name ?: "",
            lat = place.lat ?: 0.0,
            lon = place.lon ?: 0.0,
            categories = place.categories ?: emptyList(),
        )
        doc.set(bookmark).await()
    }

    suspend fun removeBookmark(id: String) {
        val u = uid()
        if (u.isEmpty()) return
        db.collection("users").document(u).collection("bookmarks").document(id).delete().await()
    }

    suspend fun listBookmarks(): List<Bookmark> {
        val u = uid()
        if (u.isEmpty()) return emptyList()
        val snap = db.collection("users").document(u).collection("bookmarks").get().await()
        return snap.documents.mapNotNull { it.toObject(Bookmark::class.java)?.copy(id = it.id) }
    }

    suspend fun getBookmark(id: String): Bookmark? {
        val u = uid()
        if (u.isEmpty() || id.isBlank()) return null
        val doc = db.collection("users").document(u).collection("bookmarks").document(id).get().await()
        return doc.toObject(Bookmark::class.java)?.copy(id = doc.id)
    }

    suspend fun saveItinerary(items: List<ItineraryItem>): String? {
        val u = uid()
        if (u.isEmpty()) return null
        val doc = db.collection("users").document(u).collection("itineraries").document()
        // Save a new itinerary document for this user
        val itinerary = Itinerary(id = doc.id, uid = u, items = items)
        doc.set(itinerary).await()
        return doc.id
    }

    suspend fun getLatestItinerary(): Itinerary? {
        val u = uid()
        if (u.isEmpty()) return null
        val snap = db.collection("users").document(u).collection("itineraries").limit(1).get().await()
        return snap.documents.firstOrNull()?.toObject(Itinerary::class.java)
    }

    suspend fun listItineraries(): List<Itinerary> {
        val u = uid()
        if (u.isEmpty()) return emptyList()
        // Fetch all itineraries for the signed-in user
        val snap = db.collection("users").document(u).collection("itineraries").get().await()
        return snap.documents.mapNotNull { it.toObject(Itinerary::class.java)?.copy(id = it.id) }
    }
}



