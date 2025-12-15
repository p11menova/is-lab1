
if [ -d "/usr/lib/jvm/java-17-openjdk" ]; then
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
    export PATH=$JAVA_HOME/bin:$PATH
    echo "Using Java 17 from $JAVA_HOME"
    java -version
fi

echo "Starting WildFly..."

# Запускаем WildFly в фоне
/opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 -bmanagement 0.0.0.0 &
WILDFLY_PID=$!

# Функция для ожидания запуска WildFly
wait_for_wildfly() {
    echo "Waiting for WildFly to start..."
    for i in {1..60}; do
        if /opt/jboss/wildfly/bin/jboss-cli.sh --connect --command=":read-attribute(name=server-state)" 2>/dev/null | grep -q "running"; then
            echo "WildFly is running!"
            return 0
        fi
        sleep 2
    done
    echo "Timeout waiting for WildFly to start"
    return 1
}

# Ждем запуска WildFly
if wait_for_wildfly; then
    echo "Configuring datasource..."
    
    # Сначала регистрируем драйвер PostgreSQL
    echo "Registering PostgreSQL driver..."
    /opt/jboss/wildfly/bin/jboss-cli.sh --connect --command="/subsystem=datasources/jdbc-driver=postgresql:add(driver-name=postgresql, driver-module-name=org.postgresql, driver-class-name=org.postgresql.Driver)" 2>&1 || echo "Driver may already exist"
    
    # Затем создаем datasource
    echo "Creating datasource..."
    /opt/jboss/wildfly/bin/jboss-cli.sh --connect --file=/opt/jboss/wildfly/configure-datasource.cli
    if [ $? -eq 0 ]; then
        echo "Datasource configured successfully!"
        
        # Redeploy приложение, чтобы оно подхватило datasource
        echo "Redeploying application..."
        /opt/jboss/wildfly/bin/jboss-cli.sh --connect --command="deployment redeploy is_lab1.war" 2>/dev/null || echo "Application will be redeployed automatically"
        echo "Configuration complete!"
    else
        echo "Failed to configure datasource! Check logs above."
        # Не выходим, чтобы увидеть ошибки
    fi
else
    echo "Failed to start WildFly"
    kill $WILDFLY_PID 2>/dev/null || true
    exit 1
fi

# Ждем завершения процесса WildFly
wait $WILDFLY_PID
