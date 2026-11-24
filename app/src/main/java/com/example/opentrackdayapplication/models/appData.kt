package com.example.opentrackdayapplication.models

import android.content.Context
import java.io.File

object AppData {

    lateinit var placedMarkList: PlacemarkStore
        private set

    fun initialize(context: Context) {
        val file = File(
            context.filesDir,
            "place_list.json"
        )

        placedMarkList = PlacemarkStore(file)
    }
}