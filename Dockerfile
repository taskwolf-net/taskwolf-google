FROM alpine

COPY /build/libs/google-1.0.0-SNAPSHOT.jar google.jar
COPY /locale/ /locale/