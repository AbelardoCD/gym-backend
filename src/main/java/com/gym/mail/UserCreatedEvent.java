package com.gym.mail;

/** Se publica al crear un usuario; el correo sale cuando la transacción se confirma. */
public record UserCreatedEvent(String email, String fullName) {}
