package usc.uscPredict.controller;

import com.github.fge.jsonpatch.JsonPatchException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import org.springframework.web.bind.annotation.*;
import usc.uscPredict.model.User;
import usc.uscPredict.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import usc.uscPredict.util.PatchUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Tag(name = "Users", description = "API de gestión de usuarios")
@RestController
@RequestMapping("users")
class UserController {
    UserService userService;
    PatchUtils patchUtils;

    @Autowired
    public UserController(UserService userService, PatchUtils patchUtils) {
        this.userService = userService;
        this.patchUtils = patchUtils;
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

    @Operation(
            summary = "Actualizar usuario con JSON-Patch",
            description = "Aplica operaciones JSON-Patch (RFC 6902) para actualizar un usuario. Permite actualizar nombre, email y contraseña."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "400", description = "Operación de patch inválida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    })
    @PatchMapping("/{uuid}")
    public ResponseEntity<User> patchUser(
            @Parameter(description = "UUID del usuario a actualizar", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID uuid,
            @Parameter(description = "Lista de operaciones JSON-Patch", required = true)
            @RequestBody List<Map<String, Object>> updates) {
        try {
            // 1. Obter o usuario da base de datos
            User existingUser = userService.getUserById(uuid);
            if (existingUser == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            // 2. Aplicar as operacións JSON-Patch
            User patchedUser = patchUtils.applyPatch(existingUser, updates);

            // 3. Gardar o usuario actualizado
            User updated = userService.updateUser(uuid, patchedUser);
            return new ResponseEntity<>(updated, HttpStatus.OK);

        } catch (JsonPatchException e) {
            // Erro ao aplicar o parche (operación inválida, path incorrecto, etc.)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}


