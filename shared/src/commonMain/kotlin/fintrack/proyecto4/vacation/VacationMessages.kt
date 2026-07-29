package fintrack.proyecto4.vacation

/**
 * Textos de la calculadora de vacaciones, alineados con la calculadora del Ministerio de Trabajo
 * y Seguridad Social de Costa Rica. Centralizados aqui para no duplicar strings entre distintas
 * partes de la UI.
 */
object VacationMessages {
    const val SUBTITLE =
        "Calcule los días y el monto de vacaciones según su antigüedad y jornada laboral en Costa Rica."

    const val LEGAL_BASIS =
        "Base legal: Constitución Política Art. 59, Código de Trabajo Arts. 153-161. Derecho: 2 semanas de vacaciones remuneradas por cada 50 semanas de labores continuas."

    const val SALARY_HELPER =
        "Promedio de remuneraciones ordinarias y extraordinarias de las últimas 50 semanas (Art. 157)."

    const val ABOUT_CALCULATOR_TITLE = "Sobre esta calculadora"

    const val ABOUT_CALCULATOR =
        "La calculadora de vacaciones determina los días de descanso remunerado y el monto a pagar conforme a los artículos 153 a 156 del Código de Trabajo de Costa Rica. El derecho a vacaciones se genera tras 50 semanas continuas de trabajo con el mismo patrono. Para calcular el monto, se promedian los salarios ordinarios de ese período. La herramienta también permite calcular vacaciones proporcionales cuando la relación laboral no ha completado el período de 50 semanas."

    const val ABOUT_CALCULATOR_LAST_UPDATED = "Última actualización: 15 de enero de 2026"

    const val EMPTY_RESULT_PLACEHOLDER = "Ingrese las fechas para ver el resultado"

    const val PROPORTIONAL_NOTICE =
        "Aún no se completan las cincuenta semanas continuas; el resultado es un proporcional estimado."
}
