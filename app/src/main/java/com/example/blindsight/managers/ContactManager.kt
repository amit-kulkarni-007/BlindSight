package com.example.blindsight.managers

import android.content.Context
import android.content.SharedPreferences

class ContactManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)

    fun getContacts(): List<String> {
        val set = sharedPreferences.getStringSet("contacts", emptySet())
        return set?.toList() ?: emptyList()
    }

    fun addContact(contact: String) {
        val current = getContacts().toMutableSet()
        current.add(contact)
        saveContacts(current)
    }

    fun deleteContact(contact: String) {
        val current = getContacts().toMutableSet()
        current.remove(contact)
        saveContacts(current)
    }

    fun editContact(old: String, new: String) {
        val current = getContacts().toMutableSet()
        if (current.remove(old)) {
            current.add(new)
            saveContacts(current)
        }
    }

    private fun saveContacts(contacts: Set<String>) {
        sharedPreferences.edit().putStringSet("contacts", contacts).apply()
    }
}
