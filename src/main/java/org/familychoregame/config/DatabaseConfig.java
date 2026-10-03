package org.familychoregame.config;

import java.time.Instant;
import java.time.LocalDate;
import org.familychoregame.domain.Assignment;
import org.familychoregame.domain.Right;
import org.familychoregame.domain.RightRedemption;
import org.familychoregame.domain.Chore;
import org.familychoregame.domain.ChoreCategory;
import org.familychoregame.domain.FamilyMember;
import org.familychoregame.domain.FamilyProfile;
import org.familychoregame.api.ApiModels.MemberBalance;
import org.familychoregame.api.ApiModels.LoginMember;
import org.familychoregame.repository.GameRepository.AuthMember;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class DatabaseConfig {
    @Bean
    Jdbi jdbi(AppProperties properties) {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(properties.databaseUrl());
        dataSource.setUser(properties.databaseUsername());
        dataSource.setPassword(properties.databasePassword());
        return Jdbi.create(dataSource)
                .installPlugins()
                .registerColumnMapper(LocalDate.class,
                        (resultSet, column, context) -> resultSet.getObject(column, LocalDate.class))
                .registerColumnMapper(Instant.class, (resultSet, column, context) -> {
                    var value = resultSet.getObject(column, java.time.OffsetDateTime.class);
                    return value == null ? null : value.toInstant();
                })
                .registerRowMapper(ConstructorMapper.factory(FamilyMember.class))
                .registerRowMapper(ConstructorMapper.factory(FamilyProfile.class))
                .registerRowMapper(ConstructorMapper.factory(ChoreCategory.class))
                .registerRowMapper(ConstructorMapper.factory(Chore.class))
                .registerRowMapper(ConstructorMapper.factory(Assignment.class))
                .registerRowMapper(ConstructorMapper.factory(Right.class))
                .registerRowMapper(ConstructorMapper.factory(RightRedemption.class))
                .registerRowMapper(ConstructorMapper.factory(MemberBalance.class))
                .registerRowMapper(ConstructorMapper.factory(LoginMember.class))
                .registerRowMapper(ConstructorMapper.factory(AuthMember.class));
    }
}
