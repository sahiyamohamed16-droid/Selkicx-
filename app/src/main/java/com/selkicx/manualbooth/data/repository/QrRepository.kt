package com.selkicx.manualbooth.data.repository

import com.selkicx.manualbooth.data.local.dao.ShareLinkDao
import com.selkicx.manualbooth.data.local.entity.ShareLinkEntity
import java.security.SecureRandom

/**
 * QR share links (spec sections 42-46). A link is only ever created when
 * the photographer explicitly taps QR (spec rules #25-26) - nothing here
 * runs automatically. Uses a secure random token rather than a
 * sequential id (spec section 46: "do not expose simple sequential
 * public URLs"), and belongs to the session/cloud record, not the phone.
 *
 * The share domain below is a placeholder: resolving it into "all
 * session originals + Final Output" (spec rule #27) is the real SelkicX
 * backend's job (spec section 46), which is out of scope for this client
 * scaffold. This repository's contract is what that backend needs from
 * the client: a stable per-session token.
 */
class QrRepository(private val shareLinkDao: ShareLinkDao) {

    suspend fun getOrCreateShareLink(sessionId: Long): ShareLinkEntity {
        shareLinkDao.getLatestForSession(sessionId)?.let { return it }
        val token = generateSecureToken()
        val id = shareLinkDao.upsert(ShareLinkEntity(sessionId = sessionId, token = token))
        return ShareLinkEntity(id = id, sessionId = sessionId, token = token)
    }

    fun shareUrl(shareLink: ShareLinkEntity): String = "$SHARE_BASE_URL${shareLink.token}"

    private fun generateSecureToken(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        // Placeholder domain - the real SelkicX share endpoint isn't wired up yet.
        const val SHARE_BASE_URL = "https://share.selkicx.example/s/"
    }
}
