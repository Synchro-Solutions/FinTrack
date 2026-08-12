# ⚙️ Guía de Configuración — FinTrack

Esta guía explica cómo configurar el entorno de desarrollo y ejecutar **FinTrack**, un proyecto **Kotlin Multiplatform (KMP)** con **Compose Multiplatform** que apunta a **Android** y **Web**.

> ℹ️ **Nota:** El proyecto está preparado como plantilla KMP, pero en esta entrega **solo se implementaron los targets de Android y Web**. El target de iOS **no** forma parte del alcance actual.

---

## 📋 Requisitos previos

Antes de empezar, asegúrate de tener instalado:

| Herramienta | Versión recomendada | Notas |
|-------------|--------------------|-------|
| **JDK** | 17 (o superior) | Necesario para Gradle y Kotlin. |
| **Android Studio** | Última estable (Ladybug o superior) | Con el plugin **Kotlin Multiplatform**. |
| **Android SDK** | API 24+ (mín.) / API 34 (target) | Se instala desde Android Studio. |
| **Git** | Última versión | Para clonar el repositorio. |
| **Navegador moderno** | Chrome / Edge / Firefox actualizado | Requerido para el target **Wasm** de la app web. |

> 💡 **Recomendado:** Ejecuta la herramienta [`kdoctor`](https://github.com/Kotlin/kdoctor) para verificar que tu entorno de KMP está correctamente configurado:
> ```bash
> brew install kdoctor   # macOS
> kdoctor
> ```

---

## 📥 1. Clonar el repositorio

```bash
git clone https://github.com/Synchro-Solutions/FinTrack.git
cd FinTrack
```

---

## 🔥 2. Configurar Firebase

FinTrack utiliza **Firebase Authentication** (email/contraseña, Google y recuperación de contraseña por email) y **Firebase Cloud Messaging (FCM)** para las notificaciones.

1. Crea un proyecto en la [Consola de Firebase](https://console.firebase.google.com/).
2. **Android:** registra la app con el `applicationId` del proyecto, descarga el archivo **`google-services.json`** y colócalo en:
   ```
   androidApp/google-services.json
   ```
3. **Web:** registra una app web en Firebase y copia la configuración (`firebaseConfig`) en el archivo de configuración correspondiente del módulo `webApp` (por ejemplo, variables de entorno o el archivo de config indicado en el código).
4. En la consola de Firebase, habilita en **Authentication → Sign-in method**:
   - **Correo electrónico/contraseña**
   - **Google**
5. (Opcional) Habilita **Cloud Messaging** para las notificaciones push.

> ⚠️ **Importante:** Los archivos de configuración de Firebase contienen claves del proyecto y **no deben** subirse al repositorio. Verifica que estén incluidos en el `.gitignore`.

---

## 📂 3. Estructura del proyecto

```
FinTrack/
├── androidApp/        # Punto de entrada de la aplicación Android
├── webApp/            # Punto de entrada de la aplicación Web (Wasm / JS)
└── shared/            # Código compartido entre todos los targets
    └── src/
        ├── commonMain/   # Código común para todas las plataformas
        ├── androidMain/  # Código específico de Android
        └── wasmJsMain/   # Código específico del target Web (Wasm)
```

- **`/shared`** contiene la lógica de negocio y la UI compartida con Compose Multiplatform.
- **`commonMain`** es el código común a todos los targets.
- Las demás carpetas (`androidMain`, `wasmJsMain`, etc.) son para código específico de cada plataforma.

---

## ▶️ 4. Ejecutar las aplicaciones

Puedes usar las **run configurations** que provee el _run widget_ en la barra de herramientas de tu IDE, o los siguientes comandos de Gradle:

### 🤖 App de Android
```bash
./gradlew :androidApp:assembleDebug
```
También puedes ejecutarla directamente desde Android Studio seleccionando la configuración **androidApp** y un emulador o dispositivo físico.

### 🌐 App Web
- **Target Wasm** (más rápido, navegadores modernos):
  ```bash
  ./gradlew :webApp:wasmJsBrowserDevelopmentRun
  ```
- **Target JS** (más lento, soporta navegadores más antiguos):
  ```bash
  ./gradlew :webApp:jsBrowserDevelopmentRun
  ```

Una vez iniciado, la app web quedará disponible en el navegador (por defecto en `http://localhost:8080`).

---

## 🧪 5. Ejecutar las pruebas

Puedes usar el botón de _run_ en el margen del editor de tu IDE, o las siguientes tareas de Gradle:

### Pruebas de Android
```bash
./gradlew :shared:testAndroidHostTest
```

### Pruebas de Web
- **Target Wasm:**
  ```bash
  ./gradlew :shared:wasmJsTest
  ```
- **Target JS:**
  ```bash
  ./gradlew :shared:jsTest
  ```

### Todas las pruebas del módulo compartido
```bash
./gradlew :shared:allTests
```

---

## 🛠️ 6. Solución de problemas comunes

| Problema | Solución |
|----------|----------|
| `SDK location not found` | Crea un archivo `local.properties` en la raíz con `sdk.dir=/ruta/al/Android/Sdk`. |
| Falla la compilación por Firebase | Verifica que `google-services.json` esté en `androidApp/` y que la config web esté correctamente cargada. |
| Gradle no sincroniza | Ejecuta `./gradlew --refresh-dependencies` o usa **File → Invalidate Caches** en Android Studio. |
| La app web no carga | Confirma que usas un navegador moderno para Wasm o cambia al target **JS**. |
| Permisos de `gradlew` en Linux/macOS | Ejecuta `chmod +x ./gradlew`. |

---

## 📚 Más información

- [Kotlin Multiplatform — Get Started](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform)
- [Kotlin/Wasm](https://kotl.in/wasm/)
- [Firebase Documentation](https://firebase.google.com/docs)

---

_¿Todo listo? Vuelve al [README.md](./README.md) para conocer más sobre FinTrack 🇨🇷._
