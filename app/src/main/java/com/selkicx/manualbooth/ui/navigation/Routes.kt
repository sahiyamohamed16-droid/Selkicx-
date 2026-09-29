package com.selkicx.manualbooth.ui.navigation

const val ARG_SESSION_ID = "sessionId"
const val ARG_FOLDER_ID = "folderId"
const val ARG_TEMPLATE_ID = "templateId"

/** Minimal navigation (spec section 4: no unnecessary page levels). */
sealed class Routes(val route: String) {
    object Home : Routes("home")
    object NewSession : Routes("new_session")

    object ActiveSession : Routes("active_session/{$ARG_SESSION_ID}") {
        fun build(sessionId: Long) = "active_session/$sessionId"
    }

    object Admin : Routes("admin")

    /** Admin > Templates (spec sections 24-28). */
    object AdminTemplateFolders : Routes("admin/templates")
    object AdminTemplateList : Routes("admin/templates/{$ARG_FOLDER_ID}") {
        fun build(folderId: Long) = "admin/templates/$folderId"
    }
    object AdminAddTemplate : Routes("admin/templates/{$ARG_FOLDER_ID}/add") {
        fun build(folderId: Long) = "admin/templates/$folderId/add"
    }
    object AdminHolderEditor : Routes("admin/templates/edit/{$ARG_TEMPLATE_ID}") {
        fun build(templateId: Long) = "admin/templates/edit/$templateId"
    }

    /** Admin > QR Sharing toggle (spec section 42). */
    object AdminQrSettings : Routes("admin/qr")

    /** Session History (spec section 48). */
    object History : Routes("history")
    object SessionDetail : Routes("history/{$ARG_SESSION_ID}") {
        fun build(sessionId: Long) = "history/$sessionId"
    }
}
