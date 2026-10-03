package org.familychoregame.security;

import java.io.Serializable;

public record AuthenticatedMember(String id, String displayName, String role) implements Serializable {
}
