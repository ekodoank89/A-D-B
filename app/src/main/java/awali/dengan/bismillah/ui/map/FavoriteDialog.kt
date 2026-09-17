package awali.dengan.bismillah.ui.map

import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import awali.dengan.bismillah.R
import com.google.android.gms.maps.model.LatLng
import java.util.Locale

// =====================================================================
// Dialog Favorite:
//  - 2 baris tab (persisten):
//      baris 1: GRB / GJK           (list favorite terpisah per tab)
//      baris 2: Dari Pin / Manual   (form aktif; pilihan tersimpan)
//  - Warna tab Dari Pin/Manual mengikuti warna tab GRB/GJK aktif:
//      GRB -> merah, GJK -> biru
//  - Form "Dari Pin": nama manual, Latitude & Longitude OTOMATIS
//    terisi dari pin tengah (read-only) -> Tombol Simpan
//  - Form "Manual": nama + latitude + longitude manual -> Tombol Simpan
//  - Tap ✏ pada item -> form "Edit Favorite" terisi dari item
//    (nama, latitude, longitude) -> Tombol Update
//  - Tap 🗑 pada item -> dialog konfirmasi (Hapus/Batal)
//  - Tap baris = fly ke lokasi
// =====================================================================

@Composable
internal fun FavoriteDialog(
    prefs: SharedPreferences,
    initialTab: FavTab,
    grbFavs: List<FavItem>,
    gjkFavs: List<FavItem>,
    currentPin: LatLng,
    onDismiss: () -> Unit,
    onAdd: (FavTab, String, LatLng) -> Unit,
    onUpdate: (FavTab, Long, String, Double, Double) -> Unit,
    onDelete: (FavTab, Long) -> Unit,
    onFlyTo: (LatLng) -> Unit
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(initialTab) }

    // Tab section aktif — dibuka sesuai yang terakhir di-tap (persisten)
    var section by remember { mutableStateOf(FavStore.lastSection(prefs)) }

    // Form "Dari Pin" — lat/lng otomatis dari pin tengah (read-only)
    var pinName by remember { mutableStateOf("") }

    // Form "Manual"
    var manualName by remember { mutableStateOf("") }
    var manualLat by remember { mutableStateOf("") }
    var manualLng by remember { mutableStateOf("") }

    // Form "Edit Favorite"
    var editingId by remember { mutableStateOf<Long?>(null) }
    var editingName by remember { mutableStateOf("") }
    var editingLat by remember { mutableStateOf("") }
    var editingLng by remember { mutableStateOf("") }

    var deleteTarget by remember { mutableStateOf<FavItem?>(null) }

    val list = if (tab == FavTab.GRB) grbFavs else gjkFavs

    fun cancelEdit() {
        editingId = null
        editingName = ""
        editingLat = ""
        editingLng = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Favorite", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // ===== Baris tab 1: GRB / GJK =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FavTabChip(
                        label = "GRB",
                        active = tab == FavTab.GRB,
                        activeColor = GRB_RED,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            tab = FavTab.GRB
                            cancelEdit()
                            FavStore.saveLastTab(prefs, FavTab.GRB)
                        }
                    )
                    FavTabChip(
                        label = "GJK",
                        active = tab == FavTab.GJK,
                        activeColor = GJK_BLUE,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            tab = FavTab.GJK
                            cancelEdit()
                            FavStore.saveLastTab(prefs, FavTab.GJK)
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ===== Baris tab 2: Dari Pin / Manual =====
                // Warna aktif mengikuti tab GRB/GJK yang dipilih:
                // GRB -> merah, GJK -> biru
                val sectionActiveColor = when (tab) {
                    FavTab.GRB -> GRB_RED
                    FavTab.GJK -> GJK_BLUE
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FavTabChip(
                        label = "Dari Pin",
                        active = section == "PIN",
                        activeColor = sectionActiveColor,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            section = "PIN"
                            FavStore.saveLastSection(prefs, "PIN")
                        }
                    )
                    FavTabChip(
                        label = "Manual",
                        active = section == "MANUAL",
                        activeColor = sectionActiveColor,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            section = "MANUAL"
                            FavStore.saveLastSection(prefs, "MANUAL")
                        }
                    )
                }

                Spacer(Modifier.height(10.dp))

                if (editingId != null) {
                    // ================= Edit Favorite =================
                    Text(
                        text = "Edit Favorite",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = editingName,
                        onValueChange = { editingName = it },
                        label = { Text("Nama Favorite") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = editingLat,
                        onValueChange = { editingLat = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = editingLng,
                        onValueChange = { editingLng = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            val lat = editingLat.trim().toDoubleOrNull()
                            val lng = editingLng.trim().toDoubleOrNull()
                            if (editingName.isBlank() || lat == null || lng == null ||
                                lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0
                            ) {
                                Toast.makeText(
                                    context,
                                    "Data tidak valid. Periksa nama & koordinat.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                onUpdate(tab, editingId!!, editingName, lat, lng)
                                cancelEdit()
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Update") }
                } else if (section == "PIN") {
                    // ================= Form: Dari Pin =================
                    OutlinedTextField(
                        value = pinName,
                        onValueChange = { pinName = it },
                        label = { Text("Nama Favorite") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    // Latitude OTOMATIS dari pin tengah (read-only)
                    OutlinedTextField(
                        value = String.format(Locale.US, "%.6f", currentPin.latitude),
                        onValueChange = {},
                        label = { Text("Latitude") },
                        readOnly = true,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    // Longitude OTOMATIS dari pin tengah (read-only)
                    OutlinedTextField(
                        value = String.format(Locale.US, "%.6f", currentPin.longitude),
                        onValueChange = {},
                        label = { Text("Longitude") },
                        readOnly = true,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            onAdd(tab, pinName, currentPin)
                            pinName = ""
                        },
                        enabled = pinName.isNotBlank(),
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Simpan") }
                } else {
                    // ================= Form: Manual =================
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text("Nama Favorite") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = manualLat,
                        onValueChange = { manualLat = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = manualLng,
                        onValueChange = { manualLng = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            val lat = manualLat.trim().toDoubleOrNull()
                            val lng = manualLng.trim().toDoubleOrNull()
                            if (manualName.isBlank() || lat == null || lng == null ||
                                lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0
                            ) {
                                Toast.makeText(
                                    context,
                                    "Data tidak valid. Periksa nama & koordinat.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                onAdd(tab, manualName, LatLng(lat, lng))
                                manualName = ""
                                manualLat = ""
                                manualLng = ""
                            }
                        },
                        enabled = manualName.isNotBlank() &&
                            manualLat.isNotBlank() && manualLng.isNotBlank(),
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Simpan") }
                }

                HorizontalDivider()

                Spacer(Modifier.height(8.dp))

                // ===== Daftar Favorite =====
                Text(
                    text = "Daftar Favorite (${list.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))

                if (list.isEmpty()) {
                    Text(
                        text = "Belum ada favorite di tab ini",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        list.forEach { item ->
                            FavRow(
                                item = item,
                                onEdit = {
                                    editingId = item.id
                                    editingName = item.name
                                    editingLat = String.format(Locale.US, "%.6f", item.lat)
                                    editingLng = String.format(Locale.US, "%.6f", item.lng)
                                },
                                onDelete = { deleteTarget = item },
                                onClick = { onFlyTo(LatLng(item.lat, item.lng)) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )

    // ===== Dialog konfirmasi hapus =====
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus favorite?") },
            text = { Text("\"${target.name}\" akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(tab, target.id)
                    deleteTarget = null
                }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun FavTabChip(
    label: String,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (active) activeColor else MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 1.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun FavRow(
    item: FavItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatLatLng(LatLng(item.lat, item.lng)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = "Edit ${item.name}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = "Hapus ${item.name}",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
