/*
 * Copyright (c) 2024, Karthikeyan Singaravelan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.xtreak.notificationdictionary.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuTopBar(onMenuItemClick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var soundOn by remember { mutableStateOf(false) }
    TopAppBar(
        modifier = Modifier.fillMaxWidth(),
        title = { Text("Notification Directory") },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.primary),
            actions = {
                Text(color= MaterialTheme.colorScheme.primary, text = "Switch Sound")
                Switch(checked = soundOn, onCheckedChange = { soundOn = it })
                Box(contentAlignment = Alignment.TopStart) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(Icons.Default.Menu, contentDescription = "Switch Sound")
                    }
                    DropdownMenu(
                        expanded = expanded, // Control this with a state in real app
                        onDismissRequest = { expanded = false },
                        offset = DpOffset(0.dp, 0.dp),
                        //border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    ) {
                        DropdownMenuItem(
                            text = { Text("About Us") },
                            onClick = { onMenuItemClick("about_us") })
                        DropdownMenuItem(
                            onClick = { onMenuItemClick("license") },
                            text = { Text("License") })
                        DropdownMenuItem(
                            onClick = { onMenuItemClick("history") },
                            text = { Text("History") })
                        DropdownMenuItem(
                            onClick = { onMenuItemClick("favourite") },
                            text = { Text("Favourites") })
                    }
                }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewMenuTopBar() {
    MenuTopBar(onMenuItemClick = {})
}