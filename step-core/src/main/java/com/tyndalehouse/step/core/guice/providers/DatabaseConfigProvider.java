package com.tyndalehouse.step.core.guice.providers;

import org.apache.commons.dbcp.BasicDataSource;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.ServerConfig;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.tyndalehouse.step.core.data.entities.Bookmark;
import com.tyndalehouse.step.core.data.entities.HotSpot;
import com.tyndalehouse.step.core.data.entities.ScriptureReference;
import com.tyndalehouse.step.core.data.entities.ScriptureTarget;
import com.tyndalehouse.step.core.data.entities.Session;
import com.tyndalehouse.step.core.data.entities.Timeband;
import com.tyndalehouse.step.core.data.entities.TimelineEvent;
import com.tyndalehouse.step.core.data.entities.User;

/**
 * Returns a database connection server instance for use across the application
 * 
 * @author Chris
 * 
 */
public class DatabaseConfigProvider implements Provider<EbeanServer> {
    private final String driverClassName;
    private final boolean poolStatements;
    private final int maxActive;
    private final int maxIdle;
    private final int maxOpenStatements;
    private final String validationQuery;
    private final String url;
    private final String username;
    private final String password;

    /**
     * We inject some properties in to the datasource provider
     * 
     * @param driverClassName the driver name
     * @param url the URL connection string
     * @param username the username to login with
     * @param password the password
     * @param maxActive the maximum number of active connections
     * @param maxIdle the maximum number of idle connections
     * @param maxOpenStatements the maximum number of open statements
     * @param poolableStatements true if statements should be pooled
     * @param validationQuery the validation query to check the status of a connection
     */
    // CHECKSTYLE:OFF
    @Inject
    public DatabaseConfigProvider(@Named("app.db.driver") final String driverClassName,
            @Named("app.db.url") final String url, @Named("app.db.username") final String username,
            @Named("app.db.password") final String password,
            @Named("app.db.maxActive") final String maxActive, @Named("app.db.maxIdle") final String maxIdle,
            @Named("app.db.maxOpenStatement") final String maxOpenStatements,
            @Named("app.db.poolableStatements") final String poolableStatements,
            @Named("app.db.validationQuery") final String validationQuery) {
        this.driverClassName = driverClassName;
        this.url = url;
        this.username = username;
        this.password = password;
        this.validationQuery = validationQuery;

        // TODO add exception handling when i know how
        this.maxActive = Integer.parseInt(maxActive);
        this.maxIdle = Integer.parseInt(maxIdle);
        this.maxOpenStatements = Integer.parseInt(maxOpenStatements);
        this.poolStatements = Boolean.parseBoolean(poolableStatements);
    }

    // CHECKSTYLE:ON

    @Override
    public EbeanServer get() {
        final ServerConfig config = new ServerConfig();
        config.setName("db");

        final BasicDataSource ds = new BasicDataSource();
        ds.setDefaultAutoCommit(false);
        ds.setDriverClassName(this.driverClassName);
        ds.setPoolPreparedStatements(this.poolStatements);
        ds.setMaxActive(this.maxActive);
        ds.setMaxIdle(this.maxIdle);
        ds.setMaxOpenPreparedStatements(this.maxOpenStatements);
        ds.setValidationQuery(this.validationQuery);
        ds.setUrl(this.url);
        ds.setUsername(this.username);
        ds.setPassword(this.password);

        config.setDataSource(ds);

        // config.addPackage("com.tyndalehouse.step.core.data.entities");
        addEntities(config);

        // set DDL options...
        config.setDdlGenerate(true);
        config.setDdlRun(true);

        config.setDefaultServer(true);
        config.setRegister(true);

        return EbeanServerFactory.create(config);
    }

    /**
     * Adds all entities to ebean server. We need to this, since it seems since Ebean only looks at exploded
     * parts of the classpath and therfore we would have to hard code the jar file name into the classpath
     * 
     * @param config the configuration to be enhanced
     */
    private void addEntities(final ServerConfig config) {
        // timeline entities
        config.addClass(HotSpot.class);
        config.addClass(Timeband.class);
        config.addClass(TimelineEvent.class);
        config.addClass(ScriptureTarget.class);
        config.addClass(ScriptureReference.class);
        config.addClass(User.class);
        config.addClass(Session.class);
        config.addClass(Bookmark.class);
    }

}
