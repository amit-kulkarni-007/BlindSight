package com.example.blindsight.presentation

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.blindsight.managers.ContactManager

@Composable
fun ContactScreen(navController: NavController) {
    val context = LocalContext.current
    val contactManager = remember { ContactManager(context) }

    var contactList by remember { mutableStateOf(contactManager.getContacts()) }
    var input by remember { mutableStateOf("") }
    var editMode by remember { mutableStateOf(false) }
    var contactBeingEdited by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Emergency Contacts", style = MaterialTheme.typography.h6)

        // Input field and buttons for adding/updating contacts
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter phone number") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (input.isNotBlank()) {
                    if (editMode) {
                        contactManager.editContact(contactBeingEdited, input)
                        editMode = false
                        contactBeingEdited = ""
                    } else {
                        contactManager.addContact(input)
                    }
                    contactList = contactManager.getContacts()
                    input = ""
                }
            }) {
                Text(if (editMode) "Update" else "Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LazyColumn to display the contacts
        LazyColumn {
            items(contactList) { contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = contact)
                    Row {
                        TextButton(onClick = {
                            input = contact
                            editMode = true
                            contactBeingEdited = contact
                        }) {
                            Text("Edit")
                        }
                        TextButton(onClick = {
                            contactManager.deleteContact(contact)
                            contactList = contactManager.getContacts()
                        }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to go back to the camera screen
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Back to Camera")
        }
    }
}
