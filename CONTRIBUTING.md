# Manual de Contribución y Reglas del Repositorio

Este documento establece las normativas, el flujo de trabajo y los estándares de calidad para contribuir a este repositorio. Es de lectura obligatoria para todos los miembros del equipo antes de enviar código.

---

## 1. Reglas Estrictas del Repositorio (Configuradas en GitHub)

Para garantizar la integridad del código, el repositorio cuenta con reglas de protección a nivel de servidor (`Rulesets`) aplicadas a nuestras ramas principales:

*   **Bloqueo de Force Pushes:** Está estrictamente prohibido reescribir el historial público (`git push --force`).
*   **Restricción de Borrado:** Las ramas principales no pueden ser eliminadas.
*   **Requisito de Pull Request (PR):** Ningún commit puede ir directamente a las ramas principales. Todo código debe integrarse a través de un PR.
*   **Comprobaciones de Estado (Status Checks):** El código debe compilar correctamente (ej. sin errores en las builds de Spring Boot o fallos en los componentes de React) antes de habilitar el botón de *Merge*.

---

## 2. Estrategia de Ramas (Branching Model)

Trabajamos con un flujo basado en ramas por funcionalidad.

### Ramas Principales
*   `main`: Contiene el código de producción. Siempre debe ser estable y funcional.
*   `develop`: Rama de integración donde se unen las nuevas características antes de pasar a producción. (Opcional, dependiendo si el despliegue es continuo).

### Creación de Nuevas Ramas
Toda nueva rama debe crearse a partir del estado más reciente de la rama objetivo (generalmente `develop` o `main`) y seguir esta nomenclatura de prefijos:

*   `feature/` - Para nuevas características o requerimientos. (Ej: `feature/multi-tenant-auth`)
*   `bugfix/` - Para solucionar errores reportados durante el desarrollo. (Ej: `bugfix/react-render-loop`)
*   `hotfix/` - Para errores críticos en producción (Nacen de `main` y se fusionan en `main` y `develop`).
*   `chore/` - Para tareas de mantenimiento, actualización de dependencias o configuraciones.

---

## 3. Convención de Commits

Utilizamos el estándar de **Conventional Commits** para mantener un historial limpio, legible y facilitar la automatización de versiones.

**Estructura:**
`<tipo>: <descripción concisa en minúsculas y sin punto final>`

**Tipos permitidos:**
*   `feat`: Una nueva característica.
*   `fix`: Solución a un error (bug).
*   `refactor`: Cambio en el código que no corrige un error ni añade una característica (ej. optimización de código en Java).
*   `style`: Cambios de formato (espacios, punto y coma faltantes, etc.) que no afectan la lógica.
*   `docs`: Cambios exclusivos en la documentación.
*   `test`: Añadir o corregir pruebas unitarias/integración.

**Ejemplos Correctos:**
*   ✅ `feat: agregar endpoint para validación de usuarios`
*   ✅ `fix: corregir desbordamiento de memoria en el dashboard`
*   ✅ `refactor: extraer lógica de validación a un servicio independiente`

---

## 4. Proceso de Pull Requests (PR) y Revisiones

Todo el código nuevo debe ser revisado antes de integrarse. 

### Pasos para un PR exitoso:
1.  **Actualización previa:** Antes de crear el PR, asegúrate de tener tu rama actualizada con los últimos cambios (`git pull origin main` o `develop`).
2.  **Título y Descripción:** El título del PR debe seguir la misma regla de los commits. La descripción debe detallar el *qué* y el *por qué* del cambio. Si aplica, incluye capturas de pantalla de la interfaz.
3.  **Código Limpio:** 
    *   Elimina cualquier `console.log()`, `System.out.println()` o código comentado huérfano.
    *   Asegúrate de que no haya advertencias (warnings) críticas del linter.
4.  **Asignación de Revisores:** Debes asignar al menos a un miembro del equipo (Cristian, Roger, Roy o Emma) para que apruebe tu código. 
5.  **Aprobación requerida:** No puedes aprobar ni fusionar (merge) tu propio Pull Request. Requiere el visto bueno de un compañero.

### Política de Resolución de Conflictos
Si tu PR presenta conflictos de integración (`merge conflicts`), es **responsabilidad del autor del PR** resolverlos localmente antes de solicitar una nueva revisión.