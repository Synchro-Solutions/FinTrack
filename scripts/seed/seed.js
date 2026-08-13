#!/usr/bin/env node
/*
 * Seed de datos de ejemplo para FinTrack (Firestore) — pensado para DEMO.
 *
 * Usa el SDK CLIENTE de Firebase (no Admin): lee la configuración de
 * google-services.json, inicia sesión con el email/contraseña de un usuario real y
 * escribe SUS PROPIOS datos (users/{uid}/...). Como cada usuario puede escribir su
 * propia información, las reglas de seguridad lo permiten sin clave de servicio.
 *
 * Requiere Node 18+ (el SDK de Firebase usa globals de esa versión).
 *
 * Uso:
 *   1. Copia google-services.json (Android) a esta carpeta (scripts/seed/).
 *   2. npm install            (instala el paquete `firebase`)
 *   3. node seed.js --email <correo> --password <clave> [--fresh]
 *
 *      --email/--password  Credenciales del usuario a poblar (o SEED_EMAIL/SEED_PASSWORD).
 *                          Debe ser una cuenta de email/contraseña (no Google Sign-In).
 *      --fresh             Borra las subcolecciones del usuario antes de sembrar.
 */

const path = require("path");
const { initializeApp } = require("firebase/app");
const { getAuth, signInWithEmailAndPassword } = require("firebase/auth");
const {
  getFirestore,
  doc,
  collection,
  writeBatch,
  getDocs,
  setDoc,
} = require("firebase/firestore");

// ── Argumentos ───────────────────────────────────────────────────────────────
const args = process.argv.slice(2);
function argValue(flag) {
  const i = args.indexOf(flag);
  return i >= 0 && i + 1 < args.length ? args[i + 1] : null;
}
const EMAIL = argValue("--email") || process.env.SEED_EMAIL;
const PASSWORD = argValue("--password") || process.env.SEED_PASSWORD;
const FRESH = args.includes("--fresh");

if (!EMAIL || !PASSWORD) {
  console.error(
    "ERROR: faltan credenciales.\n" +
      "Usa: node seed.js --email <correo> --password <clave> [--fresh]"
  );
  process.exit(1);
}

// ── Config desde google-services.json ────────────────────────────────────────
let google;
try {
  google = require(path.join(__dirname, "google-services.json"));
} catch (e) {
  console.error(
    "ERROR: no se encontró scripts/seed/google-services.json.\n" +
      "Copia el mismo que usa la app Android en esta carpeta."
  );
  process.exit(1);
}

const client = (google.client || [])[0] || {};
const firebaseConfig = {
  apiKey: client.api_key && client.api_key[0] && client.api_key[0].current_key,
  authDomain: `${google.project_info.project_id}.firebaseapp.com`,
  projectId: google.project_info.project_id,
  storageBucket: google.project_info.storage_bucket,
  messagingSenderId: google.project_info.project_number,
  appId: client.client_info && client.client_info.mobilesdk_app_id,
};

if (!firebaseConfig.apiKey || !firebaseConfig.projectId) {
  console.error("ERROR: google-services.json no tiene apiKey/projectId válidos.");
  process.exit(1);
}

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

// ── Helpers de fecha ─────────────────────────────────────────────────────────
const now = new Date();

/** Fecha en el formato "dd/MM/yyyy" que usa la app para el campo `date`. */
function fmtDate(d) {
  const dd = String(d.getDate()).padStart(2, "0");
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  return `${dd}/${mm}/${d.getFullYear()}`;
}

/** periodKey "YYYY-MM" del mes de una fecha. */
function periodKeyOf(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}

/** periodKey a partir de una fecha "dd/MM/yyyy". */
function periodKeyOfAppDate(date) {
  const [, mm, yyyy] = date.split("/");
  return `${yyyy}-${mm}`;
}

/** Fecha desplazada `monthsAgo` meses y `day` del mes. */
function dateMonthsAgo(monthsAgo, day) {
  return new Date(now.getFullYear(), now.getMonth() - monthsAgo, day, 10, 0, 0);
}

const currentPeriodKey = periodKeyOf(now);

