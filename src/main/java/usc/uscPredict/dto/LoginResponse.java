package usc.uscPredict.dto;

import java.util.UUID;

public record LoginResponse(UUID uuid, String name, String email, String role) {}
