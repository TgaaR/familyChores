package org.familychoregame.config;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import org.familychoregame.domain.Assignment;
import org.familychoregame.domain.Benefit;
import org.familychoregame.domain.BenefitRedemption;
import org.familychoregame.domain.Chore;
import org.familychoregame.domain.ChoreCategory;
import org.familychoregame.domain.FamilyMember;
import org.familychoregame.domain.FamilyProfile;
import org.familychoregame.api.ApiModels.MemberBalance;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class DatabaseConfig {
    @Bean
    Jdbi jdbi(AppProperties properties) throws IOException {
        var databasePath = properties.databasePath().toAbsolutePath();
        if (databasePath.getParent() != null) {
            Files.createDirectories(databasePath.getParent());
        }

        var sqliteConfig = new SQLiteConfig();
        sqliteConfig.enforceForeignKeys(true);
        sqliteConfig.setBusyTimeout(5_000);
        sqliteConfig.setJournalMode(SQLiteConfig.JournalMode.WAL);
        sqliteConfig.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);

        var dataSource = new SQLiteDataSource(sqliteConfig);
        dataSource.setUrl("jdbc:sqlite:" + databasePath);
        return Jdbi.create(dataSource)
                .registerColumnMapper(LocalDate.class,
                        (resultSet, column, context) -> LocalDate.parse(resultSet.getString(column)))
                .registerColumnMapper(Instant.class, (resultSet, column, context) -> {
                    var value = resultSet.getString(column);
                    return value == null ? null : Instant.parse(value);
                })
                .registerRowMapper(ConstructorMapper.factory(FamilyMember.class))
                .registerRowMapper(ConstructorMapper.factory(FamilyProfile.class))
                .registerRowMapper(ConstructorMapper.factory(ChoreCategory.class))
                .registerRowMapper(ConstructorMapper.factory(Chore.class))
                .registerRowMapper(ConstructorMapper.factory(Assignment.class))
                .registerRowMapper(ConstructorMapper.factory(Benefit.class))
                .registerRowMapper(ConstructorMapper.factory(BenefitRedemption.class))
                .registerRowMapper(ConstructorMapper.factory(MemberBalance.class));
    }
}
