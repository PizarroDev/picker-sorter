# Mini Picker

Burbuja flotante para Android que lee tu app de picking y agrupa los productos por categoría, en el orden de tu recorrido por la tienda.

En muchas apps de picking los productos de una misma categoría quedan repartidos por la lista: la leche al principio, el yogur al final. Así terminas pasando dos veces por el mismo pasillo. Mini Picker lee la lista que ya tienes en pantalla y te la muestra ordenada, sin tocar la otra app.

- **Liviana:** APK de ~60 KB, sin librerías externas (solo Kotlin y vistas nativas de Android).
- **Privada:** funciona sin internet, no pide permiso de red y no envía datos a ningún lado.
- **Solo lee:** no presiona botones ni cambia nada en tu app de picking.
- **Bajo consumo:** solo trabaja mientras la app de picking está en primer plano.

## Cómo funciona

1. Un **servicio de accesibilidad** lee los textos que muestra tu app de picking mientras recorres el pedido.
2. Mini Picker aprende dónde están los nombres de producto y clasifica cada uno según **palabras clave** (por ejemplo `leche, yogur, queso` → Lácteos).
3. Una **burbuja flotante** muestra cuántos productos quedan. Al tocarla se abre la lista agrupada por categoría, con el progreso del pedido.

En la burbuja:

| Acción | Resultado |
|---|---|
| Tocar un producto | Lo marca como recogido |
| Mantener presionado un texto | Lo ignora para siempre (útil para botones o títulos) |
| **Limpiar** | Vacía la lista para el siguiente pedido |
| Arrastrar la burbuja | La mueve; al soltarla se pega al borde |

## Instalación

Descarga `MiniPicker.apk` desde [Releases](../../releases) e instálalo en el teléfono.

### Primer uso

La app te guía con una lista de **Primeros pasos**:

1. **Activa el servicio:** toca *Activar servicio* → Mini Picker → activar.
2. **Elige tu app de picking:** ábrela, vuelve a Mini Picker y tócala en la lista.
3. **Abre un pedido y recórrelo** hasta el final: aparecerá la burbuja azul.

> **Android 13 o superior:** si al activar el servicio aparece *"Ajuste restringido"*, ve a
> Ajustes → Apps → Mini Picker → ⋮ (arriba a la derecha) → **Permitir ajustes restringidos**, y vuelve a intentarlo.
>
> **Xiaomi / Redmi / POCO:** en Ajustes → Apps → Mini Picker → Ahorro de batería, elige **Sin restricciones**
> para que el sistema no cierre el servicio.

## Personalizar

Todo se cambia desde la app y se guarda solo:

- **Categorías:** edítalas, crea nuevas y ordénalas con ↑ ↓ en el mismo orden de tu recorrido. Así aparecerán en la burbuja.
- **Probar un producto:** escribe un nombre y te dice en qué categoría queda.
- **Productos sin categoría:** los que no calzan con ninguna palabra se pueden asignar con un toque. La próxima vez se ordenan solos.
- **Lectura precisa (automática):** mientras recorres un pedido, Mini Picker aprende en qué parte de la pantalla están los nombres de producto. Desde ese momento captura todos los productos, incluso los que no calzan con ninguna palabra (quedan en "Sin categoría"), y deja de leer botones y títulos.
- **¿La burbuja no se ve bien?** Desde ahí puedes volver a aprender o desactivar la lectura precisa, y con *Ver qué está leyendo* ves todo lo que se leyó en la última pantalla.

## Practicar sin pedidos reales

El proyecto incluye **Picker Demo** (`PickerDemo.apk`), una app de práctica que simula una app de picking: pedidos de 10, 25 o 50 productos mezclados, con SKU, pasillo, cantidad y notas. Algunos productos no tienen categoría a propósito, para practicar cómo asignarlos.

1. Instala Picker Demo y ábrela.
2. En Mini Picker, elige **Picker Demo** como tu app de picking.
3. Recorre el pedido y toca la burbuja.

## Compilar

Requisitos: **JDK 17** y **Android SDK** (plataforma 35). Lo más fácil es abrir la carpeta en Android Studio y presionar *Run*.

Desde la terminal:

```bash
./gradlew assembleRelease
```

Los APK quedan en:

- `app/build/outputs/apk/release/app-release.apk` (Mini Picker)
- `demo/build/outputs/apk/release/demo-release.apk` (Picker Demo)

Pruebas del clasificador:

```bash
./gradlew :app:testReleaseUnitTest
```

> Los APK de release se firman con la clave de depuración para poder instalarlos directamente.
> Si alguna vez quieres publicar la app en una tienda, configura tu propia clave de firma.

## Estructura

```
app/                      Mini Picker
  src/main/java/cl/picker/sorter/
    PickerService.kt      Servicio de accesibilidad: lee la pantalla y guarda los productos
    Classifier.kt         Asigna categoría según palabras clave
    CategoryList.kt       Modelo editable de categorías
    Overlay.kt            Burbuja y panel flotante
    MainActivity.kt       Pantalla principal (pasos, categorías, ayuda)
    Store.kt              Configuración guardada
    Ui.kt                 Colores y componentes visuales
  src/main/assets/categorias.txt   Categorías por defecto
demo/                     Picker Demo (app de práctica)
```

## Limitaciones

- Android solo deja leer lo que está en pantalla, así que hay que **recorrer la lista completa** una vez para capturar todos los productos.
- La clasificación por palabras clave no es perfecta. Revisa "Sin categoría" y ajusta las palabras a cómo nombra los productos tu app.
- Es una herramienta personal de apoyo. Revisa que su uso esté permitido en tu lugar de trabajo.