// ── Utilidades ───────────────────────────────────────────────────────────────
function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}
function randAmount(min, max) {
  // Redondeado a la centena, como montos reales en colones.
  return Math.round((min + Math.random() * (max - min)) / 100) * 100;
}
function roundTo(value, step) {
  return Math.max(step, Math.round(value / step) * step);
}

// ── Catálogo de categorías (alineado con BUDGET_CATEGORIES de la app) ─────────
const CATEGORIES = [
  { name: "Alimentación", icon: "🛒", colorHex: "#1F6B54" },
  { name: "Transporte", icon: "🚌", colorHex: "#26705B" },
  { name: "Vivienda", icon: "🏠", colorHex: "#2F7D65" },
  { name: "Servicios", icon: "⚡", colorHex: "#3B8F74" },
  { name: "Salud", icon: "🍎", colorHex: "#4FA184" },
  { name: "Entretenimiento", icon: "🎮", colorHex: "#5FAF93" },
  { name: "Ropa", icon: "👕", colorHex: "#62C9A7" },
  { name: "Educación", icon: "📚", colorHex: "#7DD4B7" },
  { name: "Otro", icon: "📦", colorHex: "#5A9683" },
];

// Comercios/gastos reales de Costa Rica por categoría, con rangos de monto plausibles.
const EXPENSE_MODEL = {
  Alimentación: {
    range: [6000, 58000],
    perMonth: [4, 6],
    desc: ["Automercado", "Walmart", "Más x Menos", "Palí", "Feria del agricultor", "Súper La Amistad", "Panadería Musmanni"],
    method: ["CARD", "SINPE", "CASH"],
  },
  Transporte: {
    range: [2500, 27000],
    perMonth: [3, 5],
    desc: ["Gasolina", "Uber", "Recarga bus", "Peaje Ruta 27", "Parqueo", "DIDI"],
    method: ["CARD", "CASH", "SINPE"],
  },
  Servicios: {
    range: [8000, 42000],
    perMonth: [3, 4],
    desc: ["ICE - Electricidad", "AyA - Agua", "Kölbi Internet", "Netflix", "Spotify", "Recarga celular"],
    method: ["CARD", "TRANSFER"],
  },
  Salud: {
    range: [5000, 38000],
    perMonth: [1, 2],
    desc: ["Farmacia Fischel", "Consulta médica", "Farmacia La Bomba", "Laboratorio Clínico", "Óptica"],
    method: ["CARD", "CASH", "SINPE"],
  },
  Entretenimiento: {
    range: [4000, 34000],
    perMonth: [3, 5],
    desc: ["Cinépolis", "Restaurante", "Café Britt", "McDonald's", "Bar", "Rappi"],
    method: ["CARD", "SINPE", "CASH"],
  },
  Ropa: {
    range: [12000, 62000],
    perMonth: [0, 1],
    desc: ["Zara", "Universal", "Aldo", "Tienda de ropa"],
    method: ["CARD"],
  },
  Educación: {
    range: [15000, 80000],
    perMonth: [0, 1],
    desc: ["Curso Platzi", "Libros", "Mensualidad Universidad", "Materiales de estudio"],
    method: ["CARD", "TRANSFER"],
  },
};

const RENT_AMOUNT = 260000; // Alquiler mensual (categoría Vivienda).
const SALARY = 850000; // Salario mensual (ingreso).

// ── Generadores de datos ─────────────────────────────────────────────────────

/** Perfil del usuario (documento users/{uid}). */
function buildProfile() {
  return {
    name: "Mariana Rodríguez Jiménez",
    photoPath: "",
    income: SALARY,
    currency: "CRC",
    onboardingComplete: true,
    budgetAlertEnabled: true,
  };
}

