package com.neatmessage.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Canvas = Color(0xFFF5F6F1)
private val Ink = Color(0xFF1F2925)
private val Muted = Color(0xFF77827A)
private val Leaf = Color(0xFF2E6B50)
private val PaleLeaf = Color(0xFFDCEBDD)
private val Coral = Color(0xFFE77860)

private data class Message(
    val sender: String,
    val preview: String,
    val time: String,
    val label: String,
    val initials: String,
    val tint: Color,
    val state: String,
    val unread: Boolean = false
)

private val sampleMessages = listOf(
    Message("HDFC BANK", "Your account statement is ready to view.", "09:42", "Trusted sender", "H", Color(0xFFBFDCC8), "all", true),
    Message("+1 555 018 209", "You have been selected for a cash reward. Claim now...", "08:18", "Likely spam", "?", Color(0xFFF4D3C5), "spam"),
    Message("QuickCash", "Loan approved! Get instant funds with zero paperwork.", "Yesterday", "Blocked", "Q", Color(0xFFE7C7BD), "blocked"),
    Message("AMAZON", "Your package arrives today between 2:00–4:00 PM.", "Yesterday", "Trusted sender", "A", Color(0xFFD5E4D4), "all", true),
    Message("VM-TRAVELX", "Flash sale: 70% off flights this weekend only.", "Mon", "Needs review", "V", Color(0xFFE9E0B7), "review"),
    Message("Unknown sender", "Congratulations, you won a gift card. Tap to redeem.", "Sun", "Likely spam", "!", Color(0xFFF0C9C2), "spam")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NeatMessageApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NeatMessageApp() {
    var selectedTab by remember { mutableStateOf("All") }
    var selectedNav by remember { mutableStateOf(0) }
    val visibleMessages = sampleMessages.filter {
        when (selectedTab) {
            "Needs review" -> it.state == "review"
            "Blocked" -> it.state == "blocked" || it.state == "spam"
            else -> true
        }
    }

    MaterialTheme {
        Scaffold(
            containerColor = Canvas,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Good morning", color = Muted, fontSize = 13.sp)
                            Text("Your messages", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) { Icon(Icons.Outlined.Search, "Search", tint = Ink) }
                        IconButton(onClick = {}) { Icon(Icons.Outlined.MoreHoriz, "More options", tint = Ink) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Canvas)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    listOf(Icons.Outlined.Inbox to "Inbox", Icons.Outlined.Security to "Protection", Icons.Outlined.Settings to "Settings").forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedNav == index,
                            onClick = { selectedNav = index },
                            icon = { Icon(item.first, item.second) },
                            label = { Text(item.second) }
                        )
                    }
                }
            },
            floatingActionButton = {
                androidx.compose.material3.FloatingActionButton(
                    onClick = {}, containerColor = Leaf, contentColor = Color.White, shape = CircleShape
                ) { Icon(Icons.Outlined.Add, "New message") }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { SafetyBanner() }
                item { FilterRow(selectedTab) { selectedTab = it } }
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.weight(1f))
                        Text("${visibleMessages.size} messages", color = Muted, fontSize = 12.sp)
                    }
                }
                items(visibleMessages) { message -> MessageCard(message) }
                item { Spacer(Modifier.height(70.dp)) }
            }
        }
    }
}

@Composable
private fun SafetyBanner() {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = PaleLeaf)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Security, null, tint = Leaf)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("You’re protected", color = Ink, fontWeight = FontWeight.Bold)
                Text("2 suspicious messages caught today", color = Color(0xFF53715E), fontSize = 12.sp)
            }
            Icon(Icons.Outlined.CheckCircle, null, tint = Leaf)
        }
    }
}

@Composable
private fun FilterRow(selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("All", "Needs review", "Blocked").forEach { label ->
            val active = selected == label
            Button(
                onClick = { onSelect(label) },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (active) Ink else Color.White,
                    contentColor = if (active) Color.White else Muted
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 15.dp, vertical = 0.dp),
                modifier = Modifier.height(38.dp)
            ) {
                if (label == "Needs review") Icon(Icons.Outlined.FilterList, null, Modifier.size(16.dp))
                if (label == "Needs review") Spacer(Modifier.width(5.dp))
                Text(label, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun MessageCard(message: Message) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(message.tint), contentAlignment = Alignment.Center) {
                Text(message.initials, color = Ink, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(message.sender, color = Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.weight(1f))
                    Text(message.time, color = Muted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(5.dp))
                Text(message.preview, color = Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val labelColor = if (message.label == "Blocked" || message.label == "Likely spam") Coral else Leaf
                    Icon(if (message.label == "Blocked" || message.label == "Likely spam") Icons.Outlined.Block else Icons.Outlined.CheckCircle, null, tint = labelColor, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(message.label, color = labelColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (message.unread) Box(Modifier.size(8.dp).clip(CircleShape).background(Leaf))
        }
    }
}
