package com.cockroachcommunity.app.feature.nearby

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockroachcommunity.app.core.network.model.NearbyDevice
import com.cockroachcommunity.app.core.network.model.ConnectionType
import com.cockroachcommunity.app.core.designsystem.theme.Dimens

@Composable
fun NearbyScreen(
    onDeviceConnected: (NearbyDevice) -> Unit,
    viewModel: NearbyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }.toTypedArray()
    }

    var hasPermissions by remember {
        mutableStateOf(
            permissionsToRequest.all {
                androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        hasPermissions = granted
        if (granted) {
            viewModel.startDiscovery()
        }
    }

    LaunchedEffect(hasPermissions) {
        if (!hasPermissions) {
            permissionLauncher.launch(permissionsToRequest)
        } else {
            viewModel.startDiscovery()
        }
    }

    val state = uiState
    if (state is NearbyUiState.Active && state.error != null) {
        LaunchedEffect(state.error) {
            snackbarHostState.showSnackbar(
                message = state.error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }
    
    // Automatically navigate to chat when successfully connected
    // This is optional if we want the user to explicitly tap "Chat" after connecting
    // Let's assume onDeviceConnected should only be fired when we are fully connected,
    // or we can let the user tap a "Chat" button. For now, we will fire it on click if connected,
    // or wait for connection to succeed.

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Nearby",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(if ((state as? NearbyUiState.Active)?.isDiscovering == true) com.cockroachcommunity.app.core.designsystem.theme.BrandSecondary else com.cockroachcommunity.app.core.designsystem.theme.OnlineEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if ((state as? NearbyUiState.Active)?.isDiscovering == true) "SCANNING RADAR (WI-FI DIRECT & BLE)" else "RADAR READY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = if ((state as? NearbyUiState.Active)?.isDiscovering == true) com.cockroachcommunity.app.core.designsystem.theme.BrandSecondary else com.cockroachcommunity.app.core.designsystem.theme.OnlineEmerald
                            )
                        }
                    }

                    if (hasPermissions) {
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .clickable {
                                    if (state is NearbyUiState.Active) {
                                        if (state.isDiscovering) viewModel.stopDiscovery() else viewModel.startDiscovery()
                                    }
                                },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp,
                            shadowElevation = 6.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val isDiscovering = (state as? NearbyUiState.Active)?.isDiscovering == true
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = if (isDiscovering) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasPermissions) {
                PermissionDeniedView { permissionLauncher.launch(permissionsToRequest) }
            } else {
                when (state) {
                    is NearbyUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is NearbyUiState.Active -> {
                        if (state.isDiscovering) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                        
                        if (state.devices.isEmpty()) {
                            EmptyDevicesView(state.isDiscovering)
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = Dimens.Space16, vertical = Dimens.Space8),
                                verticalArrangement = Arrangement.spacedBy(Dimens.Space12)
                            ) {
                                items(state.devices, key = { it.id }) { device ->
                                    val isConnecting = state.connectingDeviceAddress == device.address
                                    val isConnected = state.connectedDeviceAddress == device.address
                                    
                                    DeviceItem(
                                        device = device, 
                                        isConnecting = isConnecting,
                                        isConnected = isConnected,
                                        onConnectClick = { 
                                            viewModel.connectToDevice(device) 
                                        },
                                        onDisconnectClick = {
                                            viewModel.disconnect()
                                        },
                                        onChatClick = {
                                            onDeviceConnected(device)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionDeniedView(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.Space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.WifiTethering, 
            contentDescription = null, 
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimens.Space24))
        Text(
            text = "Location and Bluetooth permissions are required to discover and connect securely to nearby devices using Wi-Fi Direct and Bluetooth.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimens.Space32))
        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(Dimens.Space8)
        ) {
            Text("Grant Permissions", modifier = Modifier.padding(vertical = Dimens.Space4))
        }
    }
}

@Composable
fun EmptyDevicesView(isDiscovering: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.Space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            if (isDiscovering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(120.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            }
            Icon(
                imageVector = Icons.Default.WifiTethering,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isDiscovering) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(Dimens.Space24))
        
        if (isDiscovering) {
            Text(
                "Searching for peers", 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(Dimens.Space8))
            Text(
                "Keep this screen open. Make sure the other device is also searching.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            Text(
                "No devices found", 
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(Dimens.Space8))
            Text(
                "Tap the refresh icon in the top right to start scanning.", 
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun DeviceItem(
    device: NearbyDevice, 
    isConnecting: Boolean,
    isConnected: Boolean,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onChatClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = androidx.compose.material3.CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.Space16)
        ) {
            val friendlyName = com.cockroachcommunity.app.core.common.SocialPersonaManager.formatPeerDisplayName(device.name.ifBlank { device.id })
            val handle = if (friendlyName != "Nearby Friend") "@${friendlyName.lowercase().replace(" ", "_").take(15)}" else "@mesh_user"
            val badgeEmoji = "⚡"

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.cockroachcommunity.app.core.designsystem.components.CockroachCommunityAvatar(
                    name = friendlyName,
                    size = 56.dp,
                    isOnline = isConnected,
                    badgeEmoji = badgeEmoji
                )
                
                Spacer(modifier = Modifier.width(Dimens.Space16))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friendlyName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = handle,
                        style = MaterialTheme.typography.bodySmall,
                        color = com.cockroachcommunity.app.core.designsystem.theme.BrandPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            isConnected -> "Connected • Ready to Chat"
                            isConnecting -> "Connecting securely..."
                            device.isPaired -> "Paired Device"
                            else -> "Discovered nearby on local mesh ⚡"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isConnected || isConnecting) FontWeight.Medium else FontWeight.Normal,
                        color = when {
                            isConnected -> MaterialTheme.colorScheme.primary
                            isConnecting -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                device.signalStrength?.let { rssi ->
                    Text(
                        text = "$rssi dBm",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surfaceVariant, 
                            shape = androidx.compose.foundation.shape.CircleShape
                        ).padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Dimens.Space24))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isConnected) {
                    TextButton(
                        onClick = onDisconnectClick,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Disconnect")
                    }
                    Spacer(modifier = Modifier.width(Dimens.Space8))
                    Button(
                        onClick = onChatClick,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        contentPadding = PaddingValues(horizontal = Dimens.Space24, vertical = Dimens.Space12)
                    ) {
                        Text("Open Chat")
                    }
                } else if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(Dimens.Space16))
                } else {
                    OutlinedButton(
                        onClick = onConnectClick,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        contentPadding = PaddingValues(horizontal = Dimens.Space24, vertical = Dimens.Space12)
                    ) {
                        Text("Connect")
                    }
                }
            }
        }
    }
}
