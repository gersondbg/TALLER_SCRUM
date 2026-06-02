package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.*
import com.example.ui.viewmodel.MoodleViewModel
import com.example.ui.viewmodel.SyncState
import com.example.ui.viewmodel.TestConnectionState
import kotlinx.coroutines.flow.collectLatest
import java.util.*

// Redefine to a gorgeous, premium Light Tech/Education vibe (Fondo claro y profesional)
val SlateBackground = Color(0xFFF1F5F9) // Beautiful Light Slate 50
val SlateSurface = Color(0xFFFFFFFF) // Crisp solid White
val SlatePrimary = Color(0xFF2563EB) // Bright energetic Royal/Tech Blue
val SlateSecondary = Color(0xFF4F46E5) // Clean Indigo
val SlateAccent = Color(0xFFF43F5E) // Vibrant Alert Pink/Rose
val SlateTextPrimary = Color(0xFF0F172A) // Dark Slate 900 (High contrast text)
val SlateTextSecondary = Color(0xFF64748B) // Subtle Slate 500 (Secondary description)
val SlateBorder = Color(0xFFE2E8F0) // Clean Slate 200 border lines

enum class AuthScreen {
    WELCOME,
    LOGIN,
    REGISTER,
    FORGOT_PASSWORD
}

sealed class AppScreen(val route: String, val title: String, val iconFilled: ImageVector, val iconOutlined: ImageVector) {
    object Dashboard : AppScreen("dashboard", "Inicio", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object Evaluations : AppScreen("evaluations", "Alertas", Icons.Filled.NotificationsActive, Icons.Outlined.NotificationsActive)
    object Payments : AppScreen("payments", "Pagos", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
    object Academic : AppScreen("academic", "Cambios", Icons.Filled.Event, Icons.Outlined.Event)
    object Settings : AppScreen("settings", "Perfil", Icons.Filled.Person, Icons.Outlined.Person)
    object CoursesList : AppScreen("courses_list", "Cursos", Icons.Filled.Class, Icons.Outlined.Class)
    object Schedule : AppScreen("schedule", "Horario", Icons.Filled.Schedule, Icons.Outlined.Schedule)
    object Calendar : AppScreen("calendar", "Calendario", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
}

@Composable
fun AuthNavigationWrapper(
    viewModel: MoodleViewModel,
    config: MoodleConfig?,
    onAuthSuccess: () -> Unit
) {
    var screenState by remember { mutableStateOf(AuthScreen.WELCOME) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
    ) {
        AnimatedContent(
            targetState = screenState,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "AuthTransitions"
        ) { currentScreen ->
            when (currentScreen) {
                AuthScreen.WELCOME -> {
                    WelcomeScreen(
                        onLoginClick = { screenState = AuthScreen.LOGIN },
                        onRegisterClick = { screenState = AuthScreen.REGISTER }
                    )
                }
                AuthScreen.LOGIN -> {
                    LoginScreen(
                        viewModel = viewModel,
                        config = config,
                        onBack = { screenState = AuthScreen.WELCOME },
                        onForgotPassword = { screenState = AuthScreen.FORGOT_PASSWORD },
                        onRegisterClick = { screenState = AuthScreen.REGISTER }
                    )
                }
                AuthScreen.REGISTER -> {
                    RegisterScreen(
                        viewModel = viewModel,
                        onBack = { screenState = AuthScreen.WELCOME },
                        onLoginClick = { screenState = AuthScreen.LOGIN }
                    )
                }
                AuthScreen.FORGOT_PASSWORD -> {
                    ForgotPasswordScreen(
                        viewModel = viewModel,
                        onBack = { screenState = AuthScreen.LOGIN }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: MoodleViewModel,
    modifier: Modifier = Modifier
) {
    val configState by viewModel.config.collectAsStateWithLifecycle()
    val isLoggedIn = configState?.isLoggedIn ?: false

    if (!isLoggedIn) {
        AuthNavigationWrapper(
            viewModel = viewModel,
            config = configState,
            onAuthSuccess = {}
        )
    } else {
        val navController = rememberNavController()
        val context = LocalContext.current
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: AppScreen.Dashboard.route
        val inAppNotification by viewModel.inAppNotification.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        // Collect viewmodel toast events
        LaunchedEffect(key1 = true) {
            viewModel.toastEvent.collectLatest { msg ->
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )
            }
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (currentRoute) {
                                AppScreen.Dashboard.route -> "Panel de Control"
                                AppScreen.Evaluations.route -> "Evaluaciones"
                                AppScreen.Payments.route -> "Pagos y Deudas"
                                AppScreen.Academic.route -> "Historial Académico"
                                AppScreen.Settings.route -> "Configuración"
                                AppScreen.CoursesList.route -> "Mis Cursos"
                                AppScreen.Schedule.route -> "Horario"
                                AppScreen.Calendar.route -> "Calendario"
                                else -> "Taller Scrum"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = SlateSurface,
                        titleContentColor = SlateTextPrimary
                    ),
                    actions = {
                        IconButton(
                            onClick = { viewModel.logoutStudent() },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(SlateAccent.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Cerrar Sesión",
                                tint = SlateAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    navigationIcon = {
                        if (currentRoute != AppScreen.Dashboard.route) {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Volver",
                                    tint = SlatePrimary
                                )
                            }
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = SlateSurface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    val screens = listOf(
                        AppScreen.Dashboard,
                        AppScreen.Evaluations,
                        AppScreen.Payments,
                        AppScreen.Academic,
                        AppScreen.Settings
                    )
                    screens.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.iconFilled else screen.iconOutlined,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) SlatePrimary else SlateTextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    color = if (isSelected) SlatePrimary else SlateTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = SlatePrimary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SlateBackground)
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                // Connection Banner (Show if moodle is config placeholder or URL is default)
                val isPlaceholder = configState?.wsToken == "abc123tokenexample789" || configState?.moodleUrl == "https://aistudio.moodlecloud.com"
                if (isPlaceholder) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSecondary.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, brush = Brush.linearGradient(listOf(SlateSecondary, SlatePrimary))),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Modo Demo",
                                tint = SlatePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Modo Demo de Simulación",
                                    color = SlateTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Usando base de datos interna (Room). Pulsa para configurar una conexión real a Moodle Cloud.",
                                    color = SlateTextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { navController.navigate(AppScreen.Settings.route) },
                                colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Ajustar", color = SlateBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Nav Host
                NavHost(
                    navController = navController,
                    startDestination = AppScreen.Dashboard.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(AppScreen.Dashboard.route) {
                        DashboardScreen(viewModel, navController)
                    }
                    composable(AppScreen.Evaluations.route) {
                        EvaluationsScreen(viewModel)
                    }
                    composable(AppScreen.Payments.route) {
                        PaymentsScreen(viewModel)
                    }
                    composable(AppScreen.Academic.route) {
                        AcademicScreen(viewModel)
                    }
                    composable(AppScreen.Settings.route) {
                        SettingsScreen(viewModel)
                    }
                    composable(AppScreen.CoursesList.route) {
                        CoursesScreen(viewModel, navController)
                    }
                    composable(AppScreen.Schedule.route) {
                        ScheduleScreen(viewModel, navController)
                    }
                    composable(AppScreen.Calendar.route) {
                        CalendarScreen(viewModel, navController)
                    }
                }
            }

            // High priority custom In-App Notification overlay (H10 received tester)
            AnimatedVisibility(
                visible = inAppNotification != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                inAppNotification?.let { notif ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.dismissInAppNotification() },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E212D)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(
                                colors = when (notif.type) {
                                    "ALERT" -> listOf(SlateAccent, Color.Yellow)
                                    "PAYMENT" -> listOf(Color.Yellow, SlatePrimary)
                                    else -> listOf(SlatePrimary, SlateSecondary)
                                }
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (notif.type) {
                                        "ALERT" -> Icons.Default.WarningAmber
                                        "PAYMENT" -> Icons.Default.Payments
                                        else -> Icons.Default.School
                                    },
                                    contentDescription = null,
                                    tint = when (notif.type) {
                                        "ALERT" -> SlateAccent
                                        "PAYMENT" -> Color.Yellow
                                        else -> SlatePrimary
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = notif.title,
                                    color = SlateTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Ahora",
                                    color = SlateTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = notif.message,
                                color = SlateTextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { viewModel.dismissInAppNotification() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = SlatePrimary)
                                ) {
                                    Text("Entendido", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// FORMAT TIME HELPER
fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    return DateFormat.format("dd/MM/yyyy hh:mm a", date).toString()
}

// -------------------------------------------------------------
// SCREEN 1: DASHBOARD
// -------------------------------------------------------------
@Composable
fun DashboardScreen(
    viewModel: MoodleViewModel,
    navController: NavHostController
) {
    val configState by viewModel.config.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val evaluations by viewModel.evaluations.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val academicChanges by viewModel.academicChanges.collectAsStateWithLifecycle()

    val pendingEvals = remember(evaluations) { evaluations.filter { it.status == "PENDIENTE" } }
    val pendingPayments = remember(payments) { payments.filter { it.status == "PENDIENTE" } }

    // Sincronización automática al entrar al dashboard si no hay datos
    LaunchedEffect(configState) {
        if (configState != null && courses.isEmpty()) {
            viewModel.triggerSync()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Welcoming header with dynamic student profile information
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "¡Hola, ${configState?.studentName ?: "Estudiante"}!",
                        color = SlateTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = configState?.studentEmail ?: "Conectado a Moodle Cloud",
                        color = SlateTextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Sincronización 100% Automática (H10): Botón manual eliminado
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateSurface),
                    contentAlignment = Alignment.Center
                ) {
                    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
                    if (syncState is SyncState.Syncing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SlatePrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Estado: Sincronizado",
                            tint = Color(0xFF10B981).copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Quick Stats row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Pendientes", color = SlateTextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${pendingEvals.size} Tareas",
                            color = SlateAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Cuotas Obligación", color = SlateTextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${pendingPayments.size} Pagos",
                            color = Color.Yellow,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Quick Actions
        item {
            Column {
                Text(
                    text = "Accesos Rápidos",
                    color = SlateTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    QuickActionCard(
                        title = "Cursos",
                        icon = Icons.Default.Class,
                        color = SlatePrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(AppScreen.CoursesList.route) }
                    )
                    QuickActionCard(
                        title = "Horario",
                        icon = Icons.Default.Schedule,
                        color = SlateSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(AppScreen.Schedule.route) }
                    )
                    QuickActionCard(
                        title = "Calendario",
                        icon = Icons.Default.CalendarMonth,
                        color = SlateAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(AppScreen.Calendar.route) }
                    )
                }
            }
        }

        // Moodle Course Listing
        item {
            Text(
                text = "Tus Cursos de Moodle (${courses.size})",
                color = SlateTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        if (courses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(SlateSurface, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
                    Text(
                        if (syncState is SyncState.Syncing) "Sincronizando con Moodle Cloud..." 
                        else "No se encontraron cursos. Asegúrate de tener conexión.",
                        color = SlateTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(courses) { course ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateSecondary.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = SlatePrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = course.fullname,
                                color = SlateTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${course.shortname} • ${course.defaultRoom}",
                                color = SlateTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Quick academic changes bulletin
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Últimos Cambios de Aula y Horarios",
                    color = SlateTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { navController.navigate(AppScreen.Academic.route) }) {
                    Text("Ver Todo", color = SlatePrimary, fontSize = 12.sp)
                }
            }
        }

        if (academicChanges.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay cambios de horarios reportados por el centro.",
                            color = SlateTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(academicChanges.take(2)) { change ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateSecondary.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, brush = Brush.linearGradient(listOf(SlateSecondary.copy(alpha = 0.6f), Color.Transparent)))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = SlatePrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = change.courseName,
                                color = SlateTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Traslado de salón: ${change.oldRoom} ➔ ${change.newRoom}",
                            color = SlateTextPrimary,
                            fontSize = 11.sp
                        )
                        if (change.oldSchedule != change.newSchedule) {
                            Text(
                                text = "Horario: ${change.newSchedule}",
                                color = SlateAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, SlateBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = SlateTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// -------------------------------------------------------------
// SCREEN 2: EVALUATIONS (H7)
// -------------------------------------------------------------
@Composable
fun EvaluationsScreen(viewModel: MoodleViewModel) {
    val evaluations by viewModel.evaluations.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }

    // Dialog state variables
    var examTitle by remember { mutableStateOf("") }
    var selectedCourseId by remember { mutableStateOf(if (courses.isNotEmpty()) courses.first().id else 101) }
    var inputDaysFromNow by remember { mutableStateOf("2") }
    var descriptionText by remember { mutableStateOf("") }
    var triggerAlert by remember { mutableStateOf(true) }
    var minutesBefore by remember { mutableStateOf("15") }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = SlatePrimary,
                contentColor = SlateBackground
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Registrar Examen")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Alertas y Evaluaciones",
                        color = SlateTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Planificador de control académico (H7)",
                        color = SlateTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { viewModel.testAlertReception("ALERT") },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateSecondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.CellTower, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Probar Recepción", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (evaluations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SlatePrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No hay evaluaciones registradas.", color = SlateTextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(evaluations) { eval ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (eval.dueDate < System.currentTimeMillis() && eval.status == "PENDIENTE") {
                                    SlateAccent.copy(alpha = 0.08f)
                                } else {
                                    SlateSurface
                                }
                            ),
                            border = if (eval.dueDate < System.currentTimeMillis() && eval.status == "PENDIENTE") {
                                BorderStroke(1.dp, brush = Brush.linearGradient(listOf(SlateAccent, Color.Transparent)))
                            } else null
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = eval.courseName,
                                            color = SlatePrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = eval.title,
                                            color = SlateTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteEvaluation(eval.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Borrar",
                                            tint = SlateTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (eval.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = eval.description,
                                        color = SlateTextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = SlateBorder)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = if (eval.dueDate < System.currentTimeMillis()) SlateAccent else SlateTextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = formatTimestamp(eval.dueDate),
                                            color = if (eval.dueDate < System.currentTimeMillis()) SlateAccent else SlateTextPrimary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    if (eval.isAlertConfigured) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("Alerta ${eval.alertTimeMinutesBefore}m", fontSize = 10.sp, color = SlatePrimary) },
                                            leadingIcon = { Icon(Icons.Default.Alarm, null, modifier = Modifier.size(10.dp), tint = SlatePrimary) },
                                            colors = AssistChipDefaults.assistChipColors(containerColor = SlateSecondary.copy(alpha = 0.2f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CREATE DIALOG FOR NEW EVALUATION / EXAM (H7)
        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Registrar Evaluación (H7)",
                            color = SlateTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = examTitle,
                            onValueChange = { examTitle = it },
                            label = { Text("Título de la evaluación") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SlatePrimary,
                                unfocusedBorderColor = SlateBorder,
                                focusedLabelColor = SlatePrimary,
                                focusedTextColor = SlateTextPrimary,
                                unfocusedTextColor = SlateTextPrimary
                            )
                        )

                        // Course selection mockup drop downs
                        Text("Selecciona el Curso:", color = SlateTextSecondary, fontSize = 11.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            courses.forEach { course ->
                                val isSelected = selectedCourseId == course.id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) SlatePrimary else SlateBorder)
                                        .clickable { selectedCourseId = course.id }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = course.shortname ?: course.fullname,
                                        color = if (isSelected) SlateBackground else SlateTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = inputDaysFromNow,
                            onValueChange = { inputDaysFromNow = it },
                            label = { Text("¿En cuántos días vence?") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SlatePrimary,
                                focusedTextColor = SlateTextPrimary,
                                unfocusedTextColor = SlateTextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = descriptionText,
                            onValueChange = { descriptionText = it },
                            label = { Text("Temario / Descripción (Opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SlatePrimary,
                                focusedTextColor = SlateTextPrimary,
                                unfocusedTextColor = SlateTextPrimary
                            )
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = triggerAlert,
                                onCheckedChange = { triggerAlert = it },
                                colors = CheckboxDefaults.colors(checkedColor = SlatePrimary)
                            )
                            Text("Configurar Alerta", color = SlateTextPrimary, fontSize = 13.sp)
                        }

                        if (triggerAlert) {
                            OutlinedTextField(
                                value = minutesBefore,
                                onValueChange = { minutesBefore = it },
                                label = { Text("Tiempo de alerta (Minutos antes)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SlatePrimary,
                                    focusedTextColor = SlateTextPrimary,
                                    unfocusedTextColor = SlateTextPrimary
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Cancelar", color = SlateTextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val matchCourse = courses.find { it.id == selectedCourseId }
                                    val nameOfCourse = matchCourse?.fullname ?: "Curso Desconocido"
                                    val days = inputDaysFromNow.toLongOrNull() ?: 2L
                                    val mins = minutesBefore.toIntOrNull() ?: 15
                                    val dueTimestamp = System.currentTimeMillis() + (days * 24L * 60L * 60L * 1000L)

                                    if (examTitle.isNotBlank()) {
                                        viewModel.addEvaluation(
                                            title = examTitle,
                                            courseId = selectedCourseId,
                                            courseName = nameOfCourse,
                                            dueDate = dueTimestamp,
                                            description = descriptionText,
                                            isAlertConfigured = triggerAlert,
                                            alertTimeMinutesBefore = mins
                                        )
                                        showDialog = false
                                        // Reset
                                        examTitle = ""
                                        descriptionText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                            ) {
                                Text("Registrar", color = SlateBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 3: PAYMENTS & MATRICULA (H8)
// -------------------------------------------------------------
@Composable
fun PaymentsScreen(viewModel: MoodleViewModel) {
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf(0) } // 0 = Pagos & Matrículas, 1 = Fechas Administrativas

    var showAddDialog by remember { mutableStateOf(false) }
    var showAddAdminEventDialog by remember { mutableStateOf(false) }

    // Forms fields for payments
    var payConcept by remember { mutableStateOf("") }
    var payAmount by remember { mutableStateOf("280.00") }
    var payDaysFromNow by remember { mutableStateOf("7") }
    var payTypeSelected by remember { mutableStateOf("PAGO") } // PAGO, MATRICULA, EXAMEN

    // Forms fields for administrative events
    var eventTitle by remember { mutableStateOf("") }
    var eventDesc by remember { mutableStateOf("") }
    var eventDaysFromNow by remember { mutableStateOf("10") }

    // Handle payments simulated credit card action
    var showPaymentDialog by remember { mutableStateOf<Payment?>(null) }
    var inputTxnRef by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    if (activeTab == 0) {
                        showAddDialog = true 
                    } else {
                        showAddAdminEventDialog = true
                    }
                },
                containerColor = SlatePrimary,
                contentColor = SlateBackground
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Registrar")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (activeTab == 0) "Pagos y Matrículas" else "Fechas Administrativas",
                        color = SlateTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (activeTab == 0) "Gestión de boletas, tasas y matrículas" else "Cronograma de hitos institucionales",
                        color = SlateTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { 
                        if (activeTab == 0) viewModel.testAlertReception("PAYMENT") 
                        else viewModel.testAlertReception("ALERT") 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateSecondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.CellTower, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Avisos", fontSize = 11.sp)
                }
            }

            // Tabs navigation layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SlateBorder)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == 0) SlateSurface else Color.Transparent)
                        .clickable { activeTab = 0 }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Boletas Financieras",
                        color = if (activeTab == 0) SlatePrimary else SlateTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == 1) SlateSurface else Color.Transparent)
                        .clickable { activeTab = 1 }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Calendario Académico",
                        color = if (activeTab == 1) SlatePrimary else SlateTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (activeTab == 0) {
                // TAB 0: PAYMENTS & ENROLLMENTS LIST
                if (payments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No se cargaron obligaciones de pago.", color = SlateTextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(payments) { pay ->
                            val isOverdue = pay.dueDate < System.currentTimeMillis() && pay.status == "PENDIENTE"
                            val isMatricula = pay.concept.startsWith("[MATRÍCULA]") || pay.concept.contains("Matrícula")
                            
                            val statusText = when {
                                pay.status == "PAGADO" -> "PAGADO"
                                isOverdue -> "VENCIDO"
                                else -> "PENDIENTE"
                            }
                            val statusBgColor = when {
                                pay.status == "PAGADO" -> Color(0xFF10B981).copy(alpha = 0.12f)
                                isOverdue -> SlateAccent.copy(alpha = 0.12f)
                                else -> Color(0xFFEAB308).copy(alpha = 0.12f)
                            }
                            val statusTextColor = when {
                                pay.status == "PAGADO" -> Color(0xFF10B981)
                                isOverdue -> SlateAccent
                                else -> Color(0xFFD97706)
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateSurface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isMatricula) Color(0xFF6366F1).copy(alpha = 0.15f)
                                                    else SlatePrimary.copy(alpha = 0.15f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isMatricula) Icons.Default.School else Icons.Default.Payments,
                                                contentDescription = null,
                                                tint = if (isMatricula) Color(0xFF6366F1) else SlatePrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pay.concept,
                                                color = SlateTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = "s/. ${String.format("%.2f", pay.amount)}",
                                                    color = SlatePrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(statusBgColor)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(text = statusText, color = statusTextColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        IconButton(onClick = { viewModel.deletePayment(pay.id) }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Borrar", tint = SlateTextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Divider(color = SlateBorder)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (pay.status == "PAGADO") "Fec. Pago:" else "Vence:",
                                                color = SlateTextSecondary,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = if (pay.status == "PAGADO") {
                                                    pay.paymentDate?.let { formatTimestamp(it).substring(0, 10) } ?: "-"
                                                } else {
                                                    formatTimestamp(pay.dueDate).substring(0, 10)
                                                },
                                                color = if (isOverdue) SlateAccent else SlateTextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (pay.status == "PAGADO") {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Referencia:", color = SlateTextSecondary, fontSize = 9.sp)
                                                Text(pay.transactionRef ?: "Ref Directa", color = SlatePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Button(
                                                onClick = { showPaymentDialog = pay; inputTxnRef = "TXN-${(1000..9999).random()}-${(100..999).random()}X" },
                                                colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Pagar Ahora", color = SlateBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 1: FECHAS ADMINISTRATIVAS & CRONOGRAMA ACADÉMICO
                val adminMilestones = remember(calendarEvents) {
                    calendarEvents.filter { it.type == "MATRICULA" || it.type == "GENERAL" || it.type == "ADMINISTRATIVA" }
                }

                if (adminMilestones.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay hitos administrativos agendados.", color = SlateTextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(adminMilestones) { ev ->
                            val daysLeft = ((ev.dateMillis - System.currentTimeMillis()) / (24 * 60 * 60 * 1000L)).toInt()
                            val colorTag = when(ev.type) {
                                "MATRICULA" -> Color(0xFF6366F1)
                                "ADMINISTRATIVA" -> SlatePrimary
                                else -> SlateSecondary
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                                border = BorderStroke(1.dp, SlateBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(colorTag)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(ev.title, color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(colorTag.copy(alpha = 0.12f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(ev.type, color = colorTag, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text(ev.description, color = SlateTextSecondary, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Fecha: " + formatTimestamp(ev.dateMillis).substring(0, 10),
                                            color = SlateTextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = when {
                                                daysLeft < 0 -> "Hecho"
                                                daysLeft == 0 -> "Hoy!"
                                                else -> "en $daysLeft d"
                                            },
                                            color = if (daysLeft in 0..3) SlateAccent else SlateTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(onClick = { viewModel.deleteCalendarEvent(ev.id.toLong()) }) {
                                            Icon(Icons.Default.DeleteOutline, "Borrar hito", tint = SlateTextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // DIALOG: ADD NEW UNDERTAKING PAYMENT OBLIGATION (H8)
        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Registrar Derecho de Pago o Matrícula", color = SlateTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                        // Selector Tipo de carga
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("PAGO", "MATRICULA").forEach { type ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (payTypeSelected == type) SlatePrimary.copy(alpha=0.15f) else SlateBorder)
                                        .border(1.dp, if (payTypeSelected == type) SlatePrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { payTypeSelected = type }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(type, color = if (payTypeSelected == type) SlatePrimary else SlateTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = payConcept,
                            onValueChange = { payConcept = it },
                            label = { Text("Concepto de pago") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary)
                        )

                        OutlinedTextField(
                            value = payAmount,
                            onValueChange = { payAmount = it },
                            label = { Text("Monto (S/.)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary)
                        )

                        OutlinedTextField(
                            value = payDaysFromNow,
                            onValueChange = { payDaysFromNow = it },
                            label = { Text("Días hasta vencimiento") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddDialog = false }) { Text("Atrás", color = SlateTextSecondary) }
                            Button(
                                onClick = {
                                    val amt = payAmount.toDoubleOrNull() ?: 280.00
                                    val days = payDaysFromNow.toLongOrNull() ?: 7L
                                    val dueAt = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000)

                                    if (payConcept.isNotBlank()) {
                                        val finalConcept = if (payTypeSelected == "MATRICULA") "[MATRÍCULA] $payConcept" else payConcept
                                        viewModel.addPayment(finalConcept, amt, dueAt)
                                        showAddDialog = false
                                        payConcept = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                            ) {
                                Text("Registrar", color = SlateBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // DIALOG: ADD NEW INSTITUTIONAL MILESTONE (ADMIN EVENT)
        if (showAddAdminEventDialog) {
            Dialog(onDismissRequest = { showAddAdminEventDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Registrar Hito / Fecha Administrativa", color = SlateTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = eventTitle,
                            onValueChange = { eventTitle = it },
                            label = { Text("Título del hito") },
                            placeholder = { Text("Ej. Retiro de Cursos") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary)
                        )

                        OutlinedTextField(
                            value = eventDesc,
                            onValueChange = { eventDesc = it },
                            label = { Text("Descripción / Detalle") },
                            placeholder = { Text("Ej. Pago de derecho s/. 50") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary)
                        )

                        OutlinedTextField(
                            value = eventDaysFromNow,
                            onValueChange = { eventDaysFromNow = it },
                            label = { Text("Días desde hoy") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAddAdminEventDialog = false }) { Text("Atrás", color = SlateTextSecondary) }
                            Button(
                                onClick = {
                                    val days = eventDaysFromNow.toLongOrNull() ?: 10L
                                    val actMillis = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L)
                                    if (eventTitle.isNotBlank()) {
                                        viewModel.addCalendarEvent(
                                            title = eventTitle,
                                            dateMillis = actMillis,
                                            type = "ADMINISTRATIVA",
                                            courseId = 0,
                                            description = eventDesc
                                        )
                                        showAddAdminEventDialog = false
                                        eventTitle = ""
                                        eventDesc = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                            ) {
                                Text("Guardar", color = SlateBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // DIALOG: EXECUTE SIMULATED PAYMENT OR CREDIT CARD TRANSACTION (H8)
        if (showPaymentDialog != null) {
            val paymentSelected = showPaymentDialog!!
            Dialog(onDismissRequest = { showPaymentDialog = null }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Simular Pasarela de Pagos", color = SlateTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Concepto: ${paymentSelected.concept}", color = SlateTextSecondary, fontSize = 12.sp)
                        Text(
                            text = "Monto Total: S/. ${paymentSelected.amount}",
                            color = SlatePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        OutlinedTextField(
                            value = inputTxnRef,
                            onValueChange = { inputTxnRef = it },
                            label = { Text("Código de Operación / Tarjeta") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showPaymentDialog = null }) { Text("Atrás", color = SlateTextSecondary) }
                            Button(
                                onClick = {
                                    viewModel.payPayment(paymentSelected.id, inputTxnRef)
                                    showPaymentDialog = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                            ) {
                                Text("Aprobar Transacción", color = SlateBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 4: ACADEMIC CHANGES & WEEKLY SCHEDULE (H9)
// -------------------------------------------------------------
@Composable
fun AcademicScreen(viewModel: MoodleViewModel) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val academicChanges by viewModel.academicChanges.collectAsStateWithLifecycle()

    var showChangeDialog by remember { mutableStateOf(false) }

    // Form inputs
    var selectCourseId by remember { mutableStateOf(if (courses.isNotEmpty()) courses.first().id else 101) }
    var inputOldRoom by remember { mutableStateOf("") }
    var inputNewRoom by remember { mutableStateOf("") }
    var inputOldSchedule by remember { mutableStateOf("") }
    var inputNewSchedule by remember { mutableStateOf("") }
    var inputMotive by remember { mutableStateOf("") }

    // Toggle between View Changelog and Weekly Schedule
    var showChangelogView by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (courses.isNotEmpty()) {
                        val first = courses.first()
                        selectCourseId = first.id
                        inputOldRoom = first.defaultRoom
                        inputOldSchedule = first.defaultSchedule
                    }
                    showChangeDialog = true
                },
                containerColor = SlatePrimary,
                contentColor = SlateBackground
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Modificar Clase/Aula")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Plan Académico",
                        color = SlateTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Traslados de salón y reprogramaciones (H9)",
                        color = SlateTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = { viewModel.testAlertReception("ACADEMIC") },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateSecondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.CellTower, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Probar Cambio", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub navigation bar within view (Toggle between Schedule and Changelog)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SlateSurface)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!showChangelogView) SlateSecondary else Color.Transparent)
                        .clickable { showChangelogView = false }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Horario Semanal (H9)",
                        color = SlateTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showChangelogView) SlateSecondary else Color.Transparent)
                        .clickable { showChangelogView = true }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Historial de Modificaciones",
                        color = SlateTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!showChangelogView) {
                // SCHEDULE VIEW
                // Representing courses, incorporating changes (old to new changes applied)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text(
                            text = "Lunes a Sábados",
                            color = SlatePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    if (courses.isEmpty()) {
                        item {
                            Text("Sin asignaturas matriculadas.", color = SlateTextSecondary, fontSize = 13.sp)
                        }
                    } else {
                        items(courses) { course ->
                            // Check if this course has a recent change
                            val change = academicChanges.find { it.courseId == course.id }
                            val roomToShow = change?.newRoom ?: course.defaultRoom
                            val scheduleToShow = change?.newSchedule ?: course.defaultSchedule

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (change != null) SlateSecondary.copy(alpha = 0.15f) else SlateSurface
                                ),
                                border = if (change != null) BorderStroke(1.dp, brush = Brush.linearGradient(listOf(SlateSecondary, SlateAccent))) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = course.fullname,
                                            color = SlateTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = null,
                                                tint = if (change != null) SlateAccent else SlatePrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = roomToShow,
                                                color = if (change != null) SlateAccent else SlateTextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = SlateTextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = scheduleToShow,
                                                color = SlateTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }

                                        if (change != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(SlateAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Cambiado de ${change.oldRoom} • ${change.description}",
                                                    color = SlateAccent,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // CHANGELOG HISTORIC LIST
                if (academicChanges.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No se registraron reportes históricos.", color = SlateTextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(academicChanges) { change ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateSurface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = change.courseName,
                                            color = SlatePrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        IconButton(onClick = { viewModel.deleteAcademicChange(change.id) }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = SlateTextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Reubicación de aula:",
                                        color = SlateTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "De '${change.oldRoom}' ➔ '${change.newRoom}'",
                                        color = SlateTextPrimary,
                                        fontSize = 12.sp
                                    )

                                    if (change.oldSchedule != change.newSchedule) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Horario reprogramado: ${change.newSchedule}",
                                            color = SlateAccent,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Causa: ${change.description}",
                                        color = SlateTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // DIALOG: ADD/REGISTER ACADEMIC CHANGES (H9)
        if (showChangeDialog) {
            Dialog(onDismissRequest = { showChangeDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Registrar Cambio de Aula/Horario (H9)", color = SlateTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

                        Text("Selecciona la Asignatura:", color = SlateTextSecondary, fontSize = 11.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            courses.forEach { c ->
                                val selected = selectCourseId == c.id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) SlatePrimary else SlateBorder)
                                        .clickable {
                                            selectCourseId = c.id
                                            inputOldRoom = c.defaultRoom
                                            inputOldSchedule = c.defaultSchedule
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(c.shortname, color = if (selected) SlateBackground else SlateTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = inputOldRoom,
                            onValueChange = { inputOldRoom = it },
                            label = { Text("Aula anterior") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary)
                        )

                        OutlinedTextField(
                            value = inputNewRoom,
                            onValueChange = { inputNewRoom = it },
                            label = { Text("Nueva aula asignada") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary)
                        )

                        OutlinedTextField(
                            value = inputOldSchedule,
                            onValueChange = { inputOldSchedule = it },
                            label = { Text("Horario anterior") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary)
                        )

                        OutlinedTextField(
                            value = inputNewSchedule,
                            onValueChange = { inputNewSchedule = it },
                            label = { Text("Nuevo horario asignado") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary)
                        )

                        OutlinedTextField(
                            value = inputMotive,
                            onValueChange = { inputMotive = it },
                            label = { Text("Motivo del traslado") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showChangeDialog = false }) { Text("Atrás", color = SlateTextSecondary) }
                            Button(
                                onClick = {
                                    val matched = courses.find { it.id == selectCourseId }
                                    val courseFullname = matched?.fullname ?: "Curso"
                                    if (inputNewRoom.isNotBlank() && inputMotive.isNotBlank()) {
                                        viewModel.addAcademicChange(
                                            courseId = selectCourseId,
                                            courseName = courseFullname,
                                            oldRoom = inputOldRoom,
                                            newRoom = inputNewRoom,
                                            oldSched = inputOldSchedule,
                                            newSched = inputNewSchedule,
                                            description = inputMotive
                                        )
                                        showChangeDialog = false
                                        inputNewRoom = ""
                                        inputMotive = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                            ) {
                                Text("Registrar", color = SlateBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 5: PERFIL Y CONFIGURACION (H3 + H10)
// -------------------------------------------------------------
@Composable
fun SettingsScreen(viewModel: MoodleViewModel) {
    val configState by viewModel.config.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val testState by viewModel.testState.collectAsStateWithLifecycle()
    val notificationLogs by viewModel.notificationLogs.collectAsStateWithLifecycle()

    var inputUrl by remember { mutableStateOf("") }
    var inputToken by remember { mutableStateOf("") }
    var pushEnabled by remember { mutableStateOf(true) }
    var alertMinutesSetting by remember { mutableStateOf("15") }
    
    // Granular alert settings
    var academicAlerts by remember { mutableStateOf(true) }
    var paymentAlerts by remember { mutableStateOf(true) }
    var changesAlerts by remember { mutableStateOf(true) }
    
    // H3 Profile state fields
    var studName by remember { mutableStateOf("") }
    var studEmail by remember { mutableStateOf("") }
    var studCode by remember { mutableStateOf("") }
    var studCareer by remember { mutableStateOf("") }
    var studCycle by remember { mutableStateOf("") }
    var studPhone by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("avatar_1") }

    // Validation messages
    var validationError by remember { mutableStateOf<String?>(null) }
    var validationSuccess by remember { mutableStateOf(false) }

    var showLogsTab by remember { mutableStateOf(false) }

    // Init inputs when config loads
    LaunchedEffect(configState) {
        configState?.let { conf ->
            inputUrl = conf.moodleUrl
            inputToken = conf.wsToken
            pushEnabled = conf.notificationsEnabled
            alertMinutesSetting = conf.alertLeadTimeMinutes.toString()
            academicAlerts = conf.academicAlertsEnabled
            paymentAlerts = conf.paymentAlertsEnabled
            changesAlerts = conf.changesAlertsEnabled
            studName = conf.studentName
            studEmail = conf.studentEmail
            studCode = conf.studentCode
            studCareer = conf.studentCareer
            studCycle = conf.studentCycle
            studPhone = conf.studentPhone
            selectedAvatar = conf.studentAvatarRes
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App settings/profile header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Mi Perfil Estudiantil",
                    color = SlateTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ajustes del portal y sincronización Moodle",
                    color = SlateTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Tab selection row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SlateBorder)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!showLogsTab) SlateSurface else Color.Transparent)
                    .clickable { showLogsTab = false }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Mi Cuenta UC",
                    color = if (!showLogsTab) SlatePrimary else SlateTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (showLogsTab) SlateSurface else Color.Transparent)
                    .clickable { showLogsTab = true }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Historial Notifics. (${notificationLogs.size})",
                    color = if (showLogsTab) SlatePrimary else SlateTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!showLogsTab) {
            // PARAMETERS FORM CONFIG WITH PROFILE EDIT (H3 PROFILE)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // AVATAR AND GREETING SUMMARY CARD
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = BorderStroke(1.dp, SlateBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Elige tu Avatar Universitario",
                                color = SlateTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Interactive selection line
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val avatars = listOf("avatar_1", "avatar_2", "avatar_3", "avatar_4", "avatar_5", "avatar_6")
                                avatars.forEach { avId ->
                                    val isSelected = selectedAvatar == avId
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) SlatePrimary.copy(alpha = 0.16f) else Color.Transparent)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) SlatePrimary else SlateBorder,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                selectedAvatar = avId
                                                viewModel.updateStudentAvatar(avId)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        StudentAvatarImage(avId, size = 32.dp)
                                    }
                                }
                            }
                        }
                    }
                }

                // DATA PERSONAL CONTAINER (H3)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = BorderStroke(1.dp, SlateBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Datos Personales Universitarios (H3)", color = SlatePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Icon(Icons.Default.ManageAccounts, null, tint = SlatePrimary, modifier = Modifier.size(18.dp))
                            }

                            if (validationError != null) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateAccent.copy(alpha = 0.08f)),
                                    border = BorderStroke(1.dp, SlateAccent.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = validationError ?: "",
                                        color = SlateAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            if (validationSuccess) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.08f)),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "✔ Perfil guardado con éxito.",
                                        color = Color(0xFF10B981),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = studName,
                                onValueChange = { studName = it; validationError = null; validationSuccess = false },
                                label = { Text("Nombre Completo Estudiante") },
                                leadingIcon = { Icon(Icons.Default.Badge, null, tint = SlateTextSecondary, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = studEmail,
                                onValueChange = { studEmail = it; validationError = null; validationSuccess = false },
                                label = { Text("Email Institucional") },
                                leadingIcon = { Icon(Icons.Default.Email, null, tint = SlateTextSecondary, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary),
                                singleLine = true
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = studCode,
                                    onValueChange = { studCode = it; validationError = null; validationSuccess = false },
                                    label = { Text("Código UC") },
                                    leadingIcon = { Icon(Icons.Default.AssignmentInd, null, tint = SlateTextSecondary, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = studCycle,
                                    onValueChange = { studCycle = it; validationError = null; validationSuccess = false },
                                    label = { Text("Años/Ciclo") },
                                    modifier = Modifier.weight(0.9f),
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary),
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = studCareer,
                                onValueChange = { studCareer = it; validationError = null; validationSuccess = false },
                                label = { Text("Carrera Profesional") },
                                leadingIcon = { Icon(Icons.Default.School, null, tint = SlateTextSecondary, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = studPhone,
                                onValueChange = { studPhone = it; validationError = null; validationSuccess = false },
                                label = { Text("Celular / Teléfono") },
                                leadingIcon = { Icon(Icons.Default.Phone, null, tint = SlateTextSecondary, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    // Robust UI Validation
                                    if (studName.isBlank() || studEmail.isBlank() || studCode.isBlank() || studCareer.isBlank()) {
                                        validationError = "Nombre, email, código y carrera son obligatorios."
                                        return@Button
                                    }
                                    if (!studEmail.contains("@") || !studEmail.contains(".")) {
                                        validationError = "Formato de correo electrónico no válido."
                                        return@Button
                                    }

                                    // Saving
                                    validationError = null
                                    validationSuccess = true
                                    viewModel.saveStudentProfile(
                                        name = studName,
                                        email = studEmail,
                                        code = studCode,
                                        career = studCareer,
                                        cycle = studCycle,
                                        phone = studPhone,
                                        avatar = selectedAvatar
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Guardar Datos Personales", color = SlateSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // INTEGRATION CREDENTIALS AND TESTS WITH MOODLE CLOUD
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = BorderStroke(1.dp, SlateBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Credenciales Moodle Cloud (H10 / Sync)", color = SlatePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)

                            OutlinedTextField(
                                value = inputUrl,
                                onValueChange = { inputUrl = it },
                                label = { Text("URL de Moodle Cloud") },
                                placeholder = { Text("https://myclass.moodlecloud.com") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = inputToken,
                                onValueChange = { inputToken = it },
                                label = { Text("Token de Acceso Web Service") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.testConnection(inputUrl, inputToken) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateSecondary),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (testState is TestConnectionState.Testing) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SlateSurface, strokeWidth = 2.dp)
                                    } else {
                                        Text("Probar Conexión", fontSize = 11.sp, color = SlateSurface)
                                    }
                                }

                                Button(
                                    onClick = {
                                        viewModel.saveConfig(
                                            moodleUrl = inputUrl,
                                            wsToken = inputToken,
                                            notificationsEnabled = pushEnabled,
                                            alertLeadTime = alertMinutesSetting.toIntOrNull() ?: 15,
                                            academicAlerts = academicAlerts,
                                            paymentAlerts = paymentAlerts,
                                            changesAlerts = changesAlerts,
                                            studentName = studName,
                                            studentEmail = studEmail
                                        )
                                        // La sincronización se dispara automáticamente al guardar cambios de credenciales
                                        viewModel.triggerSync()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Guardar y Validar Credenciales", color = SlateSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Feedback from testState
                            when (testState) {
                                is TestConnectionState.Success -> {
                                    val succ = testState as TestConnectionState.Success
                                    Text(
                                        text = "✓ Conectado: ${succ.fullname} en ${succ.sitename}.",
                                        color = Color(0xFF10B981),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                is TestConnectionState.Error -> {
                                    Text(
                                        text = "✗ Error: ${(testState as TestConnectionState.Error).message}",
                                        color = SlateAccent,
                                        fontSize = 11.sp
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }

                // ALERT PREFERENCES SETUP CARD
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        border = BorderStroke(1.dp, SlateBorder),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, null, tint = SlatePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Configuración de Alertas & Notificaciones", color = SlatePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Divider(color = SlateBorder)

                            // 1. General push
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Activar Notificaciones Globales", color = SlateTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Permite recibir cualquier aviso del sistema", color = SlateTextSecondary, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = pushEnabled,
                                    onCheckedChange = { pushEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = SlatePrimary)
                                )
                            }

                            if (pushEnabled) {
                                // 2. Academic Alerts
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Alertas de Exámenes y Tareas", color = SlateTextPrimary, fontSize = 12.sp)
                                        Text("Pruebas programadas y entregas", color = SlateTextSecondary, fontSize = 10.sp)
                                    }
                                    Switch(
                                        checked = academicAlerts,
                                        onCheckedChange = { academicAlerts = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = SlatePrimary)
                                    )
                                }

                                // 3. Payment Alerts
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Recordatorios de Pagos y Matrículas", color = SlateTextPrimary, fontSize = 12.sp)
                                        Text("Vencimientos de pensiones y tasas", color = SlateTextSecondary, fontSize = 10.sp)
                                    }
                                    Switch(
                                        checked = paymentAlerts,
                                        onCheckedChange = { paymentAlerts = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = SlatePrimary)
                                    )
                                }

                                // 4. Academic Changes Alerts
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Modificaciones de Aulas y Horarios", color = SlateTextPrimary, fontSize = 12.sp)
                                        Text("Traslados de salón de última hora", color = SlateTextSecondary, fontSize = 10.sp)
                                    }
                                    Switch(
                                        checked = changesAlerts,
                                        onCheckedChange = { changesAlerts = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = SlatePrimary)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = alertMinutesSetting,
                                onValueChange = { alertMinutesSetting = it },
                                label = { Text("Tiempo de alerta global (Minutos antes)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary),
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    viewModel.saveConfig(
                                        moodleUrl = inputUrl,
                                        wsToken = inputToken,
                                        notificationsEnabled = pushEnabled,
                                        alertLeadTime = alertMinutesSetting.toIntOrNull() ?: 15,
                                        academicAlerts = academicAlerts,
                                        paymentAlerts = paymentAlerts,
                                        changesAlerts = changesAlerts,
                                        studentName = studName,
                                        studentEmail = studEmail
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateSecondary),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Guardar Preferencias de Alertas", color = SlateSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // NOTIFICATIONS & USER ACTIVITIES LOGS HISTORY (H10)
            var showActivitiesOnly by remember { mutableStateOf(false) }
            val filteredLogs = remember(notificationLogs, showActivitiesOnly) {
                if (showActivitiesOnly) {
                    notificationLogs.filter { it.type == "USER_ACTION" }
                } else {
                    notificationLogs.filter { it.type != "USER_ACTION" }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Historial de Actividad y Avisos", color = SlateTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { viewModel.clearNotificationLogs() }) {
                        Text("Limpiar Todo", color = SlateAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Sub-tabs switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SlateBorder)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!showActivitiesOnly) SlateSurface else Color.Transparent)
                            .clickable { showActivitiesOnly = false }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Avisos del Portal",
                            color = if (!showActivitiesOnly) SlatePrimary else SlateTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (showActivitiesOnly) SlateSurface else Color.Transparent)
                            .clickable { showActivitiesOnly = true }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Mis Acciones",
                            color = if (showActivitiesOnly) SlatePrimary else SlateTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showActivitiesOnly) "Presiona guardar, pagar o registrar para generar historial." else "No hay avisos ni alertas registradas.",
                        color = SlateTextSecondary,
                        fontSize = 12.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredLogs) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SlateSurface),
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (log.type) {
                                                "ALERT" -> SlateAccent.copy(alpha = 0.1f)
                                                "PAYMENT" -> Color(0xFFFBBF24).copy(alpha = 0.1f)
                                                "USER_ACTION" -> Color(0xFF10B981).copy(alpha = 0.1f)
                                                else -> SlatePrimary.copy(alpha = 0.1f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (log.type) {
                                            "ALERT" -> Icons.Default.Warning
                                            "PAYMENT" -> Icons.Default.Payments
                                            "USER_ACTION" -> Icons.Default.Settings
                                            else -> Icons.Default.School
                                        },
                                        contentDescription = null,
                                        tint = when (log.type) {
                                            "ALERT" -> SlateAccent
                                            "PAYMENT" -> Color(0xFFD97706)
                                            "USER_ACTION" -> Color(0xFF10B981)
                                            else -> SlatePrimary
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.title,
                                        color = SlateTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = log.message,
                                        color = SlateTextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatTimestamp(log.timestamp),
                                        color = SlateTextSecondary,
                                        fontSize = 9.sp
                                    )
                                }
 
                                IconButton(onClick = { viewModel.deleteNotificationLog(log.id) }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Eliminar", tint = SlateTextSecondary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STUDENT AVATAR RENDERER
// -------------------------------------------------------------
@Composable
fun StudentAvatarImage(avatarId: String, size: androidx.compose.ui.unit.Dp = 48.dp, modifier: Modifier = Modifier) {
    val emoji = when (avatarId) {
        "avatar_1" -> "👨‍🎓"
        "avatar_2" -> "👩‍🎓"
        "avatar_3" -> "💻"
        "avatar_4" -> "📚"
        "avatar_5" -> "🔬"
        "avatar_6" -> "🎨"
        else -> "🎓"
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 26))
            .background(SlatePrimary.copy(alpha = 0.1f))
            .border(1.dp, SlatePrimary.copy(alpha = 0.25f), RoundedCornerShape(percent = 26)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = (size.value * 0.52f).sp)
    }
}

// -------------------------------------------------------------
// SCREEN: WELCOME / ONBOARDING (H1)
// -------------------------------------------------------------
@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SlateBackground, SlateSurface)
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Visual Educational Badge
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(SlatePrimary.copy(alpha = 0.12f))
                    .border(2.dp, SlatePrimary, RoundedCornerShape(30.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = SlatePrimary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Moodle Companion",
                color = SlateTextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Tu Portal Universitario Inteligente",
                color = SlatePrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Short dynamic slider cards
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha = 0.9f)),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Speed,
                        contentDescription = null,
                        tint = SlateSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sincronización Total",
                        color = SlateTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Conéctate de forma ágil a Moodle Cloud para visualizar tus evaluaciones, cambios en horarios, aulas y pagos pendientes en tiempo real.",
                        color = SlateTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Login, contentDescription = null, tint = SlateSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Iniciar Sesión",
                        color = SlateSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onRegisterClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SlatePrimary),
                border = BorderStroke(1.5.dp, SlatePrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = SlatePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Crear Cuenta Estudiantil",
                        color = SlatePrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Versión Digital 2.5 • Conectividad Local & Cloud",
                color = SlateTextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

// -------------------------------------------------------------
// SCREEN: LOGIN / INICIO DE SESION (H2)
// -------------------------------------------------------------
@Composable
fun LoginScreen(
    viewModel: MoodleViewModel,
    config: MoodleConfig?,
    onBack: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegisterClick: () -> Unit
) {
    var emailOrCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateSurface)
                        .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = SlateTextPrimary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Iniciar Sesión",
                    color = SlateTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Input Form block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¡Hola de nuevo!",
                    color = SlateTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ingresa con tu correo universitario o código de alumno para sincronizar tus asignaciones.",
                    color = SlateTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                    lineHeight = 16.sp
                )

                if (errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateAccent.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, SlateAccent.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = SlateAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = SlateAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Inputs
                OutlinedTextField(
                    value = emailOrCode,
                    onValueChange = { 
                        emailOrCode = it
                        errorMessage = null 
                    },
                    label = { Text("Correo o Código de Estudiante") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SlateTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SlatePrimary,
                        unfocusedBorderColor = SlateBorder,
                        focusedTextColor = SlateTextPrimary,
                        unfocusedTextColor = SlateTextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        errorMessage = null 
                    },
                    label = { Text("Contraseña Estudiantil") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SlateTextSecondary) },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(image, contentDescription = null, tint = SlateTextSecondary)
                        }
                    },
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SlatePrimary,
                        unfocusedBorderColor = SlateBorder,
                        focusedTextColor = SlateTextPrimary,
                        unfocusedTextColor = SlateTextPrimary
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onForgotPassword) {
                        Text(
                            text = "¿Olvidaste tu contraseña?",
                            color = SlatePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isLoading = true
                        viewModel.loginStudent(emailOrCode, password) { success, msg ->
                            isLoading = false
                            if (!success) {
                                errorMessage = msg
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SlateSurface, strokeWidth = 2.dp)
                    } else {
                        Text("Acceder Seguro", color = SlateSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Bottom prompt
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "¿No tienes una cuenta?", color = SlateTextSecondary, fontSize = 13.sp)
                TextButton(onClick = onRegisterClick) {
                    Text(text = "Regístrate aquí", color = SlatePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN: REGISTRO DE USUARIOS (H2 + PERSISTENCIA)
// -------------------------------------------------------------
@Composable
fun RegisterScreen(
    viewModel: MoodleViewModel,
    onBack: () -> Unit,
    onLoginClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("U202110482") }
    var career by remember { mutableStateOf("Ingeniería de Sistemas") }
    var cycle by remember { mutableStateOf("8vo Ciclo") }
    var phone by remember { mutableStateOf("+51 987 654 432") }
    var password by remember { mutableStateOf("") }
    
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isRegistering by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateSurface)
                        .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = SlateTextPrimary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Registro Estudiantil",
                    color = SlateTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Forms Fields with Lazy Column
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Crear Cuenta Nueva",
                        color = SlateTextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Regístrate en el portal local sincronizado con Moodle Cloud.",
                        color = SlateTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (errorMsg != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SlateAccent.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, SlateAccent.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = errorMsg ?: "",
                                color = SlateAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMsg = null },
                        label = { Text("Nombre Completo (Obligatorio)") },
                        leadingIcon = { Icon(Icons.Default.Badge, null, tint = SlateTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMsg = null },
                        label = { Text("Correo Electrónico de Moodle (Obligatorio)") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = SlateTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary),
                        singleLine = true
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it; errorMsg = null },
                            label = { Text("Código Alumno") },
                            leadingIcon = { Icon(Icons.Default.AssignmentInd, null, tint = SlateTextSecondary) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = cycle,
                            onValueChange = { cycle = it; errorMsg = null },
                            label = { Text("Ciclo") },
                            leadingIcon = { Icon(Icons.Default.School, null, tint = SlateTextSecondary) },
                            modifier = Modifier.weight(0.8f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = career,
                        onValueChange = { career = it; errorMsg = null },
                        label = { Text("Especialidad / Carrera") },
                        leadingIcon = { Icon(Icons.Default.Bookmark, null, tint = SlateTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; errorMsg = null },
                        label = { Text("Celular / Teléfono") },
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = SlateTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null },
                        label = { Text("Contraseña de Acceso (mínimo 4 caracteres)") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = SlateTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary),
                        singleLine = true
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            isRegistering = true
                            viewModel.registerStudent(
                                name = name,
                                email = email,
                                code = code,
                                career = career,
                                cycle = cycle,
                                phone = phone,
                                password = password
                            ) { success, msg ->
                                isRegistering = false
                                if (!success) {
                                    errorMsg = msg
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isRegistering) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SlateSurface)
                        } else {
                            Text("Registrarse & Conectar", color = SlateSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Prompt
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "¿Ya creaste una cuenta?", color = SlateTextSecondary, fontSize = 13.sp)
                TextButton(onClick = onLoginClick) {
                    Text(text = "Inicia Sesión", color = SlatePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN: RECUPERAR CONTRASEÑA (H2)
// -------------------------------------------------------------
@Composable
fun ForgotPasswordScreen(
    viewModel: MoodleViewModel,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var infoMsg by remember { mutableStateOf<String?>(null) }
    var successAlert by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateSurface)
                        .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", tint = SlateTextPrimary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Recuperar Acceso",
                    color = SlateTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Central block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    color = SlateTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ingresa tu correo estudiantil para simular una recuperación segura de tu clave Moodle Cloud.",
                    color = SlateTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                    lineHeight = 16.sp
                )

                if (infoMsg != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (successAlert) Color(0xFF10B981).copy(alpha = 0.08f) else SlateAccent.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, if (successAlert) Color(0xFF10B981).copy(alpha = 0.3f) else SlateAccent.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (successAlert) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (successAlert) Color(0xFF10B981) else SlateAccent
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = infoMsg ?: "",
                                color = if (successAlert) Color(0xFF10B981) else SlateAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        infoMsg = null 
                    },
                    label = { Text("Correo Electrónico Estudiantil") },
                    leadingIcon = { Icon(Icons.Default.Email, "Email", tint = SlateTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SlatePrimary,
                        focusedTextColor = SlateTextPrimary,
                        unfocusedTextColor = SlateTextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        viewModel.recoverPassword(email) { ok, resp ->
                            successAlert = ok
                            infoMsg = resp
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Recuperar Clave", color = SlateSurface, fontWeight = FontWeight.Bold)
                }
            }

            // Bottom prompt
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = SlatePrimary)
            ) {
                Text("Volver al Inicio de Sesión", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 6: COURSES (H4)
// -------------------------------------------------------------
@Composable
fun CoursesScreen(viewModel: MoodleViewModel, navController: NavHostController) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    
    var showDialog by remember { mutableStateOf(false) }
    var courseToEdit by remember { mutableStateOf<Course?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Course?>(null) }
    
    var fullname by remember { mutableStateOf("") }
    var shortname by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Moodle Cloud") }
    var defaultRoom by remember { mutableStateOf("") }
    var defaultSchedule by remember { mutableStateOf("") }

    // H10: Sincronización automática al entrar si la lista está vacía
    LaunchedEffect(Unit) {
        if (courses.isEmpty()) {
            viewModel.triggerSync()
        }
    }

    val closeDialog = {
        showDialog = false
        courseToEdit = null
        fullname = ""
        shortname = ""
        category = "Moodle Cloud"
        defaultRoom = ""
        defaultSchedule = ""
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    courseToEdit = null
                    showDialog = true 
                },
                containerColor = SlatePrimary,
                contentColor = SlateBackground
            ) {
                Icon(Icons.Default.Add, "Nuevo")
            }
        }
    ) { paddingInfo ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingInfo)
                .padding(horizontal = 16.dp)
        ) {
            // Nota: El encabezado con flecha atrás ya lo provee el Scaffold principal de la App
            Spacer(modifier = Modifier.height(8.dp))
            
            if (syncState is SyncState.Syncing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = SlatePrimary,
                    trackColor = SlateBorder
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
            ) {
                if (courses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.MenuBook, 
                                    null, 
                                    modifier = Modifier.size(64.dp), 
                                    tint = SlateBorder
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    if (syncState is SyncState.Syncing) "Buscando cursos en Moodle Cloud..." 
                                    else if (syncState is SyncState.Error) "Error: ${(syncState as SyncState.Error).message}"
                                    else "No hay cursos registrados en tu cuenta.", 
                                    color = SlateTextSecondary, 
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                                if (syncState is SyncState.Idle && courses.isEmpty()) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = { viewModel.triggerSync() },
                                        colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                                    ) {
                                        Icon(Icons.Default.Sync, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Reintentar Sincronización")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(courses) { course ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SlateSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SlatePrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Book, contentDescription = null, tint = SlatePrimary)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = course.fullname, 
                                            color = SlateTextPrimary, 
                                            fontWeight = FontWeight.ExtraBold, 
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "${course.shortname} • ${course.category}", 
                                            color = SlateTextSecondary, 
                                            fontSize = 12.sp
                                        )
                                    }
                                    IconButton(onClick = {
                                        courseToEdit = course
                                        fullname = course.fullname
                                        shortname = course.shortname
                                        category = course.category
                                        defaultRoom = course.defaultRoom
                                        defaultSchedule = course.defaultSchedule
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = SlatePrimary, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { showDeleteConfirm = course }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = SlateAccent, modifier = Modifier.size(20.dp))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Place, null, tint = SlateSecondary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(course.defaultRoom.ifBlank { "Virtual" }, color = SlateTextPrimary, fontSize = 13.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Event, null, tint = SlateSecondary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(course.defaultSchedule.ifBlank { "Sincronizado" }, color = SlateTextPrimary, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = closeDialog) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SlateSurface)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (courseToEdit == null) "Nuevo Curso" else "Editar Curso", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = fullname, onValueChange = { fullname = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = shortname, onValueChange = { shortname = it }, label = { Text("Nombre Corto") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoría") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = defaultRoom, onValueChange = { defaultRoom = it }, label = { Text("Aula por defecto") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = defaultSchedule, onValueChange = { defaultSchedule = it }, label = { Text("Horario por defecto") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = closeDialog) { Text("Cancelar", color = SlateTextSecondary) }
                        Button(
                            onClick = {
                                if (fullname.isNotBlank() && shortname.isNotBlank()) {
                                    if (courseToEdit != null) {
                                        viewModel.updateCourse(courseToEdit!!.id, fullname, shortname, category, defaultRoom, defaultSchedule)
                                    } else {
                                        viewModel.addCourse(fullname, shortname, category, defaultRoom, defaultSchedule)
                                    }
                                    closeDialog()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                        ) { Text("Guardar") }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Confirmar Eliminación", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text("¿Estás seguro de que deseas eliminar el curso '${showDeleteConfirm?.fullname}'?", color = SlateTextSecondary) },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteCourse(showDeleteConfirm!!.id)
                    showDeleteConfirm = null
                }, colors = ButtonDefaults.buttonColors(containerColor = SlateAccent)) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancelar", color = SlateTextSecondary)
                }
            },
            containerColor = SlateSurface
        )
    }
}

// -------------------------------------------------------------
// SCREEN 7: SCHEDULE (H5)
// -------------------------------------------------------------
@Composable
fun ScheduleScreen(viewModel: MoodleViewModel, navController: NavHostController) {
    val scheduleBlocks by viewModel.scheduleBlocks.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val evaluations by viewModel.evaluations.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val academicChanges by viewModel.academicChanges.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Mi Horario & Exámenes, 1 = Cambios de Aula/Horas
    var viewModeList by remember { mutableStateOf(true) } // true = Lista por Día, false = Agenda/Horas Grid

    // Dialog state for schedule block
    var showBlockDialog by remember { mutableStateOf(false) }
    var blockToEdit by remember { mutableStateOf<ScheduleBlock?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<ScheduleBlock?>(null) }
    
    var selectedCourseId by remember { mutableStateOf(if (courses.isNotEmpty()) courses.first().id else 0) }
    var dayOfWeek by remember { mutableStateOf("Lunes") }
    var startTime by remember { mutableStateOf("08:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var room by remember { mutableStateOf("Aula 101") }

    // Dialog state for custom Class / Schedule Change (AcademicChange)
    var showAddChangeDialog by remember { mutableStateOf(false) }
    var showDeleteChangeConfirm by remember { mutableStateOf<AcademicChange?>(null) }
    var changeCourseId by remember { mutableStateOf(if (courses.isNotEmpty()) courses.first().id else 0) }
    var oldRoom by remember { mutableStateOf("Aula 101") }
    var newRoom by remember { mutableStateOf("Laboratorio L-302") }
    var oldSchedule by remember { mutableStateOf("Lunes 08:00-10:00") }
    var newSchedule by remember { mutableStateOf("Lunes 10:00-12:00 (Recuperación)") }
    var changeReason by remember { mutableStateOf("Mantenimiento técnico de equipos") }

    val daysOptions = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")

    val closeBlockDialog = {
        showBlockDialog = false
        blockToEdit = null
        selectedCourseId = if (courses.isNotEmpty()) courses.first().id else 0
        dayOfWeek = "Lunes"
        startTime = "08:00"
        endTime = "10:00"
        room = "Aula 101"
    }

    // Helper to evaluate which day of the week a date millisecond corresponds to
    fun getDayOfWeekLabel(dateMillis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dateMillis
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Lunes"
            Calendar.TUESDAY -> "Martes"
            Calendar.WEDNESDAY -> "Miércoles"
            Calendar.THURSDAY -> "Jueves"
            Calendar.FRIDAY -> "Viernes"
            Calendar.SATURDAY -> "Sábado"
            Calendar.SUNDAY -> "Domingo"
            else -> "Lunes"
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    if (activeTab == 0) {
                        blockToEdit = null
                        showBlockDialog = true 
                    } else {
                        showAddChangeDialog = true
                    }
                },
                containerColor = SlatePrimary,
                contentColor = SlateBackground
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Registro")
            }
        }
    ) { paddingInfo ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingInfo)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = SlateTextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (activeTab == 0) "Horario Semanal" else "Modificaciones Académicas",
                        color = SlateTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (activeTab == 0) "Planificación semanal, exámenes y hilos importantes" else "Cambios de aulas, horarios e incidencias",
                        color = SlateTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Top Tab Selector (Horario vs Cambios)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SlateBorder)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == 0) SlateSurface else Color.Transparent)
                        .clickable { activeTab = 0 }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Mis Horas y Exámenes",
                        color = if (activeTab == 0) SlatePrimary else SlateTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == 1) SlateSurface else Color.Transparent)
                        .clickable { activeTab = 1 }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cambios de Aula",
                        color = if (activeTab == 1) SlatePrimary else SlateTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (activeTab == 0) {
                // TAB 0: WEEKLY HORARIO & INTEGRATED EVALUATIONS
                // Selector: List View vs Schedule Grid Timeline Style
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Visualización de Horario:", color = SlateTextSecondary, fontSize = 11.sp)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SlateBorder)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (viewModeList) SlatePrimary.copy(alpha=0.15f) else Color.Transparent)
                                .clickable { viewModeList = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Lista por Día", color = if (viewModeList) SlatePrimary else SlateTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (!viewModeList) SlatePrimary.copy(alpha=0.15f) else Color.Transparent)
                                .clickable { viewModeList = false }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Vista Horario", color = if (!viewModeList) SlatePrimary else SlateTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (viewModeList) {
                    // MODE A: CHRONOLOGICAL DAY COHORT
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(daysOptions.size) { index ->
                            val day = daysOptions[index]
                            val dailyBlocks = scheduleBlocks.filter { it.dayOfWeek == day }.sortedBy { it.startTime }
                            
                            // Filter evaluations and manual events matching this weekday label
                            val dayExams = evaluations.filter { getDayOfWeekLabel(it.dueDate) == day }
                            val dayEvents = calendarEvents.filter { getDayOfWeekLabel(it.dateMillis) == day && it.type != "ADMINISTRATIVA" }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateSurface.copy(alpha=0.6f)),
                                border = BorderStroke(1.dp, SlateBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(day.uppercase(), color = SlatePrimary, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                        if (dailyBlocks.isNotEmpty()) {
                                            Text("${dailyBlocks.size} clases", color = SlateTextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                    Divider(color = SlateBorder.copy(alpha=0.5f))

                                    // Render classes
                                    if (dailyBlocks.isEmpty()) {
                                        Text("No tienes clases programadas para este día.", color = SlateTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                                    } else {
                                        dailyBlocks.forEach { block ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(SlateSurface)
                                                    .clickable {
                                                        blockToEdit = block
                                                        selectedCourseId = block.courseId
                                                        dayOfWeek = block.dayOfWeek
                                                        startTime = block.startTime
                                                        endTime = block.endTime
                                                        room = block.room
                                                        showBlockDialog = true
                                                    }
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.width(55.dp)) {
                                                    Text(block.startTime, color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text(block.endTime, color = SlateTextSecondary, fontSize = 10.sp)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Divider(modifier = Modifier.width(2.dp).height(32.dp), color = SlatePrimary)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(block.courseName, color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(block.room, color = SlateTextSecondary, fontSize = 11.sp)
                                                }
                                                IconButton(onClick = { showDeleteConfirm = block }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.DeleteOutline, "Eliminar", tint = SlateAccent, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }

                                    // INTEGRATION OF WEEKLY EXAMS AND IMPORTANT EVENTS (H5)
                                    if (dayExams.isNotEmpty() || dayEvents.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Exámenes y Fechas Importantes del Día:", color = SlateAccent, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        
                                        dayExams.forEach { ex ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(SlateAccent.copy(alpha=0.08f))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Warning, null, tint = SlateAccent, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("[EXAMEN] ${ex.title}: ${ex.description}", color = SlateAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        dayEvents.forEach { ev ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF6366F1).copy(alpha=0.08f))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.School, null, tint = Color(0xFF6366F1), modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("[FECHA UC] ${ev.title}: ${ev.description}", color = Color(0xFF6366F1), fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // MODE B: HORIZONTAL HOUR-TIMELINE VIEW (HORARIO REJILLA)
                    val slotsList = listOf("08:00 - 10:00", "10:00 - 12:00", "12:00 - 14:00", "14:00 - 16:00", "16:00 - 18:00")
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(slotsList.size) { index ->
                            val slot = slotsList[index]
                            val matchingBlocks = scheduleBlocks.filter {
                                val sInit = it.startTime.substring(0, 2).toIntOrNull() ?: 0
                                val targetInit = slot.substring(0, 2).toIntOrNull() ?: 0
                                sInit == targetInit
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                                border = BorderStroke(1.dp, SlateBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "Rango de Horas: $slot", color = SlatePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    if (matchingBlocks.isEmpty()) {
                                        Text("Libre en todos los días programados.", color = SlateTextSecondary, fontSize = 11.sp)
                                    } else {
                                        matchingBlocks.sortedBy { daysOptions.indexOf(it.dayOfWeek) }.forEach { b ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(SlatePrimary.copy(alpha=0.15f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(b.dayOfWeek, color = SlatePrimary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("${b.courseName} (${b.room})", color = SlateTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Text("${b.startTime} - ${b.endTime}", color = SlateTextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 1: ACADEMIC CHANGES & CLASSROOM CONVERSION TRACKING
                if (academicChanges.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No se registraron cambios de aula u horarios.", color = SlateTextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(academicChanges) { change ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                                border = BorderStroke(1.dp, SlateBorder)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(SlateAccent.copy(alpha=0.12f))
                                                    .padding(6.dp)
                                            ) {
                                                Icon(Icons.Default.Warning, null, tint = SlateAccent, modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(change.courseName, color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("Modificación de Clase", color = SlateTextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                        
                                        IconButton(onClick = { showDeleteChangeConfirm = change }) {
                                            Icon(Icons.Default.DeleteOutline, "Borrar", tint = SlateTextSecondary)
                                        }
                                    }
                                    Divider(color = SlateBorder)

                                    // Old vs New details layout
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("AULA ANTERIOR:", color = SlateTextSecondary, fontSize = 9.sp)
                                            Text(change.oldRoom, color = SlateTextSecondary, fontSize = 12.sp, style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("HORARIO ANTERIOR:", color = SlateTextSecondary, fontSize = 9.sp)
                                            Text(change.oldSchedule, color = SlateTextSecondary, fontSize = 11.sp, style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                                        }

                                        Column(modifier = Modifier.weight(0.12f), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("➡", color = SlatePrimary, fontSize = 14.sp)
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("AULA NUEVA:", color = SlatePrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text(change.newRoom, color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("HORARIO NUEVO:", color = SlatePrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text(change.newSchedule, color = SlatePrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (change.description.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(SlateBorder)
                                                .padding(8.dp)
                                        ) {
                                            Text("Incidencia: ${change.description}", color = SlateTextPrimary, fontSize = 11.sp, lineHeight = 15.sp)
                                        }
                                    }
                                    
                                    Text(
                                        text = "Actualizado el: " + formatTimestamp(change.changeDate),
                                        color = SlateTextSecondary,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG: ADD/EDIT SCHEDULE BLOCK
    if (showBlockDialog) {
        Dialog(onDismissRequest = closeBlockDialog) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SlateSurface)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (blockToEdit == null) "Nuevo Bloque" else "Editar Bloque", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    
                    // Simple course picker drop emulator
                    Text("Selecciona el curso asignado:", fontSize = 11.sp, color = SlateTextSecondary, modifier = Modifier.align(Alignment.Start))
                    
                    if (courses.isEmpty()) {
                        Text("Ingresa curso primero en Mis Cursos", color = SlateAccent, fontSize = 11.sp)
                    } else {
                        // Horizontal selectors
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            items(courses) { cs ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selectedCourseId == cs.id) SlatePrimary.copy(alpha=0.15f) else SlateBorder)
                                        .border(1.dp, if (selectedCourseId == cs.id) SlatePrimary else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable { selectedCourseId = cs.id }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(cs.shortname, color = if (selectedCourseId == cs.id) SlatePrimary else SlateTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    OutlinedTextField(value = dayOfWeek, onValueChange = { dayOfWeek = it }, label = { Text("Día (Lunes, Martes...)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary), singleLine = true)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Inicio (HH:mm)") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary), singleLine = true)
                        OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("Fin (HH:mm)") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary), singleLine = true)
                    }
                    OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Aula") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary), singleLine = true)

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = closeBlockDialog) { Text("Cancelar", color = SlateTextSecondary) }
                        Button(
                            onClick = {
                                val cName = courses.find { it.id == selectedCourseId }?.fullname ?: "Curso $selectedCourseId"
                                if (blockToEdit != null) {
                                    viewModel.updateScheduleBlock(blockToEdit!!.id, selectedCourseId, cName, dayOfWeek, startTime, endTime, room)
                                } else {
                                    viewModel.addScheduleBlock(selectedCourseId, cName, dayOfWeek, startTime, endTime, room)
                                }
                                closeBlockDialog()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                        ) { Text("Guardar", color = SlateBackground, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    // DIALOG: RECORD AN ACADEMIC / SCHEDULE CLASSROOM SHIFT (AcademicChange)
    if (showAddChangeDialog) {
        Dialog(onDismissRequest = { showAddChangeDialog = false }) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SlateSurface)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Registrar Modificación de Aula / Horario", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    
                    Text("Selecciona el curso afectado:", fontSize = 11.sp, color = SlateTextSecondary, modifier = Modifier.align(Alignment.Start))
                    if (courses.isEmpty()) {
                        Text("No hay cursos registrados", color = SlateAccent, fontSize = 11.sp)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            items(courses) { cs ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (changeCourseId == cs.id) SlatePrimary.copy(alpha=0.15f) else SlateBorder)
                                        .border(1.dp, if (changeCourseId == cs.id) SlatePrimary else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable { changeCourseId = cs.id }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(cs.shortname, color = if (changeCourseId == cs.id) SlatePrimary else SlateTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    OutlinedTextField(value = oldRoom, onValueChange = { oldRoom = it }, label = { Text("Aula Antigua") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary), singleLine = true)
                    OutlinedTextField(value = newRoom, onValueChange = { newRoom = it }, label = { Text("Aula Nueva") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary), singleLine = true)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = oldSchedule, onValueChange = { oldSchedule = it }, label = { Text("Horario Viejo") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary), singleLine = true)
                        OutlinedTextField(value = newSchedule, onValueChange = { newSchedule = it }, label = { Text("Horario Nuevo") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary), singleLine = true)
                    }
                    OutlinedTextField(value = changeReason, onValueChange = { changeReason = it }, label = { Text("Detalle de Incidencia / Razón") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SlateTextPrimary, unfocusedTextColor = SlateTextPrimary))

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { showAddChangeDialog = false }) { Text("Atrás", color = SlateTextSecondary) }
                        Button(
                            onClick = {
                                val cName = courses.find { it.id == changeCourseId }?.fullname ?: "Asignatura UC"
                                viewModel.addAcademicChange(
                                    courseId = changeCourseId,
                                    courseName = cName,
                                    oldRoom = oldRoom,
                                    newRoom = newRoom,
                                    oldSched = oldSchedule,
                                    newSched = newSchedule,
                                    description = changeReason
                                )
                                showAddChangeDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                        ) { Text("Guardar", color = SlateBackground, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    // CONFIRM DELETE Horario Block
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Confirmar Eliminación", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text("¿Estás seguro de que deseas eliminar este bloque de horario?", color = SlateTextSecondary) },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteScheduleBlock(showDeleteConfirm!!.id)
                    showDeleteConfirm = null
                }, colors = ButtonDefaults.buttonColors(containerColor = SlateAccent)) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancelar", color = SlateTextSecondary) }
            },
            containerColor = SlateSurface
        )
    }

    // CONFIRM DELETE Academic Classroom/Schedule Change
    if (showDeleteChangeConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteChangeConfirm = null },
            title = { Text("Eliminar Modificación", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = { Text("¿Estás seguro de que deseas eliminar este reporte de cambio de aula?", color = SlateTextSecondary) },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteAcademicChange(showDeleteChangeConfirm!!.id)
                    showDeleteChangeConfirm = null
                }, colors = ButtonDefaults.buttonColors(containerColor = SlateAccent)) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChangeConfirm = null }) { Text("Cancelar", color = SlateTextSecondary) }
            },
            containerColor = SlateSurface
        )
    }
}

// -------------------------------------------------------------
// SCREEN 8: CALENDAR (H6)
// -------------------------------------------------------------
@Composable
fun CalendarScreen(viewModel: MoodleViewModel, navController: NavHostController) {
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val evaluations by viewModel.evaluations.collectAsStateWithLifecycle()
    val payments by viewModel.payments.collectAsStateWithLifecycle()
    val courses by viewModel.courses.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var selectedDayMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Dialog state
    var eventTitle by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf("EXAMEN") }
    var eventDesc by remember { mutableStateOf("") }
    var eventCourseId by remember { mutableStateOf(0) }

    val closeDialog = {
        showDialog = false
        eventTitle = ""
        eventType = "EXAMEN"
        eventDesc = ""
        eventCourseId = 0
    }

    val cal = Calendar.getInstance()
    cal.timeInMillis = selectedDayMillis
    val currentMonthTxt = DateFormat.format("MMMM yyyy", cal.time).toString().replaceFirstChar { it.uppercase() }

    // Aggregate everything into a daily map for the bottom view
    val allEvents = remember(calendarEvents, evaluations, payments) {
        val list = mutableListOf<CalendarEvent>()
        list.addAll(calendarEvents)
        evaluations.forEach { ev ->
            list.add(CalendarEvent(id = -ev.id, title = ev.title, dateMillis = ev.dueDate, type = "ALERT", courseId = ev.courseId, description = ev.description))
        }
        payments.forEach { pay ->
            list.add(CalendarEvent(id = -pay.id - 1000, title = pay.concept, dateMillis = pay.dueDate, type = "PAGO", courseId = 0, description = "Monto: s/. ${pay.amount}"))
        }
        list
    }

    val todayStr = DateFormat.format("yyyyMMdd", selectedDayMillis).toString()
    val todaysEvents = allEvents.filter { DateFormat.format("yyyyMMdd", it.dateMillis).toString() == todayStr }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = SlatePrimary,
                contentColor = SlateBackground
            ) {
                Icon(Icons.Default.Add, "Evento Manual")
            }
        }
    ) { paddingInfo ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingInfo).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = SlateTextPrimary) }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = "Calendario", color = SlateTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Eventos y evaluaciones", color = SlateTextSecondary, fontSize = 13.sp)
                }
            }

            // Simple Calendar Header Nav
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val c = Calendar.getInstance()
                    c.timeInMillis = selectedDayMillis
                    c.add(Calendar.DAY_OF_YEAR, -1)
                    selectedDayMillis = c.timeInMillis
                }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Prev", tint = SlatePrimary) }
                Text(DateFormat.format("dd MMMM yyyy", selectedDayMillis).toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SlateTextPrimary)
                IconButton(onClick = {
                    val c = Calendar.getInstance()
                    c.timeInMillis = selectedDayMillis
                    c.add(Calendar.DAY_OF_YEAR, 1)
                    selectedDayMillis = c.timeInMillis
                }) { Icon(Icons.Default.ArrowForward, "Next", tint = SlatePrimary) }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Simulated Month View Dots (Static decorative for now, focuses on Daily details)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Eventos para el día seleccionado:", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (todaysEvents.isEmpty()) {
                        Text("No hay eventos programados para esta fecha.", color = SlateTextSecondary, fontSize = 12.sp)
                    } else {
                        todaysEvents.forEach { ev ->
                            val colorTag = when(ev.type) {
                                "ALERT", "EXAMEN" -> SlateAccent
                                "PAGO" -> Color(0xFFEAB308)
                                else -> SlateSecondary
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(colorTag))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(ev.title, color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(ev.description, color = SlateTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = closeDialog) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SlateSurface)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Registrar Evento", color = SlateTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = eventTitle, onValueChange = { eventTitle = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = eventType, onValueChange = { eventType = it }, label = { Text("Tipo (EXAMEN, GENERAL, MATRICULA)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = eventDesc, onValueChange = { eventDesc = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = closeDialog) { Text("Cancelar", color = SlateTextSecondary) }
                        Button(
                            onClick = {
                                if (eventTitle.isNotBlank()) {
                                    viewModel.addCalendarEvent(eventTitle, selectedDayMillis, eventType, eventCourseId, eventDesc)
                                    closeDialog()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SlatePrimary)
                        ) { Text("Guardar") }
                    }
                }
            }
        }
    }
}


