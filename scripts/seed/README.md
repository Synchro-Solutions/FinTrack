# Seed de datos de ejemplo (FinTrack)

Herramienta de desarrollo **aparte de la app** para poblar Firestore con datos de
ejemplo (perfil, categorías, transacciones de 6 meses, presupuestos, metas y
notificaciones) y así probar el dashboard, presupuestos, metas y la bandeja de
notificaciones sin capturar todo a mano.

Usa el **SDK cliente de Firebase**: inicia sesión con un usuario real y escribe **sus
propios** datos, así que respeta las reglas de seguridad y **no necesita clave de
servicio**, solo el `google-services.json` de la app.

## Requisitos

- Node.js 18+
- El `google-services.json` de la app (Android) copiado en esta carpeta.
- Un usuario de **email/contraseña** (no Google Sign-In) en el proyecto de Firebase.

## Pasos

1. Copia `google-services.json` (el mismo de `androidApp/`) a `scripts/seed/`.
   Ya está en `.gitignore`.

2. Instala dependencias:

   ```bash
   cd scripts/seed
   npm install
   ```

3. Ejecuta con las credenciales del usuario a poblar:

   ```bash
   node seed.js --email <correo> --password <clave>
   ```

   Para limpiar los datos previos del usuario antes de sembrar:

   ```bash
   node seed.js --email <correo> --password <clave> --fresh
   ```

   Las credenciales también pueden pasarse por `SEED_EMAIL` / `SEED_PASSWORD`.

## Qué crea

| Colección | Contenido |
|---|---|
| `users/{uid}` | Perfil demo (nombre, ingreso ₡950.000, moneda CRC, onboarding completo) |
| `categories` | Las 9 categorías de gasto de la app |
| `transactions` | ~6 meses de ingresos (salario) y gastos variados |
| `budgets` | 5 presupuestos del período actual (uno en alerta, uno excedido) |
| `goals` | 2 metas de ahorro activas |
| `notifications` | 3 notificaciones (2 sin leer, para ver el badge) |

Tras ejecutarlo, cierra sesión y vuelve a entrar (o refresca el dashboard) para ver
los datos.

## Notas

- El usuario debe ser de **email/contraseña**. Las cuentas creadas solo con Google
  Sign-In no sirven con este método; crea una de email/contraseña para pruebas o usa
  la variante con clave de servicio (Admin SDK) si la necesitas.
- Escribe en el Firestore **de producción** del proyecto (no el emulador).
