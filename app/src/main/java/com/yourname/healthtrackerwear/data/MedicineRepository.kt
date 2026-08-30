package com.yourname.healthtrackerwear.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MedicineRepository {
    private val _medicineList = MutableStateFlow<List<String>>(emptyList())
    val medicineList: StateFlow<List<String>> = _medicineList.asStateFlow()

    fun loadFromPrefs(context: Context) {
        val list = MedicinePrefs.getList(context)
        _medicineList.value = list
    }

    fun update(context: Context, names: List<String>) {
        MedicinePrefs.saveList(context, names)
        _medicineList.value = names
    }
}
