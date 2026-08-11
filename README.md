# FinTrack 🇨🇷

_Aplicación móvil para tomar el control de tus finanzas personales, pensada para el contexto costarricense._

## 📌 Descripción

**FinTrack** es una aplicación móvil desarrollada por **Synchro Solutions** que reúne, en un solo lugar, las herramientas que una persona joven en Costa Rica necesita para ordenar su dinero: registro de ingresos y gastos, presupuestos por categoría, metas de ahorro, cálculos laborales propios del país (aguinaldo, salario neto, liquidación, cesantía) y un asistente financiero con inteligencia artificial.

Su objetivo es ofrecer una experiencia **sencilla, de bajo esfuerzo y adaptada a la realidad nacional** (colones, SINPE, cálculos del MTSS/CCSS), reemplazando la mezcla de hojas de cálculo, apps internacionales y sitios web separados que la mayoría usa hoy. Está construida con **Kotlin Multiplatform (KMP)** y **Compose Multiplatform**, con targets para **Android** y **Web**, compartiendo la lógica y la interfaz entre ambas plataformas.

## 🧩 Problemática

Según los datos de investigación recopilados para el proyecto:

- El **93 %** de los jóvenes costarricenses de 18 a 35 años está preocupado por cómo administra su dinero, y el **65 %** tiene algún tipo de deuda _(sondeo Coopenae-Wink, 2025)_.
- El **48 %** califica su conocimiento financiero como básico y el **58 %** destina menos del 10 % de sus ingresos al ahorro, pese a que el **63 %** reconoce su importancia _(Coopenae-Wink, 2025)_.
- El **47,4 %** de las personas recurriría a un préstamo para afrontar un gasto inesperado de ₡500.000, y solo una de cada tres podría cubrirlo con recursos propios _(encuesta Actualidades 2025, UCR)_.
- La alfabetización financiera del país ronda el **44 %** de la población adulta _(medición internacional de S&P)_.

A pesar de esta necesidad, no existe una herramienta única, en español, adaptada a los cálculos y la moneda de Costa Rica, que centralice el control del dinero de forma clara y accesible.

## 🎯 Objetivo

Brindar una herramienta digital que permita a la persona usuaria:

- Registrar y consultar sus ingresos y gastos de forma rápida, incluso capturando tiquetes con la cámara.
- Presupuestar por categoría y recibir alertas al acercarse a sus límites.
- Fijar metas de ahorro y dar seguimiento a su progreso.
- Realizar cálculos laborales propios de Costa Rica (aguinaldo, salario neto, liquidación, cesantía).
- Recibir recomendaciones e insights personalizados mediante inteligencia artificial.

## 🛠️ Funcionalidades

### 💸 Transacciones
- Registro manual de ingresos y gastos con categorías, adjuntos y notas.
- Historial con filtros, búsqueda global y edición de movimientos.
- Captura de tiquetes por **OCR** (CameraX + Google ML Kit): la app extrae los datos y la persona los confirma o edita.

### 📊 Presupuestos y metas
- Presupuestos mensuales por categoría con seguimiento del consumo.
- Alertas automáticas al acercarse o superar el límite.
- Metas de ahorro con objetivo y fecha, abonos, progreso detallado y celebración visual al completarlas.

### 🧮 Centro de Cálculos (Costa Rica)
- Aguinaldo, salario neto (deducciones CCSS/renta), liquidación, cesantía, vacaciones pendientes y preaviso.
- Conversión de montos entre colones y otras divisas con tipo de cambio vigente.

### 📈 Dashboard e informes
- Tarjetas de KPIs, gráfico de gastos por categoría y reporte de ingresos vs. gastos.
- Comparativas entre meses y tendencia de ahorro de los últimos 6 meses.

### 🤖 Asistente financiero con IA
- Chat de asistencia financiera y consejos de ahorro personalizados.
- Resumen financiero mensual, predicción de gastos del próximo mes y detección de gastos inusuales.

### 🔔 Notificaciones y ajustes
- Notificaciones push (FCM) para alertas de presupuesto y recordatorios.
- Pantalla de ajustes con preferencias de notificaciones, perfil y gestión de la cuenta.

## ⚙️ Setup del Proyecto

Para configurar el entorno de desarrollo y ejecutar el proyecto, sigue las instrucciones detalladas en la guía de configuración:

➡️ **[Guía de Configuración (SETUP.md)](./SETUP.md)**

**Requisitos previos (resumen):**
- **JDK 17** y **Android Studio** (última versión estable) con el plugin de **Kotlin Multiplatform**.
- Un **navegador moderno** (Chrome/Edge/Firefox) para ejecutar el target Web (Wasm).
- Archivo de configuración de **Firebase**: `google-services.json` (Android) y la config web en el módulo `webApp`.

**Inicio rápido:**
```bash
# 1. Clonar el repositorio
git clone https://github.com/Synchro-Solutions/FinTrack.git
cd FinTrack

# 2. Ejecutar la app de Android
./gradlew :androidApp:assembleDebug

# 3. Ejecutar la app Web (Wasm)
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

## 🧱 Tecnologías

- **Kotlin Multiplatform (KMP)** — lógica de negocio compartida entre Android y Web.
- **Compose Multiplatform** — interfaz de usuario declarativa compartida (Android + Web).
- **Kotlin/Wasm** — compilación del target Web para navegadores modernos.
- **Firebase Authentication** — autenticación de usuarios (email/contraseña y Google).
- **SQLDelight (SQLite)** — base de datos local con consultas tipo-seguras.
- **Google ML Kit + CameraX** — reconocimiento óptico (OCR) de tiquetes.
- **Firebase Cloud Messaging (FCM)** — notificaciones push.

## 🔒 Autenticación

FinTrack requiere una cuenta para proteger la información financiera de la persona usuaria. Se admite:

- **Registro e inicio de sesión** con email y contraseña.
- **Inicio de sesión con Google.**
- **Recuperación de contraseña por email** (US-04): la persona solicita el restablecimiento y recibe un enlace seguro en su correo, siguiendo el flujo de recuperación de Firebase Authentication.

La gestión de la cuenta (editar perfil, cambiar contraseña, eliminar cuenta y borrar datos personales) está disponible desde la pantalla de Ajustes.

## 🧪 Metodología

Este proyecto se desarrolla bajo la metodología ágil **Scrum**, con **sprints semanales** que permiten entregas iterativas y ajustes frecuentes. Para la priorización se utiliza **MoSCoW** y para la estimación de esfuerzo **T-Shirt Sizing**, gestionando todo el backlog, las épicas y las historias de usuario en **GitHub Projects**.

## 👥 Equipo — Synchro Solutions

- Barrantes Moraga, L.
- Pérez Boza, J.
- Quirós Artavia, R.
- Salazar Lobo, A.
- Villegas, M.

---

_FinTrack 🇨🇷 — porque ordenar tu dinero debería ser tan fácil como usar tu teléfono._
