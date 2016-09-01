FROM java:8-alpine
MAINTAINER Brian Murphy-Dye <bmurphydye@summit.com>

ADD target/uberjar/timmus.jar /timmus/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/timmus/app.jar"]
