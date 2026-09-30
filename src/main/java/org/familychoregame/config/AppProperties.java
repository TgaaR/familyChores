package org.familychoregame.config;

import java.nio.file.Path;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("family-chore-game")
public record AppProperties(Path databasePath, ZoneId timezone) {
}
