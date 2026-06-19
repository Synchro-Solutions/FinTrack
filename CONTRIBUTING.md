# Manual de Contribución y Reglas del Repositorio

Este documento establece las normativas, el flujo de trabajo y los estándares de calidad para contribuir a este proyecto. Es de lectura obligatoria para todos los miembros del equipo antes de enviar código.

---

## 1. Acuerdos Fundamentales del Equipo

Dado que no contamos con bloqueos automáticos en la plataforma, la estabilidad del proyecto depende de la estricta disciplina de todos. **Ningún miembro debe evadir estas reglas bajo ninguna circunstancia:**

*   **Cero Commits Directos:** Está terminantemente prohibido hacer `git push` directamente a las ramas `main` o `develop`. Todo código sin excepción debe integrarse a través de un Pull Request (PR).
*   **Prohibición de Force Pushes:** Nunca utilices `git push --force` sobre las ramas principales, ya que esto destruye el historial de trabajo de los demás.
*   **Protección Manual de Ramas:** Las ramas `main` y `develop` son sagradas. No deben ser eliminadas ni alteradas directamente.
*   **Validación Local Obligatoria:** Como no tenemos verificaciones de estado (status checks) automáticas bloqueando los PRs, es responsabilidad absoluta del desarrollador asegurarse de que el código compila y levanta correctamente en su máquina (tanto el backend en Spring Boot como el frontend en React) antes de solicitar una revisión.

---

## 2. Estrategia de Ramas (Branching Model)

Trabajamos con un flujo basado en ramas por funcionalidad.

### Ramas Principales
*   `main`: Contiene el código de producción. Siempre debe ser estable y funcional.
*   `develop`: Rama de integración donde se unen las nuevas características antes de pasar a producción.

### Creación de Nuevas Ramas
Toda nueva rama debe crearse a partir del estado más reciente de la rama objetivo (generalmente `develop`) y seguir esta nomenclatura de prefijos:

*   `feature/` - Para nuevas características o requerimientos. (Ej: `feature/multi-tenant-auth`)
*   `bugfix/` - Para solucionar errores reportados durante el desarrollo. (Ej: `bugfix/react-render-loop`)
*   `hotfix/` - Para errores críticos en producción (Nacen de `main` y se fusionan en `main` y `develop`).
*   `chore/` - Para tareas de mantenimiento, actualización de dependencias o configuraciones.

---

## 3. Convención de Commits

Utilizamos el estándar de **Conventional Commits** para mantener un historial limpio, legible y facilitar el seguimiento del proyecto.

**Estructura:**
`<tipo>: <descripción concisa en minúsculas y sin punto final>`

**Tipos permitidos:**
*   `feat`: Una nueva característica.
*   `fix`: Solución a un error (bug).
*   `refactor`: Cambio en el código que no corrige un error ni añade una característica (ej. optimización de código en Java).
*   `style`: Cambios de formato (espacios, punto y coma faltantes, etc.) que no afectan la lógica.
*   `docs`: Cambios exclusivos en la documentación.
*   `chore`: Tareas del proceso de construcción, herramientas auxiliares, etc.

**Ejemplos Correctos:**
*   ✅ `feat: agregar endpoint para validación de usuarios`
*   ✅ `fix: corregir desbordamiento de memoria en el dashboard`
*   ✅ `refactor: extraer lógica de validación a un servicio independiente`

---

## 4. Proceso de Pull Requests (PR) y Revisiones

Todo el código nuevo debe ser revisado por un compañero antes de fusionarse.

### Pasos para un PR exitoso:
1.  **Actualización previa:** Antes de crear el PR, asegúrate de tener tu rama actualizada con los últimos cambios (`git pull origin develop`).
2.  **Título y Descripción:** El título del PR debe seguir la misma regla de los commits. La descripción debe detallar el *qué* y el *por qué* del cambio. Si aplica, incluye capturas de pantalla de la interfaz visual.
3.  **Código Limpio:** 
    *   Elimina cualquier `console.log()`, `System.out.println()` o código comentado que ya no se utilice.
    *   Asegúrate de que no haya advertencias (warnings) críticas en tu entorno de desarrollo.
4.  **Asignación de Revisores:** Solicita explícitamente la revisión de al menos un miembro del equipo (Cristian, Roger, Roy o Emma). 
5.  **Aprobación y Fusión:** Una vez que el PR tiene la aprobación (Approve) del compañero asignado, el autor del PR (o el revisor, según se acuerde en el momento) puede realizar el *Merge*. **No fusiones tu propio código sin aprobación.**

### Política de Resolución de Conflictos
Si tu PR presenta conflictos de integración (`merge conflicts`), es **responsabilidad del autor del PR** resolverlos localmente y probar que todo siga funcionando antes de solicitar que se complete la revisión.
