package com.jzel

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/example")
class ExampleResource(private val persistence: ExamplePersistence) {
    private val csrfToken: String = UUID.randomUUID().toString()
    private val sessionId: String = UUID.randomUUID().toString()

    @GetMapping
    fun getData(@CookieValue("SESSION_ID", required = false) sessionId: String?): ResponseEntity<List<ExampleEntity>> {
        return if (sessionId == this.sessionId) {
            ResponseEntity.ok(persistence.findAll())
        } else {
            ResponseEntity.status(401).build()
        }
    }

    @GetMapping("/login")
    fun login(): ResponseEntity<String> {
        val headers = HttpHeaders()
        headers.add("Set-Cookie", "SESSION_ID=${sessionId}; Max-Age=604800; Path=/; Secure; HttpOnly")
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(csrfToken)
    }

    @GetMapping("/{message}")
    fun veryInsecureExample(
        @PathVariable("message") message: String,
        @CookieValue("SESSION_ID", required = false) sessionId: String?
    ): ResponseEntity<Void> {
        if (sessionId == this.sessionId) {
            persistence.save(ExampleEntity(0, message))
            return ResponseEntity.ok().build()
        } else {
            return ResponseEntity.status(401).build()
        }
    }

    @PostMapping("/{message}")
    fun insecureExample(
        @PathVariable("message") message: String,
        @CookieValue("SESSION_ID", required = false) sessionId: String?
    ): ResponseEntity<Void> {
        if (sessionId == this.sessionId) {
            persistence.save(ExampleEntity(0, message))
            return ResponseEntity.ok().build()
        } else {
            return ResponseEntity.status(401).build()
        }
    }

    @PostMapping("/secure/{message}")
    fun csrfSecureExample(
        @PathVariable("message") message: String,
        @RequestParam(name = "token") token: String?,
        @CookieValue("SESSION_ID", required = false) sessionId: String?
    ): ResponseEntity<Void> {
        if (sessionId == this.sessionId) {
            if (token == csrfToken) {
                persistence.save(ExampleEntity(0, message))
                return ResponseEntity.ok().build()
            } else {
                return ResponseEntity.status(403).build()
            }
        } else {
            return ResponseEntity.status(401).build()
        }
    }

    @GetMapping("/frontend-insecure", produces = ["text/html"])
    fun frontendInsecure(): ResponseEntity<String> {
        val htmlContent = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>CSRF Demo</title>
            <script>
                function sendRequest() {
                    var message = document.getElementById('messageInput').value;
                    fetch('/example/' + message, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                    })
                    .then(response => {
                        if (response.ok) {
                            return response.text();
                        }
                        throw new Error('Network response was not ok.');
                    })
                    .then(data => {
                        console.log('Success:', data);
                        alert('Nachricht erfolgreich gesendet!');
                    })
                    .catch((error) => {
                        console.error('Error:', error);
                        alert('Fehler beim Senden der Nachricht.');
                    });
                }
            </script>
        </head>
        <body>
            <h1>CSRF Demo</h1>
            <input type="text" id="messageInput" placeholder="Deine Nachricht hier">
            <button onclick="sendRequest()">Sende POST-Anfrage</button>
        </body>
        </html>
    """.trimIndent()

        val headers = HttpHeaders()
        headers.contentType = MediaType.TEXT_HTML

        return ResponseEntity.ok().headers(headers).body(htmlContent)
    }

    @GetMapping("/frontend-evil", produces = ["text/html"])
    fun frontendEvil(): ResponseEntity<String> {
        val htmlContent = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>CSRF Demo</title>
            <script>
                function sendRequest() {
                    fetch('/example/ich%20bringe%20morgen%20%20einen%20kuchen%20mit', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                    })
                    .then(response => {
                        if (response.ok) {
                            return response.text();
                        }
                        throw new Error('Network response was not ok.');
                    })
                    .then(data => {
                        console.log('Success:', data);
                        alert('Nachricht erfolgreich gesendet!');
                    })
                    .catch((error) => {
                        console.error('Error:', error);
                        alert('Fehler beim Senden der Nachricht.');
                    });
                }
            </script>
        </head>
        <body>
            <h1>CSRF Demo</h1>
            <input type="text" id="messageInput" placeholder="Deine Nachricht hier">
            <button onclick="sendRequest()">Sende POST-Anfrage</button>
        </body>
        </html>
    """.trimIndent()

        val headers = HttpHeaders()
        headers.contentType = MediaType.TEXT_HTML

        return ResponseEntity.ok().headers(headers).body(htmlContent)
    }

    @GetMapping("/frontend-secure", produces = ["text/html"])
    fun frontendSecure(): ResponseEntity<String> {
        val htmlContent = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>CSRF Secure Demo</title>
            <script>
                var csrfToken = '';
        
                async function login() {
                    const response = await fetch('/example/login');
                    if (response.ok) {
                        csrfToken = await response.text();
                        console.log('CSRF-Token erhalten:', csrfToken); // Zum Debuggen
                        alert('Login erfolgreich! CSRF-Token erhalten.');
                    } else {
                        alert('Login fehlgeschlagen!');
                    }
                }
        
                function sendSecureRequest() {
                    var message = document.getElementById('messageInput').value;
                    console.log('Sende Nachricht mit CSRF-Token:', csrfToken); // Zum Debuggen
                    fetch('/example/secure/' + encodeURIComponent(message) + '?token=' + csrfToken, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({})
                    })
                    .then(response => {
                        if (response.ok) {
                            alert('Nachricht sicher gesendet!');
                        } else {
                            alert('Fehler beim Senden der Nachricht. Stellen Sie sicher, dass Sie eingeloggt sind und das CSRF-Token gültig ist.');
                        }
                    })
                    .catch((error) => {
                        console.error('Error:', error);
                        alert('Fehler beim Senden der Nachricht.');
                    });
                }
            </script>
        </head>
        <body>
            <h1>CSRF Secure Demo</h1>
            <input type="text" id="messageInput" placeholder="Deine sichere Nachricht hier">
            <button onclick="login()">Login</button>
            <button onclick="sendSecureRequest()">Sende sichere Nachricht</button>
        </body>
        </html>
        """.trimIndent()
        val headers = HttpHeaders()
        headers.contentType = MediaType.TEXT_HTML
        return ResponseEntity.ok().headers(headers).body(htmlContent)
    }
}
