package fintrack.proyecto4.liquidacion

/**
 * Textos de la calculadora de liquidación laboral. Centralizados aquí para no duplicar strings
 * entre distintas partes de la UI, mismo patrón que [fintrack.proyecto4.vacation.VacationMessages].
 */
object LiquidacionMessages {
    const val SUBTITLE =
        "Estime el preaviso y la cesantía que le corresponden al terminar la relación laboral, según el Código de Trabajo de Costa Rica."

    const val LEGAL_BASIS =
        "Base legal: Código de Trabajo Arts. 28 (preaviso) y 29 (auxilio de cesantía). Este es un ESTIMADO informativo, no un documento legal ni una liquidación oficial — verifique el monto final con su departamento de RRHH, un contador o un profesional en derecho laboral."

    const val SALARY_HELPER =
        "Promedio de salarios devengados en los últimos 6 meses (Art. 30). Si trabajó menos de 6 meses, use el promedio del tiempo laborado."

    const val ABOUT_CALCULATOR_TITLE = "Sobre esta calculadora"

    const val ABOUT_CALCULATOR =
        "Calcula el preaviso (Art. 28) y el auxilio de cesantía (Art. 29) del Código de Trabajo de Costa Rica, según la antigüedad y el motivo de salida. La cesantía solo aplica en despidos sin justa causa, con un tope de 8 años reconocidos aunque la antigüedad real sea mayor. Las vacaciones y el aguinaldo proporcionales NO se incluyen aquí — use las calculadoras de Vacaciones y Aguinaldo del Centro Financiero para esos montos. La tabla de cesantía usada es la más citada por fuentes especializadas en derecho laboral costarricense; ante cualquier duda, confirme la tabla vigente con el Ministerio de Trabajo y Seguridad Social (MTSS) o un profesional."

    const val ABOUT_CALCULATOR_LAST_UPDATED = "Última actualización: 9 de agosto de 2026"

    const val EMPTY_RESULT_PLACEHOLDER = "Ingrese las fechas y el salario para ver el resultado"

    const val RENUNCIA_NOTICE =
        "En una renuncia voluntaria no se genera derecho a cesantía ni a preaviso pagado por el patrono (Art. 28 y 29)."

    const val DESPIDO_JUSTA_CAUSA_NOTICE =
        "En un despido con justa causa se pierde el derecho a cesantía y preaviso (Art. 81)."
}
