package org.ukrida.voltmeter.ui.screen.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ukrida.voltmeter.data.model.User
import org.ukrida.voltmeter.viewmodel.VoltMeterViewModel

@Composable
fun AdminUserScreen(
    viewModel: VoltMeterViewModel,
    onNavigateToAddUser: () -> Unit
) {
    val users = viewModel.adminUsersList.value
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadAdminData()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddUser,
                containerColor = Color(0xFF1565C0),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Petugas")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Daftar Petugas",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(users) { user ->
                    UserCard(
                        user = user,
                        onClick = { selectedUser = user },
                        onDelete = {
                            deletingUser = user
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Dialog Edit
    selectedUser?.let { user ->
        EditUserDialog(
            user = user,
            onDismiss = { selectedUser = null },
            onSave = { updatedUser ->
                viewModel.updateUser(updatedUser)
                selectedUser = null
            }
        )
    }

    // Dialog Hapus
    if (showDeleteDialog && deletingUser != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; deletingUser = null },
            title = { Text("Hapus Petugas?") },
            text = { Text("Yakin ingin menghapus \"${deletingUser!!.name}\"? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(deletingUser!!.id)
                        showDeleteDialog = false
                        deletingUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; deletingUser = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var role by remember { mutableStateOf(user.role) }
    var password by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Petugas", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = role.uppercase(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("ADMIN") },
                            onClick = { role = "admin"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("SURVEYOR") },
                            onClick = { role = "surveyor"; expanded = false }
                        )
                    }
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password Baru (opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedUser = user.copy(
                        name = name,
                        role = role,
                        password = password.ifBlank { "" }
                    )
                    onSave(updatedUser)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.Gray)
            }
        }
    )
}

@Composable
fun UserCard(
    user: User,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isAdmin = user.role == "admin"
    val iconBgColor = if (isAdmin) Color(0xFFFDE0DD) else Color(0xFFE3F2FD)
    val iconColor = if (isAdmin) Color(0xFFD32F2F) else Color(0xFF1565C0)
    val icon = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "ID: ${user.user_id} | Username: ${user.username}", color = Color.Gray, fontSize = 12.sp)
                Text(
                    text = "Role: ${user.role.uppercase()}",
                    color = iconColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red)
            }
        }
    }
}
