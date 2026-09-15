package com.neatmessage.app

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Reply
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import java.text.DateFormat
import java.util.Date

private val Canvas = Color(0xFFF5F6F1)
private val Ink = Color(0xFF1F2925)
private val Muted = Color(0xFF77827A)
private val Leaf = Color(0xFF2E6B50)
private val PaleLeaf = Color(0xFFDCEBDD)
private val Coral = Color(0xFFE77860)

private data class Message(
    val id: Long,
    val sender: String,
    val body: String,
    val date: Long,
    val suspicious: Boolean
)

private object SmsRepository {
    fun load(resolver: ContentResolver): List<Message> {
        val projection = arrayOf(
            BaseColumns._ID,
            Telephony.TextBasedSmsColumns.ADDRESS,
            Telephony.TextBasedSmsColumns.BODY,
            Telephony.TextBasedSmsColumns.DATE
        )
        val messages = mutableListOf<Message>()
        resolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.TextBasedSmsColumns.DATE} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(BaseColumns._ID)
            val addressIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.ADDRESS)
            val bodyIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.BODY)
            val dateIndex = cursor.getColumnIndex(Telephony.TextBasedSmsColumns.DATE)
            while (cursor.moveToNext()) {
                val body = cursor.getString(bodyIndex).orEmpty()
                val sender = cursor.getString(addressIndex).takeUnless { it.isNullOrBlank() } ?: "Unknown sender"
                messages += Message(
                    id = cursor.getLong(idIndex),
                    sender = sender,
                    body = body,
                    date = cursor.getLong(dateIndex),
                    suspicious = looksSuspicious(sender, body)
                )
            }
        }
        return messages
    }

    private fun looksSuspicious(sender: String, body: String): Boolean {
        val content = "$sender $body".lowercase()
        val signals = listOf("claim now", "won", "winner", "urgent", "loan", "cash reward", "click", "verify", "gift card", "free")
        return signals.any(content::contains)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NeatMessageApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NeatMessageApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var blockedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedTab by remember { mutableStateOf("All") }
    var selectedNav by remember { mutableStateOf(0) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    fun refresh() {
        if (hasPermission) messages = SmsRepository.load(context.contentResolver)
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) refresh() else permissionLauncher.launch(Manifest.permission.READ_SMS)
    }

    val filteredMessages = messages.filter { message ->
        val matchesSearch = searchText.isBlank() || message.sender.contains(searchText, true) || message.body.contains(searchText, true)
        val matchesTab = when (selectedTab) {
            "Needs review" -> message.suspicious && message.id !in blockedIds
            "Blocked" -> message.id in blockedIds
            else -> message.id !in blockedIds
        }
        matchesSearch && matchesTab
    }

    MaterialTheme {
        Scaffold(
            containerColor = Canvas,
            topBar = {
                TopAppBar(
                    title = {
                        if (searchOpen) {
                            OutlinedTextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                placeholder = { Text("Search messages") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                            )
                        } else {
                            Column {
                                Text("Live inbox", color = Muted, fontSize = 13.sp)
                                Text("Your messages", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) searchText = "" }) {
                            Icon(if (searchOpen) Icons.Outlined.Close else Icons.Outlined.Search, "Search", tint = Ink)
                        }
                        IconButton(onClick = { refresh() }) { Icon(Icons.Outlined.Refresh, "Refresh", tint = Ink) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Canvas)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    listOf(Icons.Outlined.Inbox to "Inbox", Icons.Outlined.Security to "Protection", Icons.Outlined.Settings to "Settings").forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedNav == index,
                            onClick = { selectedNav = index; if (index == 1) selectedTab = "Needs review"; if (index == 0) selectedTab = "All" },
                            icon = { Icon(item.first, item.second) },
                            label = { Text(item.second) }
                        )
                    }
                }
            },
            floatingActionButton = {
                androidx.compose.material3.FloatingActionButton(
                    onClick = { openComposer(context, "") }, containerColor = Leaf, contentColor = Color.White, shape = CircleShape
                ) { Icon(Icons.Outlined.Add, "New message") }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    if (hasPermission) SafetyBanner(messages.count { it.suspicious }, blockedIds.size)
                    else PermissionBanner { permissionLauncher.launch(Manifest.permission.READ_SMS) }
                }
                item { FilterRow(selectedTab) { selectedTab = it } }
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (hasPermission) "Recent" else "SMS access required", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.weight(1f))
                        Text("${filteredMessages.size} messages", color = Muted, fontSize = 12.sp)
                    }
                }
                if (hasPermission && filteredMessages.isEmpty()) {
                    item { EmptyState(searchText.isNotBlank()) }
                } else if (hasPermission) {
                    items(filteredMessages, key = { it.id }) { message ->
                        MessageCard(
                            message = message,
                            blocked = message.id in blockedIds,
                            onBlock = { blockedIds = if (message.id in blockedIds) blockedIds - message.id else blockedIds + message.id },
                            onReply = { openComposer(context, message.sender) }
                        )
                    }
                }
                item { Spacer(Modifier.height(70.dp)) }
            }
        }
    }
}

private fun openComposer(context: Context, sender: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(sender)}"))
    context.startActivity(Intent.createChooser(intent, "Send SMS"))
}

@Composable
private fun PermissionBanner(onGrant: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = PaleLeaf)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Security, null, tint = Leaf, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("SMS access is off", color = Ink, fontWeight = FontWeight.Bold)
                Text("Allow access to show messages from this device.", color = Muted, fontSize = 12.sp)
            }
            TextButton(onClick = onGrant) { Text("Allow", color = Leaf) }
        }
    }
}

@Composable
private fun SafetyBanner(suspiciousCount: Int, blockedCount: Int) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = PaleLeaf)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Security, null, tint = Leaf)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Live device inbox", color = Ink, fontWeight = FontWeight.Bold)
                Text("$suspiciousCount suspicious, $blockedCount blocked", color = Color(0xFF53715E), fontSize = 12.sp)
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
                colors = ButtonDefaults.buttonColors(containerColor = if (active) Ink else Color.White, contentColor = if (active) Color.White else Muted),
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 0.dp),
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
private fun EmptyState(searching: Boolean) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Inbox, null, tint = Muted, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(8.dp))
            Text(if (searching) "No matching messages" else "No messages in this view", color = Ink, fontWeight = FontWeight.Bold)
            Text(if (searching) "Try another sender or phrase." else "Pull down by tapping refresh to check again.", color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MessageCard(message: Message, blocked: Boolean, onBlock: () -> Unit, onReply: () -> Unit) {
    val label = when {
        blocked -> "Blocked"
        message.suspicious -> "Needs review"
        else -> "Trusted sender"
    }
    val labelColor = if (blocked || message.suspicious) Coral else Leaf
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(if (message.suspicious) Color(0xFFF4D3C5) else Color(0xFFBFDCC8)), contentAlignment = Alignment.Center) {
                Text(message.sender.firstOrNull()?.uppercase() ?: "?", color = Ink, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(message.sender, color = Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.weight(1f))
                    Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.date)), color = Muted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(5.dp))
                Text(message.body, color = Muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (blocked || message.suspicious) Icons.Outlined.Block else Icons.Outlined.CheckCircle, null, tint = labelColor, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(label, color = labelColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onReply, modifier = Modifier.size(30.dp)) { Icon(Icons.Outlined.Reply, "Reply", tint = Leaf) }
                    IconButton(onClick = onBlock, modifier = Modifier.size(30.dp)) { Icon(Icons.Outlined.Block, if (blocked) "Unblock" else "Block", tint = if (blocked) Muted else Coral) }
                }
            }
        }
    }
}
