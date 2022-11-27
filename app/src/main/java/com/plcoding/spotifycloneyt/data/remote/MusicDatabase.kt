package com.plcoding.spotifycloneyt.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.plcoding.spotifycloneyt.data.entities.Song
import com.plcoding.spotifycloneyt.other.Constants.SONG_COLLECTION
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MusicDatabase {
    private val firestore = FirebaseFirestore.getInstance()
    private val songCollection = firestore.collection(SONG_COLLECTION)

    suspend fun getSongs() : List<Song> {
        return try {
            val list  = songCollection.get().await().toObjects(Song::class.java)
            Log.d("SONGLOG", "MusicDatabase.getSongs : ${list.size} songs received")
            return list
        } catch (e : Exception) {
            Log.d("SONGLOG", "MusicDatabase.getSongs : NO SONGS RECEIVED")
            Log.d("SONGLOG", "${e.message.toString()}")
            emptyList()
        }
    }
}