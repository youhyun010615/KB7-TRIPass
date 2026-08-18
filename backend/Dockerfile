FROM tomcat:9.0-jdk17-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

RUN sed -i 's/Connector port="8080"/Connector port="8080" URIEncoding="UTF-8"/' /usr/local/tomcat/conf/server.xml

COPY build/libs/backend-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war
COPY src/main/resources/google-credentials.json /opt/google-credentials.json

ENV GOOGLE_APPLICATION_CREDENTIALS=/opt/google-credentials.json
ENV JAVA_OPTS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dspring.http.encoding.charset=UTF-8 -Dspring.http.encoding.enabled=true -Dspring.http.encoding.force=true"
ENV LANG=ko_KR.UTF-8

EXPOSE 8080

CMD ["catalina.sh", "run"]
