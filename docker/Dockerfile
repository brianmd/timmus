FROM clojure:latest

RUN apt-get update && \
    apt-get install -y sudo

ENV MAVEN_VERSION="3.3.9" \
    M2_HOME=/usr/lib/mvn

RUN cd /tmp && \
  wget "http://ftp.unicamp.br/pub/apache/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz" && \
  tar -zxvf "apache-maven-$MAVEN_VERSION-bin.tar.gz" && \
  mv "apache-maven-$MAVEN_VERSION" "$M2_HOME" && \
  ln -s "$M2_HOME/bin/mvn" /usr/bin/mvn

RUN adduser --disabled-password --gecos '' summit && \
    adduser summit sudo && \
    echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers

RUN echo "cleaning up" && \
  # apk del wget && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
    rm -f  /var/cache/apk/*

# RUN mkdir -p /root/docker
# WORKDIR /root
# ENV HOME /root
RUN mkdir -p /home/summit/docker
WORKDIR /home/summit
USER summit
ENV HOME /home/summit

# 7897=nrepl, 7002=cljs-nrepl, 3449=http
# EXPOSE 7897 7002 3449
EXPOSE 7000 3007 7002 3449

# WORKDIR /home/clojure
# ENV HOME /home/clojure
# VOLUME ["/home/clojure"]

# USER clojure

