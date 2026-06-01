# Proyecto Taller Scrum - Alertas Académicas

Aplicación Android para la gestión de notificaciones académicas sincronizada con **Moodle Cloud**.

## Integrantes
* LUQUE CUSI, LUZ DIANA
* ALVARADO RAMÍREZ, OSCAR
* FLORES DALIA, GERSON DONATO
* BRAVO OLANO, RANDY PIERO

## Características (Historias de Usuario H1-H10)
1. **Sincronización Moodle Cloud:** Conexión vía API REST para obtener cursos y eventos.
2. **Alertas de Evaluación:** Notificaciones programadas para exámenes y tareas.
3. **Gestión de Pagos:** Control de cuotas administrativas y fechas de vencimiento.
4. **Horario Dinámico:** Visualización de clases y cambios de aula en tiempo real.
5. **Simulación de Notificaciones:** Sistema de prueba para recepción de alertas urgentes.

## Requisitos para Ejecución
1. Abrir el proyecto en **Android Studio**.
2. Configurar el acceso a Moodle en la pantalla de Ajustes:
   * **URL:** `https://univirtual2026.moodlecloud.com`
   * **Token:** Generado en Moodle (Servicios Web).
3. Compilar y ejecutar en un emulador o dispositivo físico.

## Tecnologías
* Kotlin + Jetpack Compose
* Retrofit 2.12 (Networking)
* Room DB (Persistencia Local)
* Coroutines & Flow