/** Transacciones realistas de los últimos 6 meses. */
function buildTransactions() {
  const txs = [];
  const between = ([lo, hi]) => lo + Math.floor(Math.random() * (hi - lo + 1));

  for (let monthsAgo = 5; monthsAgo >= 0; monthsAgo--) {
    // Salario (quincena/mes).
    const salaryDate = dateMonthsAgo(monthsAgo, 1);
    txs.push({
      type: "INCOME",
      amount: SALARY,
      description: "Salario",
      category: "Salario",
      paymentMethod: "TRANSFER",
      date: fmtDate(salaryDate),
      createdAt: salaryDate.getTime(),
      receiptUrl: null,
    });

    // Ingreso extra ocasional (freelance).
    if (Math.random() < 0.4) {
      const d = dateMonthsAgo(monthsAgo, 12 + Math.floor(Math.random() * 8));
      txs.push({
        type: "INCOME",
        amount: randAmount(60000, 220000),
        description: "Proyecto freelance",
        category: "Otro",
        paymentMethod: "SINPE",
        date: fmtDate(d),
        createdAt: d.getTime(),
        receiptUrl: null,
      });
    }

    // Alquiler (Vivienda), una vez al mes.
    const rentDate = dateMonthsAgo(monthsAgo, 2);
    txs.push({
      type: "EXPENSE",
      amount: RENT_AMOUNT,
      description: "Alquiler",
      category: "Vivienda",
      paymentMethod: "TRANSFER",
      date: fmtDate(rentDate),
      createdAt: rentDate.getTime(),
      receiptUrl: null,
    });

    // Gastos por categoría.
    for (const [cat, model] of Object.entries(EXPENSE_MODEL)) {
      const n = between(model.perMonth);
      for (let i = 0; i < n; i++) {
        const day = 3 + Math.floor(Math.random() * 25);
        const d = dateMonthsAgo(monthsAgo, day);
        txs.push({
          type: "EXPENSE",
          amount: randAmount(model.range[0], model.range[1]),
          description: pick(model.desc),
          category: cat,
          paymentMethod: pick(model.method),
          date: fmtDate(d),
          createdAt: d.getTime(),
          receiptUrl: null,
        });
      }
    }
  }
  return txs;
}

/** Suma de gastos del mes actual por categoría (para presupuestos coherentes). */
function currentMonthSpentByCategory(txs) {
  const spent = {};
  for (const t of txs) {
    if (t.type === "EXPENSE" && periodKeyOfAppDate(t.date) === currentPeriodKey) {
      spent[t.category] = (spent[t.category] || 0) + t.amount;
    }
  }
  return spent;
}

/**
 * Presupuestos del período actual, con `spent` = gasto real del mes y límites
 * calibrados para el demo: Entretenimiento excedido, Alimentación en alerta (~88%),
 * el resto en verde.
 */
function buildBudgets(spentMap) {
  // ratio = spent / limit objetivo. >1 => excedido.
  const defs = [
    { name: "Alimentación", ratio: 0.88 },
    { name: "Transporte", ratio: 0.55 },
    { name: "Vivienda", ratio: 0.7 },
    { name: "Servicios", ratio: 0.6 },
    { name: "Entretenimiento", ratio: 1.13 }, // excedido
    { name: "Salud", ratio: 0.4 },
  ];
  return defs.map((b) => {
    const cat = CATEGORIES.find((c) => c.name === b.name);
    const spent = spentMap[b.name] || randAmount(20000, 60000);
    const limit = roundTo(spent / b.ratio, 5000);
    return {
      categoryName: b.name,
      categoryIcon: cat.icon,
      categoryColorHex: cat.colorHex,
      spent,
      limit,
      period: "mensual",
      alertThreshold: 0.8,
      periodKey: currentPeriodKey,
      isActive: true,
      updatedAt: now.getTime(),
      alertSent: false,
      exceededSent: false,
    };
  });
}

