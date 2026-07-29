package fintrack.proyecto4.profile

/**
 * Cloud name y upload preset son seguros de tener en el cliente: el preset está
 * configurado como "Unsigned" en Cloudinary (Settings > Upload > Upload presets),
 * lo que permite subir archivos sin exponer la API secret en la app. Las reglas de
 * tamaño/formato/carpeta del preset se controlan del lado de Cloudinary, no aquí.
 */
object CloudinaryConfig {
    const val CLOUD_NAME = "cvami8c2"
    const val UPLOAD_PRESET = "fintrack_profile_photos"
    const val UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"
}
