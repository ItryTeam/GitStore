package app.gitstore.tg.auth

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import app.gitstore.tg.Config
import app.gitstore.tg.data.UserInfo
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

object GoogleAuth {

    suspend fun signIn(activity: Activity): UserInfo {
        val option = GetSignInWithGoogleOption.Builder(Config.WEB_CLIENT_ID).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = CredentialManager.create(activity).getCredential(activity, request)
        val cred = result.credential

        if (cred is CustomCredential &&
            cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val g = GoogleIdTokenCredential.createFrom(cred.data)
            return UserInfo(g.displayName ?: g.id, g.id, g.profilePictureUri?.toString())
        }
        error("Неподдерживаемый тип входа")
    }

    suspend fun signOut(activity: Activity) {
        CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())
    }
}
