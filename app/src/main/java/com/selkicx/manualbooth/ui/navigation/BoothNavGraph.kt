package com.selkicx.manualbooth.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.selkicx.manualbooth.di.AppContainer
import com.selkicx.manualbooth.ui.activesession.ActiveSessionScreen
import com.selkicx.manualbooth.ui.admin.AdminScreen
import com.selkicx.manualbooth.ui.admin.QrSettingsScreen
import com.selkicx.manualbooth.ui.admin.templates.AddTemplateScreen
import com.selkicx.manualbooth.ui.admin.templates.PhotoHolderEditorScreen
import com.selkicx.manualbooth.ui.admin.templates.TemplateFoldersScreen
import com.selkicx.manualbooth.ui.admin.templates.TemplateListScreen
import com.selkicx.manualbooth.ui.history.SessionDetailScreen
import com.selkicx.manualbooth.ui.history.SessionHistoryScreen
import com.selkicx.manualbooth.ui.home.HomeScreen
import com.selkicx.manualbooth.ui.newsession.NewSessionScreen

@Composable
fun BoothNavGraph(navController: NavHostController, appContainer: AppContainer) {
    NavHost(navController = navController, startDestination = Routes.Home.route) {
        composable(Routes.Home.route) {
            HomeScreen(
                appContainer = appContainer,
                onNewSession = { navController.navigate(Routes.NewSession.route) },
                onOpenAdmin = { navController.navigate(Routes.Admin.route) },
                onOpenSession = { sessionId -> navController.navigate(Routes.SessionDetail.build(sessionId)) }
            )
        }
        composable(Routes.NewSession.route) {
            NewSessionScreen(
                appContainer = appContainer,
                onSessionStarted = { sessionId ->
                    navController.navigate(Routes.ActiveSession.build(sessionId)) {
                        popUpTo(Routes.Home.route)
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.ActiveSession.route,
            arguments = listOf(navArgument(ARG_SESSION_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong(ARG_SESSION_ID) ?: -1L
            ActiveSessionScreen(
                appContainer = appContainer,
                sessionId = sessionId,
                onNextCustomer = { nextSessionId ->
                    navController.navigate(Routes.ActiveSession.build(nextSessionId)) {
                        popUpTo(Routes.Home.route)
                    }
                },
                onHome = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Home.route) { inclusive = true }
                    }
                },
                onViewSessionDetail = { id -> navController.navigate(Routes.SessionDetail.build(id)) }
            )
        }

        composable(Routes.Admin.route) {
            AdminScreen(
                onOpenTemplates = { navController.navigate(Routes.AdminTemplateFolders.route) },
                onOpenHistory = { navController.navigate(Routes.History.route) },
                onOpenQrSettings = { navController.navigate(Routes.AdminQrSettings.route) }
            )
        }
        composable(Routes.AdminQrSettings.route) {
            QrSettingsScreen(appContainer = appContainer)
        }
        composable(Routes.AdminTemplateFolders.route) {
            TemplateFoldersScreen(
                appContainer = appContainer,
                onOpenFolder = { folder -> navController.navigate(Routes.AdminTemplateList.build(folder.id)) }
            )
        }
        composable(
            route = Routes.AdminTemplateList.route,
            arguments = listOf(navArgument(ARG_FOLDER_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong(ARG_FOLDER_ID) ?: -1L
            TemplateListScreen(
                appContainer = appContainer,
                folderId = folderId,
                onAddTemplate = { navController.navigate(Routes.AdminAddTemplate.build(folderId)) },
                onOpenTemplate = { templateId -> navController.navigate(Routes.AdminHolderEditor.build(templateId)) }
            )
        }
        composable(
            route = Routes.AdminAddTemplate.route,
            arguments = listOf(navArgument(ARG_FOLDER_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong(ARG_FOLDER_ID) ?: -1L
            AddTemplateScreen(
                appContainer = appContainer,
                folderId = folderId,
                onTemplateCreated = { templateId ->
                    navController.navigate(Routes.AdminHolderEditor.build(templateId)) {
                        popUpTo(Routes.AdminTemplateList.build(folderId))
                    }
                }
            )
        }
        composable(
            route = Routes.AdminHolderEditor.route,
            arguments = listOf(navArgument(ARG_TEMPLATE_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getLong(ARG_TEMPLATE_ID) ?: -1L
            PhotoHolderEditorScreen(
                appContainer = appContainer,
                templateId = templateId,
                onSaved = { navController.popBackStack(Routes.AdminTemplateFolders.route, inclusive = false) }
            )
        }

        composable(Routes.History.route) {
            SessionHistoryScreen(
                appContainer = appContainer,
                onOpenSession = { sessionId -> navController.navigate(Routes.SessionDetail.build(sessionId)) }
            )
        }
        composable(
            route = Routes.SessionDetail.route,
            arguments = listOf(navArgument(ARG_SESSION_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong(ARG_SESSION_ID) ?: -1L
            SessionDetailScreen(appContainer = appContainer, sessionId = sessionId)
        }
    }
}
