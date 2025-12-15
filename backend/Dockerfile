FROM quay.io/wildfly/wildfly:latest

# Устанавливаем Java 17 и заменяем встроенную Java в WildFly
USER root
RUN yum install -y java-17-openjdk java-17-openjdk-devel && \
    alternatives --set java java-17-openjdk && \
    alternatives --set javac java-17-openjdk || \
    (dnf install -y java-17-openjdk java-17-openjdk-devel && \
     alternatives --set java java-17-openjdk && \
     alternatives --set javac java-17-openjdk) || \
    echo "Java 17 installation skipped"

# Заменяем встроенную Java в WildFly на Java 17
RUN if [ -d "/usr/lib/jvm/java-17-openjdk" ]; then \
        rm -rf /opt/jboss/wildfly/jdk && \
        ln -s /usr/lib/jvm/java-17-openjdk /opt/jboss/wildfly/jdk; \
    fi

# Увеличиваем память для Metaspace
ENV JAVA_OPTS="-Xms256m -Xmx1024m -XX:MetaspaceSize=256M -XX:MaxMetaspaceSize=1024m"

USER root

# Скачиваем PostgreSQL драйвер
RUN mkdir -p /opt/jboss/wildfly/modules/org/postgresql/main && \
    curl -L https://jdbc.postgresql.org/download/postgresql-42.7.3.jar -o /opt/jboss/wildfly/modules/org/postgresql/main/postgresql-42.7.3.jar

# Создаем module.xml для PostgreSQL драйвера
RUN echo '<?xml version="1.0" encoding="UTF-8"?>' > /opt/jboss/wildfly/modules/org/postgresql/main/module.xml && \
    echo '<module xmlns="urn:jboss:module:1.9" name="org.postgresql">' >> /opt/jboss/wildfly/modules/org/postgresql/main/module.xml && \
    echo '  <resources>' >> /opt/jboss/wildfly/modules/org/postgresql/main/module.xml && \
    echo '    <resource-root path="postgresql-42.7.3.jar"/>' >> /opt/jboss/wildfly/modules/org/postgresql/main/module.xml && \
    echo '  </resources>' >> /opt/jboss/wildfly/modules/org/postgresql/main/module.xml && \
    echo '  <dependencies>' >> /opt/jboss/wildfly/modules/org/postgresql/main/module.xml && \
    echo '    <module name="javax.api"/>' >> /opt/jboss/wildfly/modules/org/postgresql/main/module.xml && \
    echo '    <module name="javax.transaction.api"/>' >> /opt/jboss/wildfly/modules/org/postgresql/main/module.xml && \
    echo '  </dependencies>' >> /opt/jboss/wildfly/modules/org/postgresql/main/module.xml && \
    echo '</module>' >> /opt/jboss/wildfly/modules/org/postgresql/main/module.xml

# Копируем скрипты конфигурации
COPY configure-datasource.cli /opt/jboss/wildfly/
COPY entrypoint.sh /opt/jboss/wildfly/

# Делаем entrypoint скрипт исполняемым
RUN chmod +x /opt/jboss/wildfly/entrypoint.sh

USER jboss

EXPOSE 8080 9990

ENTRYPOINT ["/opt/jboss/wildfly/entrypoint.sh"]
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
