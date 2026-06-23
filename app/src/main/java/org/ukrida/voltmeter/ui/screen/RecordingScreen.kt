package org.ukrida.voltmeter.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.ukrida.voltmeter.data.model.Customer
import org.ukrida.voltmeter.viewmodel.VoltMeterViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun RecordingScreen(
    viewModel: VoltMeterViewModel,
    customer: Customer,
    onRecordingSuccess: () -> Unit
) {
    val context = LocalContext.current
    val isLoading = viewModel.isLoading.value
    val currentReading = viewModel.currentReading.value
    val visitStatus = viewModel.visitStatus.value
    val photoUriString = viewModel.photoUriString.value
    val notes = viewModel.notes.value

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentFile by remember { mutableStateOf<File?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val statusOptions = listOf("TERBACA_NORMAL", "RUMAH_KOSONG", "HALANGAN")
    val statusLabels = listOf("Terbaca Normal", "Rumah Kosong", "Halangan")
    var expandedStatus by remember { mutableStateOf(false) }

    val successMsg = viewModel.successMessage.value
    val errorMsg = viewModel.errorMessage.value

    // GPS State
    var currentLat by remember { mutableStateOf(0.0) }
    var currentLng by remember { mutableStateOf(0.0) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Check location permissions
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
    }

    LaunchedEffect(Unit) {
        val fineLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fineLoc == PackageManager.PERMISSION_GRANTED || coarseLoc == PackageManager.PERMISSION_GRANTED) {
            hasLocationPermission = true
        } else {
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    DisposableEffect(hasLocationPermission) {
        var locationCallback: LocationCallback? = null
        if (hasLocationPermission) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    p0.lastLocation?.let { loc ->
                        currentLat = loc.latitude
                        currentLng = loc.longitude
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
        onDispose {
            locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        }
    }


    LaunchedEffect(successMsg) {
        successMsg?.let {
            viewModel.clearMessages()
            onRecordingSuccess()
        }
    }

    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            viewModel.clearMessages()
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let {
                imageUri = it
                viewModel.setPhoto(it.toString(), currentFile)
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "meter_photo_${System.currentTimeMillis()}.jpg")
            currentFile = file
            cameraImageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            cameraImageUri?.let { cameraLauncher.launch(it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Customer Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, null, tint = Color(0xFF1565C0))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Data Pelanggan", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("ID: ${customer.customer_id}", color = Color.Gray, fontSize = 12.sp)
                Text(customer.address, color = Color.Gray, fontSize = 13.sp)
                Text("${customer.power_va} VA - ${customer.tariff}", color = Color.Gray, fontSize = 13.sp)
                Text(
                    "Stand Bulan Lalu: ${customer.last_meter_reading} kWh",
                    color = Color(0xFF1565C0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Meter Input
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Angka Stand Meter", fontWeight = FontWeight.Bold)
                    if (customer.meters.size > 1) {
                        Text(
                            "Meter ${viewModel.currentMeterIndex.value + 1}/${customer.meters.size}",
                            color = Color(0xFF1565C0),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = currentReading,
                    onValueChange = { viewModel.setCurrentReading(it) },
                    label = { Text("Angka Meter (kWh)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (currentReading.isNotEmpty()) {
                    val reading = currentReading.toDoubleOrNull()
                    if (reading != null && reading < customer.last_meter_reading) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFF44336), modifier = Modifier.height(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Angka < stand bulan lalu!",
                                color = Color(0xFFF44336),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Visit Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Status Kunjungan", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedStatus,
                    onExpandedChange = { expandedStatus = it }
                ) {
                    OutlinedTextField(
                        value = statusLabels[statusOptions.indexOf(visitStatus)],
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStatus) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedStatus,
                        onDismissRequest = { expandedStatus = false }
                    ) {
                        statusOptions.forEachIndexed { index, status ->
                            DropdownMenuItem(
                                text = { Text(statusLabels[index]) },
                                onClick = {
                                    viewModel.setVisitStatus(status)
                                    expandedStatus = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { viewModel.setNotes(it) },
                    label = { Text("Catatan (opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Photo
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Dokumentasi Foto (Wajib)", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                if (photoUriString != null) {
                    AsyncImage(
                        model = Uri.parse(photoUriString),
                        contentDescription = "Foto Meter",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        when {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                                val file = File(context.cacheDir, "meter_photo_${System.currentTimeMillis()}.jpg")
                                currentFile = file
                                cameraImageUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )
                                cameraImageUri?.let { cameraLauncher.launch(it) }
                            }
                            else -> {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A9A4D),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (photoUriString != null) "Ubah Foto" else "Ambil Foto")
                }
            }
        }

        // Location Status
        if (!hasLocationPermission) {
            Text(
                "Izin lokasi dibutuhkan untuk verifikasi kunjungan",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { onRecordingSuccess() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Batal")
            }

            Button(
                onClick = { viewModel.submitMeterRecord(latitude = currentLat, longitude = currentLng) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = photoUriString != null && !isLoading && hasLocationPermission
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan")
                }
            }
        }
    }
}
