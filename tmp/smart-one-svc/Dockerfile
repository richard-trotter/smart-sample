FROM adoptopenjdk:8-jre-openj9

WORKDIR /apps/smart-one-svc

COPY smart-one-svc*jar ./

# Make port 8081 available to the world outside this container
EXPOSE 8081

CMD ["/bin/sh", "-x", "-c", "java -Xms500m -Dfile.encoding=UTF8 -jar smart-one-svc*.jar --server.port=8081"]
