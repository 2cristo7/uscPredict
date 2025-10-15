package usc.uscPredict.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import usc.uscPredict.model.User;
import usc.uscPredict.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Tag(name = "Users", description = "API de gestión de usuarios")
@RestController
@RequestMapping("users")
class UserController {
    UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Listar todos los usuarios",
            description = "Obtiene una lista completa de todos los usuarios registrados en la plataforma"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class)))
    })
    @GetMapping
    public ResponseEntity<@NonNull Set<User>> getAllUsers() {
        return new ResponseEntity<>(userService.getUsers().findAll(), HttpStatus.OK);
    }

    @Operation(
            summary = "Obtener usuario por nombre",
            description = "Busca un usuario por su nombre. Nota: puede haber duplicados si múltiples usuarios tienen el mismo nombre"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    })
    @GetMapping("/{name}")
    public ResponseEntity<User> getUserByName(
            @Parameter(description = "Nombre del usuario a buscar", required = true, example = "Juan Pérez")
            @PathVariable("name") String name) {
        User user = userService.getUserByName(name);
        if (user != null) {
            return new ResponseEntity<>(user, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Operation(
            summary = "Health check",
            description = "Verifica que el servicio de usuarios esté funcionando correctamente"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Servicio funcionando correctamente")
    })
    @GetMapping("/health")
    public Map<String, Object> getHealthStatus() {
        return Map.of(
                "status", "OK",
                "service", "USC Predict",
                "timestamp", LocalDateTime.now().toString()
        );
    }

    @Operation(
            summary = "Crear nuevo usuario",
            description = "Registra un nuevo usuario en la plataforma. Requiere nombre, email único, hash de contraseña y rol"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto - Email ya existe", content = @Content)
    })
    @PostMapping
    public ResponseEntity<User> addUser(
            @Parameter(description = "Datos del usuario a crear", required = true)
            @RequestBody @NonNull User user) {
        User addedUser = userService.addUser(user);
        if (addedUser != null) {
            return new ResponseEntity<>(addedUser, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }
}


