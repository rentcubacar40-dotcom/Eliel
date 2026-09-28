# Plan de Implementación: Nexus Chat Profesional & Motor JSON Real

Nexus se transformará en una plataforma de mensajería moderna, 100% real y sin datos simulados. Se eliminará cualquier mención o formulario de Moodle en la interfaz gráfica (la comunicación de red operará silenciosamente en segundo plano con credenciales internas preconfiguradas), incorporando verificación de nombres de usuario únicos, validación de destinatarios al iniciar chats, grupo general predeterminado para toda la comunidad, creación y gestión de grupos con roles/permisos, visor de perfiles interactivo y una estética Cyber-Glass / Deep Dark OLED de última generación.

---

## 1. Resumen de Características y Arquitectura

### A. Limpieza de Interfaz (Cero Moodle en la UI)
* **`SetupProfileScreen.kt`**: Retirar por completo el desplegable de configuración de Moodle UCF, los campos de usuario/contraseña institucional y el botón de prueba de conexión. El registro es 100% nativo de Nexus.
* **`ProfileDrawer.kt`**: Retirar el diálogo de configuración de credenciales Moodle y la opción correspondiente en el menú lateral.
* **Manejo Interno**: Toda la sincronización de red con el endpoint institucional se mantendrá encapsulada en `UcfMoodleClient.kt` con configuración embebida sin pedir datos ni interrumpir al usuario.

### B. Registro con Verificación de Duplicados en Tiempo Real
* **Flujo de Registro**:
  * Selección de avatar mediante el *Android Photo Picker* nativo o iniciales con gradiente Nexus.
  * Nombre visible (`displayName`), nombre de usuario único (`@usuario`) y biografía/carrera.
* **Validación en Vivo**:
  * Comprueba si el handle `@usuario` ya está en uso en la base de datos local y directorio de paquetes.
  * Reconocimiento y distintivo especial para `@Eliel_21` como Creador y Propietario de la plataforma (👑).
  * Bloqueo del botón de avance y aviso en rojo si el usuario ya existe o tiene caracteres no permitidos.

### C. Verificación de Destinatarios en Chats Privados
* **Diálogo "Nuevo Chat Privado"**:
  * Al ingresar el `@usuario` del destinatario, se valida contra el registro de usuarios de la red.
  * Si el usuario **no existe**, se muestra el mensaje de error: *"El usuario @usuario no fue encontrado en la red"*.
  * Si el usuario **existe**, se precargan su foto y datos reales, creando y abriendo la conversación privada directa.

### D. Sistema de Grupos con Roles y Grupo General por Defecto
* **Grupo General por Defecto**:
  * Creación e inicialización automática en Room del canal público *"Nexus General • Comunidad UCF"* (ID: `group_nexus_general`), donde todos los usuarios registrados pertenecen de forma automática.
* **Grupos Personalizados**:
  * Diálogo de creación: Título del grupo, descripción, avatar y selección de miembros iniciales.
  * Roles configurables:
    * **Propietario / Creador (👑)**: Puede añadir/expulsar miembros, cambiar descripción y promover administradores.
    * **Administrador**: Puede añadir miembros y gestionar el chat.
    * **Miembro**: Puede leer, enviar mensajes y compartir archivos/fotos.
* **Panel de Información y Gestión del Grupo (`GroupInfoDialog.kt`)**:
  * Accesible desde la barra superior del chat grupal.
  * Botón para **"Añadir Miembro"** verificando que el `@usuario` exista.
  * Lista detallada de integrantes con insignias de rol y opciones de administración.

### E. Visor Interactivo de Perfiles de Usuario (`UserProfileDialog.kt`)
* Accesible al tocar la foto o nombre de cualquier usuario (en mensajes o lista de miembros).
* Muestra: Avatar en alta resolución, nombre para mostrar, handle `@usuario`, insignia oficial (👑 Creador / Admin / Miembro), biografía y botón directo para iniciar un chat privado uno a uno.

### F. Motor de Chat basado en Archivos JSON ("Johnson")
* Persistencia en Room con exportación/importación de paquetes JSON estructurados (`JsonSyncModels.kt`).
* Estados de entrega en tiempo real: `PENDING` (reloj), `SENT` (check simple) y `DELIVERED` / `READ` (doble check azul neón `#00F2FE`).
* Soporte para adjuntos reales (fotos y documentos hasta 4MB).

### G. Línea Visual "Nexus Cyber-Glass / Deep Dark OLED"
* Paleta: Fondos oscuros puros (`#0B0E14`, `#10141D`), acentos cian brillante (`#00F2FE`), azul eléctrico (`#0077FE`) y degradados violeta (`#7F00FF`).
* Efectos de cristal esmerilado en barras de navegación, burbujas de chat asimétricas y tipografía limpia de alta legibilidad.

---

## 2. Archivos a Modificar y Crear

| Archivo | Acción | Descripción |
|---|---|---|
| `app/src/main/java/com/example/ui/screens/SetupProfileScreen.kt` | Modificar | Retirar toda mención de Moodle. Implementar formulario limpio y validación instantánea de `@usuario`. |
| `app/src/main/java/com/example/ui/screens/ProfileDrawer.kt` | Modificar | Eliminar opciones y diálogos de Moodle. Añadir accesos limpios a Ajustes, Mi Perfil y Acerca de Nexus. |
| `app/src/main/java/com/example/ui/screens/ChatListScreen.kt` | Modificar | Integrar validación de existencia al crear chat privado, incorporar grupo general por defecto y modal de creación de grupos con miembros. |
| `app/src/main/java/com/example/ui/screens/ChatDetailScreen.kt` | Modificar | Añadir botón de información del grupo (lista de miembros, roles, añadir miembro) y visor de perfil al hacer clic en cualquier avatar. |
| `app/src/main/java/com/example/ui/components/UserProfileDialog.kt` | Crear | Modal interactivo para visualizar el perfil completo de cualquier usuario e iniciar chat privado directo. |
| `app/src/main/java/com/example/ui/screens/GroupInfoDialog.kt` | Crear | Diálogo completo con la información del grupo, roles (👑 Creador, Admin, Miembro), lista de participantes y acción de añadir miembros. |
| `app/src/main/java/com/example/ui/MainViewModel.kt` | Modificar | Lógica de verificación de usuario para chats privados, gestión de miembros de grupo, roles y aseguramiento del grupo general por defecto. |
| `app/src/main/java/com/example/data/AppDatabase.kt` / DAOs | Modificar | Soporte para roles de miembros de grupo y persistencia de usuarios registrados. |

---

## 3. Plan de Verificación

* **Compilación y Build:** Ejecutar `compile_applet` para garantizar que la compilación pase sin advertencias ni errores.
* **Pruebas Unitarias:** Ejecutar `gradle :app:testDebugUnitTest` para asegurar que las pruebas sigan en verde.
* **Verificación de Registro:** Validar que `@usuario` duplicado muestre error inmediato y que no haya ningún campo de Moodle en pantalla.
* **Verificación de Chats:** Validar que al intentar abrir chat con un usuario inexistente se impida y al poner uno existente se cree el chat con su perfil.
* **Verificación de Grupos:** Validar presencia de "Nexus General", creación de grupos, roles y visualización de perfiles.
