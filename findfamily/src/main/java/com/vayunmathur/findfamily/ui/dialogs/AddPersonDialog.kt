package com.vayunmathur.findfamily.ui.dialogs
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import com.vayunmathur.library.util.NavBackStack
import com.vayunmathur.findfamily.util.FindFamilyViewModel
import com.vayunmathur.findfamily.util.Networking
import com.vayunmathur.findfamily.util.Platform
import com.vayunmathur.findfamily.Route
import com.vayunmathur.findfamily.R
import com.vayunmathur.findfamily.data.RequestStatus
import com.vayunmathur.findfamily.data.User
import com.vayunmathur.library.ui.IconCopy
import com.vayunmathur.library.ui.IconShare
import kotlin.time.Clock

@Composable
fun AddPersonDialog(
    backStack: NavBackStack<Route>,
    ffViewModel: FindFamilyViewModel,
    platform: Platform,
    id: Long?,
) {
    val usersByID by ffViewModel.usersById.collectAsState()
    val myUserId by ffViewModel.selfUserId.collectAsState()

    var userid: String by remember { mutableStateOf(id?.encodeBase26() ?: "") }
    var contactName: String? by remember { mutableStateOf(null) }
    var contactPhoto by remember { mutableStateOf<String?>(null) }
    val myId = myUserId.encodeBase26()
    val shareMyIdText = stringResource(R.string.share_my_id_message, myId)
    val requestPickContact2 = platform.requestPickContact { name, photo ->
        contactName = name
        contactPhoto = photo
    }

    val userStatus = usersByID[userid.decodeBase26()]?.requestStatus

    Dialog({backStack.pop()}) {
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.add_person_title), style = MaterialTheme.typography.headlineMedium)

                OutlinedTextField(
                    myId,
                    {},
                    interactionSource = interactionSourceClickable {
                        platform.copy(myId)
                    },
                    label = { Text(stringResource(R.string.your_findfamily_id)) },
                    trailingIcon = {
                        IconCopy()
                    },
                    readOnly = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ platform.copy(myId) }, enabled = myId.isNotBlank()) {
                        Text(stringResource(R.string.copy_id))
                    }
                    OutlinedButton({ platform.shareText(shareMyIdText) }, enabled = myId.isNotBlank()) {
                        IconShare()
                        Text(stringResource(R.string.share_my_id))
                    }
                }

                OutlinedTextField(
                    userid,
                    { userid = sanitizeFindFamilyId(it) },
                    readOnly = id != null,
                    label = { Text(stringResource(R.string.contact_findfamily_id)) },
                    isError = userStatus == RequestStatus.MUTUAL_CONNECTION || userStatus == RequestStatus.AWAITING_RESPONSE,
                    supportingText =
                        when (userStatus) {
                            RequestStatus.AWAITING_REQUEST -> { @Composable {Text(stringResource(R.string.status_awaiting_request))}}
                            RequestStatus.MUTUAL_CONNECTION -> {
                                if (userid == myId)
                                    {@Composable {Text(stringResource(R.string.status_cannot_share_self))}}
                                else {@Composable {Text(stringResource(R.string.status_already_sharing))}}
                            }
                            RequestStatus.AWAITING_RESPONSE -> {@Composable {Text(stringResource(R.string.status_already_requested)) } }
                            else -> null
                        }
                    )

                if (id == null) {
                    OutlinedButton({
                        platform.readClipboardText()?.let { pasted ->
                            userid = extractFindFamilyIdCandidate(pasted)
                        }
                    }) {
                        Text(stringResource(R.string.paste_from_clipboard))
                    }
                }

                OutlinedTextField(contactName ?: "", {}, interactionSource = interactionSourceClickable {
                    requestPickContact2()
                }, label = { Text(stringResource(R.string.contact_name_label)) }, readOnly = true)

                Button(
                    {
                        val userToAdd = User(
                            contactName!!,
                            contactPhoto,
                            "Unknown Location",
                            true,
                            if (userStatus == RequestStatus.AWAITING_REQUEST) RequestStatus.MUTUAL_CONNECTION else RequestStatus.AWAITING_RESPONSE,
                            Clock.System.now(),
                            null,
                            userid.decodeBase26()
                        )
                        ffViewModel.upsertUser(userToAdd) {
                            backStack.pop()
                        }
                    },
                    enabled = userid.isNotBlank() && contactName != null && !(userStatus == RequestStatus.MUTUAL_CONNECTION || userStatus == RequestStatus.AWAITING_RESPONSE)
                ) {
                    if (userStatus == RequestStatus.AWAITING_REQUEST) {
                        Text(stringResource(R.string.accept_location_request))
                    } else {
                        Text(stringResource(R.string.request_location))
                    }
                }
            }
        }
    }
}

private fun sanitizeFindFamilyId(value: String): String =
    value.uppercase().filter { it in 'A'..'Z' }

private fun extractFindFamilyIdCandidate(value: String): String =
    Regex("[A-Za-z]+").findAll(value)
        .map { it.value.uppercase() }
        .toList()
        .lastOrNull()
        .orEmpty()

fun String.decodeBase26(): Long = sanitizeFindFamilyId(this).fold(0uL) { acc, c ->
    acc * 26uL + (c.code - 65).toULong()
}.toLong()

fun Long.encodeBase26(): String = buildString {
    var remaining = this@encodeBase26.toULong()
    while (remaining > 0uL) {
        insert(0, ((remaining % 26uL) + 65uL).toInt().toChar())
        remaining /= 26uL
    }
}

@Composable
fun interactionSourceClickable(onClick: () -> Unit): MutableInteractionSource {
    return remember { MutableInteractionSource() }
        .also { interactionSource ->
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect {
                    if (it is PressInteraction.Release) {
                        onClick()
                    }
                }
            }
        }
}
