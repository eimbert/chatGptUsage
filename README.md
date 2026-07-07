# OCR Service Spring Boot con IA

Servicio OCR Java 17 con Spring Boot, empaquetado como `WAR` para desplegar en Tomcat externo. La lectura de etiquetas se hace con OpenAI Vision, sin OpenCV, sin Tesseract y sin dependencias nativas instaladas en el servidor.

## Que hace

- Recibe una imagen en base64.
- Envia la imagen a OpenAI Vision.
- Pide una respuesta JSON estructurada.
- Extrae datos sanitarios/quirurgicos habituales:
  - EAN/GTIN con AI `(01)`.
  - Caducidad con AI `(17)`.
  - Lote con AI `(10)`.
- Aplica una validacion final con patrones GS1 en Java.
- Devuelve estado `OK`, `DUBTOSA` o `MALAMENT`, texto detectado y campos GS1.

## Requisitos

- Java 17.
- Maven 3.9+.
- Conexion a internet desde el servidor.
- API key de OpenAI.

No requiere instalar Tesseract ni OpenCV en el servidor.

## Configuracion

Configura estas variables de entorno en desarrollo, Tomcat o el servidor:

```powershell
$env:OPENAI_API_KEY="sk-..."
$env:OPENAI_MODEL="gpt-4.1-mini"
```

Tambien puedes cambiar valores en:

```text
src/main/resources/application.properties
```

Propiedades:

```properties
server.port=8088
openai.api.key=${OPENAI_API_KEY:}
openai.model=${OPENAI_MODEL:gpt-4.1-mini}
```

## Ejecutar desde Eclipse/STS

Importa el proyecto como Maven project y ejecuta:

```text
Run As > Spring Boot App
```

Clase principal:

```text
com.fhes.ocr.OcrApplication
```

Por defecto arrancara en:

```text
http://localhost:8088
```

## Compilar WAR para Tomcat

```powershell
cd C:\programacion\ocr-service
mvn clean package
```

Genera:

```text
target\ocr-service.war
```

Copia ese fichero en:

```text
TOMCAT_HOME\webapps\ocr-service.war
```

En Tomcat externo, las URLs quedan con el contexto del WAR:

```text
http://servidor:8080/ocr-service/health
http://servidor:8080/ocr-service/api/ocr
```

## Endpoints

### Salud

```http
GET /health
```

Respuesta:

```json
{
  "status": "UP"
}
```

### OCR

```http
POST /api/ocr
Content-Type: application/json
```

Body:

```json
{
  "imageBase64": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ...",
  "fileName": "etiqueta.jpg"
}
```

Respuesta ejemplo:

```json
{
  "status": "OK",
  "message": "Imagen aceptable y patrones reconocidos.",
  "text": "(01)08437012345678(17)260731(10)FHES1234",
  "fields": {
    "ean": "08437012345678",
    "lot": "FHES1234",
    "expiryDate": "2026-07-31",
    "rawAiValues": {
      "01": "08437012345678",
      "17": "2026-07-31",
      "10": "FHES1234"
    }
  }
}
```