/** Metas de ahorro (goals). */
function buildGoals() {
  return [
    {
      name: "Fondo de emergencia",
      targetAmount: 1500000,
      currentAmount: 620000,
      deadline: fmtDate(dateMonthsAgo(-6, 15)),
      iconName: "🛟",
      status: "ACTIVE",
      category: "EMERGENCY",
      colorName: "GREEN",
      priority: "HIGH",
      notes: "Ahorro de 3 meses de gastos.",
    },
    {
      name: "Viaje a Europa",
      targetAmount: 1200000,
      currentAmount: 340000,
      deadline: fmtDate(dateMonthsAgo(-8, 20)),
      iconName: "✈️",
      status: "ACTIVE",
      category: "TRAVEL",
      colorName: "BLUE",
      priority: "MEDIUM",
      notes: "Vacaciones de fin de año.",
    },
    {
      name: "Cambio de laptop",
      targetAmount: 700000,
      currentAmount: 500000,
      deadline: fmtDate(dateMonthsAgo(-3, 10)),
      iconName: "💻",
      status: "ACTIVE",
      category: "TECHNOLOGY",
      colorName: "PURPLE",
      priority: "LOW",
      notes: "Para trabajo y estudio.",
    },
  ];
}

/** Notificaciones de ejemplo (US-43): dos sin leer para ver el badge. */
function buildNotifications() {
  const h = 60 * 60 * 1000;
  return [
    {
      type: "BUDGET_EXCEEDED",
      title: "Presupuesto excedido",
      body: "Superaste tu presupuesto de Entretenimiento este mes.",
      read: false,
      createdAt: now.getTime() - 2 * h,
    },
    {
      type: "BUDGET_ALERT",
      title: "Cerca del límite",
      body: "Vas por el 88% de tu presupuesto de Alimentación.",
      read: false,
      createdAt: now.getTime() - 9 * h,
    },
    {
      type: "BUDGET_ALERT",
      title: "Resumen semanal",
      body: "Revisa tus gastos de la semana en el panel.",
      read: true,
      createdAt: now.getTime() - 30 * h,
    },
  ];
}

// ── Escritura (SDK cliente, en lotes de <= 400) ──────────────────────────────
function subcol(uid, name) {
  return collection(db, "users", uid, name);
}

async function deleteSubcollection(uid, name) {
  const snap = await getDocs(subcol(uid, name));
  if (snap.empty) return 0;
  let batch = writeBatch(db);
  let count = 0;
  for (const d of snap.docs) {
    batch.delete(d.ref);
    count++;
    if (count % 400 === 0) {
      await batch.commit();
      batch = writeBatch(db);
    }
  }
  await batch.commit();
  return count;
}

async function writeCollection(uid, name, items) {
  let batch = writeBatch(db);
  let count = 0;
  for (const item of items) {
    batch.set(doc(subcol(uid, name)), item);
    count++;
    if (count % 400 === 0) {
      await batch.commit();
      batch = writeBatch(db);
    }
  }
  await batch.commit();
  return count;
}

async function main() {
  console.log(`Iniciando sesión como ${EMAIL}...`);
  const cred = await signInWithEmailAndPassword(auth, EMAIL, PASSWORD);
  const uid = cred.user.uid;
  console.log(`OK. uid=${uid} (período ${currentPeriodKey})`);

  if (FRESH) {
    for (const c of ["transactions", "budgets", "goals", "categories", "notifications"]) {
      const n = await deleteSubcollection(uid, c);
      console.log(`  borrados ${n} de ${c}`);
    }
  }

  // Perfil (merge para no pisar otros campos existentes del usuario).
  await setDoc(doc(db, "users", uid), buildProfile(), { merge: true });
  console.log("  perfil actualizado");

  const categories = CATEGORIES.map((c) => ({ name: c.name, type: "EXPENSE", icon: c.icon }));
  const transactions = buildTransactions();
  const budgets = buildBudgets(currentMonthSpentByCategory(transactions));

  const results = {
    categories: await writeCollection(uid, "categories", categories),
    transactions: await writeCollection(uid, "transactions", transactions),
    budgets: await writeCollection(uid, "budgets", budgets),
    goals: await writeCollection(uid, "goals", buildGoals()),
    notifications: await writeCollection(uid, "notifications", buildNotifications()),
  };

  for (const [k, v] of Object.entries(results)) {
    console.log(`  ${v} ${k} creados`);
  }
  console.log("Listo ✅  (cierra sesión y vuelve a entrar en la app para verlos)");
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("Falló el seed:", err && err.message ? err.message : err);
    process.exit(1);
  });
