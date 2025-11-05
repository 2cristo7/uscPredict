package usc.uscPredict.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Utility class for applying JSON-Patch operations (RFC 6902) to domain objects.
 * Provides a generic method to apply a list of patch operations to any object.
 */
@Service
public class PatchUtils {

    private final ObjectMapper mapper;

    public PatchUtils(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Applies a list of JSON-Patch operations to a domain object.
     *
     * @param data The original domain object to patch
     * @param updates List of patch operations (each operation is a Map with "op", "path", and "value" keys)
     * @param <T> The type of the domain object
     * @return The patched domain object
     * @throws JsonPatchException If the patch operations cannot be applied
     */
    @SuppressWarnings("unchecked")
    public <T> T applyPatch(T data, List<Map<String, Object>> updates) throws JsonPatchException {
        // 1. Converter a lista de mapas a un obxecto JsonPatch
        JsonPatch operations = mapper.convertValue(updates, JsonPatch.class);

        // 2. Converter o obxecto de dominio a JsonNode
        JsonNode json = mapper.convertValue(data, JsonNode.class);

        // 3. Aplicar as operacións ao JsonNode
        JsonNode updatedJson = operations.apply(json);

        // 4. Converter o JsonNode modificado de volta ao obxecto de dominio
        return (T) mapper.convertValue(updatedJson, data.getClass());
    }
}
