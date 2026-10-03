package org.familychoregame.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("family-chore-game")
public record AppProperties(String databaseUrl, String databaseUsername, String databasePassword, ZoneId timezone) {
}
