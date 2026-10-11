# Gestor de inventario

Aplicación de escritorio para controlar el inventario de una tienda pequeña: productos con categoría y proveedor, un historial de movimientos (entradas, salidas y ajustes) que mantiene el stock, alertas de stock bajo e inicio de sesión con roles.

> **Estado:** en desarrollo. Ver [Hoja de ruta](#hoja-de-ruta).

![Inicio de sesión](docs/capturas/login.png)
![Movimientos con kardex](docs/capturas/movimientos.png)

## Tecnologías

- **Java 21** y **JavaFX 21** (interfaz gráfica)
- **SQLite** con sqlite-jdbc
- **Maven**
- **BCrypt** (at.favre.lib) para las contraseñas

## Funcionalidades

- **Catálogo:** categorías, proveedores (con teléfono opcional) y productos con precio y stock mínimo.
- **Nombres sin duplicados:** "Lácteos", "LACTEOS" y "lacteos" se consideran el mismo nombre. Se guarda una clave normalizada (sin mayúsculas, tildes ni espacios de más) con un índice único, conservando la ñ como letra propia ("Peña" ≠ "Pena").
- **Movimientos de stock:** entradas, salidas y ajustes por conteo físico. El stock solo cambia a través de movimientos.
- **Kardex:** cada movimiento guarda el stock anterior y el resultante.
- **Alertas de stock bajo:** filas resaltadas y contador en la pantalla de productos; resumen en vivo antes de registrar un movimiento.
- **Inicio de sesión:** creación del primer administrador al iniciar la aplicación por primera vez y login con usuario y contraseña.

## Decisiones de diseño

### Arquitectura por capas (MVC)

```
com.inventario
├── modelo        Entidades y reglas de validación
├── dao           Acceso a datos (SQL)
├── vista         Pantallas JavaFX, sin lógica de datos
├── controlador   Conecta cada vista con sus DAO
└── util          Validaciones y manejo de contraseñas
```

Regla de dependencias: la vista conoce al modelo pero nunca a los DAO; el controlador conoce vista, DAO y modelo; el DAO conoce al modelo.

### Integridad de los datos

- Todo el SQL usa `PreparedStatement`.
- Las operaciones que tocan varias tablas usan una sola transacción con `commit` y `rollback`.
- **Salidas:** `UPDATE ... WHERE stock_actual >= ? RETURNING stock_actual`. La base rechaza una salida sin stock suficiente, y el stock resultante se obtiene en la misma sentencia que lo modifica.
- **Ajustes:** se informa el stock contado y la diferencia se calcula dentro de la transacción, con bloqueo optimista (`WHERE stock_actual = ?`) para detectar si el stock cambió mientras se contaba.
- **Migraciones:** al iniciar, la aplicación agrega las columnas que falten (`ALTER TABLE`) para actualizar bases existentes sin perder datos.

El kardex se puede auditar con una consulta. Si devuelve filas, hay movimientos inconsistentes:

```sql
SELECT * FROM movimientos
WHERE stock_resultante - stock_anterior <>
      CASE tipo WHEN 'SALIDA' THEN -cantidad ELSE cantidad END;
```

### Seguridad

- **Contraseñas con BCrypt** (costo 12): se guarda solo un hash lento con sal aleatoria, nunca la contraseña.
- **Sin credenciales por defecto:** el primer administrador se crea en la primera ejecución; no existe un `admin/admin`.
- **Mensaje único de error en el login:** "Usuario o contraseña incorrectos", para no revelar qué usuarios existen.
- **El hash no sale de la capa de datos:** el modelo `Usuario` no lo contiene.
- **Reglas de contraseña según NIST:** mínimo 8 caracteres, sin exigir símbolos; máximo 72 bytes por el límite de BCrypt.

**Alcance (modelo de amenazas):** el login protege los flujos de la aplicación. Quien tenga acceso directo al archivo `inventario.db` puede leerlo o modificarlo con otras herramientas; protegerlo por completo requeriría una arquitectura cliente-servidor.

## Cómo ejecutarlo

Requisitos: JDK 21 y Maven.

```bash
git clone <url-del-repositorio>
cd inventario-app
mvn clean javafx:run
```

La base de datos `inventario.db` se crea en la carpeta del proyecto. En la primera ejecución, la aplicación pide crear la cuenta de administrador.

## Hoja de ruta

- [x] Catálogo de categorías, proveedores y productos
- [x] Movimientos con transacciones y kardex
- [x] Inicio de sesión con BCrypt
- [ ] Roles ADMIN y VENDEDOR *(en curso)*
- [ ] Bloqueo temporal tras intentos fallidos
- [ ] Gestión de usuarios
- [ ] Búsqueda y filtros
- [ ] Reportes en PDF y exportación a CSV
- [ ] Pruebas con JUnit y registro de errores
- [ ] Instaladores para Linux y Windows
