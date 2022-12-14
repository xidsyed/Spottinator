package com.plcoding.spotifycloneyt.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.plcoding.spotifycloneyt.data.entities.Song
import com.plcoding.spotifycloneyt.other.Constants.SONG_COLLECTION
import kotlinx.coroutines.tasks.await

class MusicDatabase {
    private val firestore = FirebaseFirestore.getInstance()
    private val songCollection = firestore.collection(SONG_COLLECTION)
    suspend fun getSongs(): List<Song> {
        return try {
            return songCollection.get().await().toObjects(Song::class.java)
        } catch (e: Exception) {
            Log.d("SONGLOGGER", "getSongs: ${e.message}")
            emptyList()
        }
    }
}